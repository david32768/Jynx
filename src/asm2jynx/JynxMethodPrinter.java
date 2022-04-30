package asm2jynx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import static jvm.AccessFlag.*;
import static jvm.AttributeName.*;
import static jvm.Context.*;
import static jynx.Directive.*;
import static jynx.Global.*;
import static jynx.ReservedWord.*;

import jvm.AccessFlag;
import jvm.FrameType;
import jvm.JvmVersion;
import jynx.GlobalOption;
import jynx2asm.MethodDesc;

public class JynxMethodPrinter {

    protected final JvmVersion jvmVersion;
    protected final Object2String o2s;
    protected final LineBuilder jp;
    protected final PrintAnnotations annotator;
    protected final String cname;


    private JynxMethodPrinter(String cname, JvmVersion jvmversion, LineBuilder jp,PrintAnnotations jasAnnotator) {
        this.cname = cname;
        this.jp = jp;
        this.o2s = new Object2String();
        this.jvmVersion = jvmversion;
        this.annotator = jasAnnotator;
    }

    public static JynxMethodPrinter getInstance(String cname, JvmVersion jvmversion, LineBuilder jp,
            PrintAnnotations jasAnnotator) {
        return new JynxMethodPrinter(cname,jvmversion,jp,jasAnnotator);
    }
    
    private final <E> Iterable<E> nonNullList(Iterable<E> list) {
        return list == null ? Collections.emptyList() : list;
    }

    private <E> boolean isAbsent(Iterable<E> list) {
        return list == null || !list.iterator().hasNext();
    }
    
    private <E> boolean isPresent(Iterable<E> list) {
        return !isAbsent(list); // list != null && list.iterator().hasNext();
    }
    
    private void printInstructions(Iterable<AbstractInsnNode> instructions,Insn2Jynx i2s) {
        for (AbstractInsnNode in:nonNullList(instructions)) {
            i2s.printInsn(in);
            jp.incrDepth();
            annotator.printInsnTypeAnnotations(in.visibleTypeAnnotations, in.invisibleTypeAnnotations);
            jp.decrDepth();
        }
    }

    private void printCatchBlocks(MethodNode mn, Insn2Jynx i2s) {
        for (TryCatchBlockNode tcbn : nonNullList(mn.tryCatchBlocks)) {
            String fromname = i2s.getLabelName(tcbn.start);
            String toname = i2s.getLabelName(tcbn.end);
            String usingname = i2s.getLabelName(tcbn.handler);
            String exception = tcbn.type == null?res_all.toString():tcbn.type;
            jp.appendNonNull(dir_catch,exception)
                    .append(res_from, fromname)
                    .append(res_to, toname)
                    .append(res_using, usingname)
                    .nl();
            if (isPresent(tcbn.visibleTypeAnnotations) || isPresent(tcbn.invisibleTypeAnnotations)) {
                jp.incrDepth();
                annotator.printExceptAnnotations(tcbn.visibleTypeAnnotations, tcbn.invisibleTypeAnnotations);
                jp.decrDepth();
                jp.appendDirective(end_catch).nl();
            }
        }
    }

    private void printLocalVariables(MethodNode mn, Insn2Jynx i2s) {
        List<LocalVariableNode> lvnlist = mn.localVariables == null?new ArrayList<>():mn.localVariables;
        for (LocalVariableNode lvn : lvnlist) {
            String fromname = i2s.getLabelName(lvn.start);
            String toname = i2s.getLabelName(lvn.end);
            jp.append(dir_var)
                    .append(lvn.index)
                    .append(res_is,lvn.name)
                    .append(lvn.desc)
                    .append(res_signature, lvn.signature)
                    .append(res_from, fromname)
                    .append(res_to,toname)
                    .nl();
        }
        annotator.printLocalVarAnnotations(mn.visibleLocalVariableAnnotations, mn.invisibleLocalVariableAnnotations,
                i2s::getLabelName,lvnlist);
    }
    
    private void printCode(MethodNode mn, String classname) {
        jvmVersion.checkSupports(Code);
        if (OPTION(GlobalOption.SKIP_CODE)) {
            jp.appendComment(GlobalOption.SKIP_CODE).nl();
            return;
        }
        MethodDesc md = MethodDesc.of(mn);
        Object[] initstack = FrameType.getInitFrame(classname, md).toArray(new Object[0]);
        Insn2Jynx i2s = new Insn2Jynx(jp,jvmVersion,o2s,initstack);
        printCatchBlocks(mn, i2s);
        printInstructions(mn.instructions, i2s);
        printLocalVariables(mn, i2s);
        jp.append(dir_limit)
            .append(res_locals, mn.maxLocals)
            .nl();
        jp.append(dir_limit)
            .append(res_stack, mn.maxStack)
            .nl();
    }
    
    public void printMethod(MethodNode mn) {
        jp.blankline();
        LOGGER().setLine("method " + mn.name);
        EnumSet<AccessFlag> accflags = AccessFlag.getEnumSet(mn.access,METHOD,jvmVersion);
        String line = jp.appendDirective(dir_method)
                .append(accflags, mn.name + mn.desc)
                .nlstr();
        LOGGER().setLine(line);
        LOGGER().pushContext();
        jp.incrDepth();
        jp.printDirective(dir_signature,mn.signature);
        if (mn.exceptions != null) {
            for (String exstr : mn.exceptions) {
                jp.printDirective(dir_throws, exstr);
            }
        }
        for (ParameterNode pn : nonNullList(mn.parameters)) {
            EnumSet<AccessFlag> pnaccflags = AccessFlag.getEnumSet(pn.access, PARAMETER,jvmVersion);
            jp.appendDirective(dir_parameter)
                    .append(pnaccflags,pn.name)
                    .nl();
        }
        annotator.printDefaultAnnotation(mn.annotationDefault,mn.desc);
        annotator.printAnnotations(mn.visibleAnnotations, mn.invisibleAnnotations);
        annotator.printTypeAnnotations(mn.visibleTypeAnnotations, mn.invisibleTypeAnnotations);
        annotator.printParamAnnotations(mn.visibleParameterAnnotations, mn.visibleAnnotableParameterCount,
                mn.invisibleParameterAnnotations, mn.invisibleAnnotableParameterCount);
        boolean isAbstract = accflags.contains(acc_abstract) || accflags.contains(acc_native);
        if (!isAbstract) {
            jp.incrDepth();
            String classname = accflags.contains(acc_static)?null:cname;
            printCode(mn,classname);
            jp.decrDepth();
        }
        jp.decrDepth();
        jp.appendDirective(end_method).nl();
        LOGGER().popContext();
    }

}

package asm2jynx;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import static asm2jynx.Util.*;
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
import jynx2asm.handles.LocalMethodHandle;

public class JynxMethodPrinter {

    protected final JvmVersion jvmVersion;
    protected final Object2String o2s;
    protected final JynxStringBuilder jp;
    protected final PrintAnnotations annotator;
    protected final String cname;


    private JynxMethodPrinter(String cname, JvmVersion jvmversion, JynxStringBuilder jp,PrintAnnotations jasAnnotator) {
        this.cname = cname;
        this.jp = jp;
        this.o2s = new Object2String();
        this.jvmVersion = jvmversion;
        this.annotator = jasAnnotator;
    }

    public static JynxMethodPrinter getInstance(String cname, JvmVersion jvmversion, JynxStringBuilder jp,
            PrintAnnotations jasAnnotator) {
        return new JynxMethodPrinter(cname,jvmversion,jp,jasAnnotator);
    }
    
    private void printInstructions(Iterable<AbstractInsnNode> instructions,Insn2Jynx i2s) {
        for (AbstractInsnNode in:nonNullIterable(instructions)) {
            i2s.printInsn(in);
                jp.incrDepth();
                annotator.printInsnTypeAnnotations(in.visibleTypeAnnotations, in.invisibleTypeAnnotations);
            jp.decrDepth();
        }
    }

    private void printCatchBlocks(MethodNode mn, Insn2Jynx i2s) {
        for (TryCatchBlockNode tcbn : nonNullList(mn.tryCatchBlocks)) {
            String exception = tcbn.type == null?res_all.toString():tcbn.type;
            jp.append(dir_catch)
                    .append(exception)
                    .append(res_from, tcbn.start)
                    .append(res_to, tcbn.end)
                    .append(res_using, tcbn.handler)
                    .nl();
            if (isAnyPresent(tcbn.visibleTypeAnnotations,tcbn.invisibleTypeAnnotations)) {
                annotator.printTypeAnnotations(tcbn.visibleTypeAnnotations, tcbn.invisibleTypeAnnotations);
            }
        }
    }

    private void printLocalVariables(MethodNode mn) {
        List<LocalVariableNode> lvnlist = mn.localVariables == null?new ArrayList<>():mn.localVariables;
        for (LocalVariableNode lvn : lvnlist) {
            jp.append(dir_var)
                    .append(lvn.index)
                    .append(res_is,lvn.name)
                    .append(lvn.desc)
                    .append(res_signature, lvn.signature)
                    .append(res_from, lvn.start)
                    .append(res_to,lvn.end)
                    .nl();
        }
        annotator.printLocalVarAnnotations(mn.visibleLocalVariableAnnotations, mn.invisibleLocalVariableAnnotations);
    }
    
    private void printCode(MethodNode mn, String classname) {
        jvmVersion.checkSupports(Code);
        if (OPTION(GlobalOption.SKIP_CODE)) {
            jp.appendComment(GlobalOption.SKIP_CODE).nl();
            return;
        }
        LocalMethodHandle lmh = LocalMethodHandle.of(mn);
        Object[] initstack = FrameType.getInitFrame(classname, lmh).toArray(new Object[0]);
        Insn2Jynx i2s = new Insn2Jynx(jp,jvmVersion,o2s,initstack);
        printCatchBlocks(mn, i2s);
        printInstructions(mn.instructions, i2s);
        printLocalVariables(mn);
        jp.append(dir_limit)
            .append(res_locals, mn.maxLocals)
            .nl()
            .append(dir_limit)
            .append(res_stack, mn.maxStack)
            .nl();
    }
    
    public void printMethod(final MethodNode mn) {
        jp.blankline();
        LOGGER().setLine("method " + mn.name);
        EnumSet<AccessFlag> accflags = AccessFlag.getEnumSet(mn.access,METHOD,jvmVersion);
        jp.append(dir_method)
                .appendFlags(accflags)
                .appendName(mn.name + mn.desc);
        String line = jp.line();
        jp.nl();
        LOGGER().setLine(line);
        LOGGER().pushContext();
        jp.incrDepth()
                .appendDir(dir_signature,mn.signature)
                .appendDirArray(dir_throws, mn.exceptions);
        int parmnum = 0;
        for (ParameterNode pn : nonNullList(mn.parameters)) {
            EnumSet<AccessFlag> pnaccflags = AccessFlag.getEnumSet(pn.access, PARAMETER,jvmVersion);
            jp.append(dir_parameter)
                    .append(parmnum)
                    .appendFlags(pnaccflags)
                    .appendName(pn.name)
                    .nl();
            ++parmnum;
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
        jp.append(end_method).nl();
        LOGGER().popContext();
    }

}

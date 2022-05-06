package asm;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;

import static jvm.AttributeName.*;
import static jynx.Global.*;
import static jynx.GlobalOption.*;
import static jynx.Message.*;
import static jynx.ReservedWord.*;

import asm.instruction.Instruction;
import jvm.AccessFlag;
import jvm.AsmOp;
import jvm.Context;
import jvm.FrameType;
import jvm.TypeRef;
import jynx.Access;
import jynx.Directive;
import jynx.ReservedWord;
import jynx2asm.ClassChecker;
import jynx2asm.JynxCatch;
import jynx2asm.JynxLabel;
import jynx2asm.JynxLabelMap;
import jynx2asm.JynxScanner;
import jynx2asm.Line;
import jynx2asm.LinesIterator;
import jynx2asm.ops.JynxOp;
import jynx2asm.OwnerNameDesc;
import jynx2asm.StackLocals;
import jynx2asm.String2Insn;
import jynx2asm.Token;
import jynx2asm.TokenArray;

public class JynxCodeHdr implements ContextDependent {

    private final JynxScanner js;
    private List<Object> localStack;
    private final int errct_at_start;

    private final StackLocals stackLocals;

    private int printFlag;
    private int expandMacro;
    
    private int tryct = 0;
    private final JynxLabelMap labelmap;
    private final List<JynxVar> vars;
    
    private final MethodNode mnode;

    private final String2Insn s2a;
    private int endif;
    
    private final Map<Directive,Line> unique_directives;
    
    private JynxCodeHdr(MethodNode mv, JynxScanner js, ClassChecker checker,
            OwnerNameDesc cmd, JynxLabelMap labelmap, Access access, Map<String,JynxOp> opmap) {
        this.js = js;
        String clname = access.is(AccessFlag.acc_static)?null:checker.getClassName();
        this.localStack = FrameType.getInitFrame(clname, cmd); // classname set non null for virtual methods
        this.mnode = mv;
        this.errct_at_start = LOGGER().numErrors();
        this.labelmap = labelmap;
        this.vars = new ArrayList<>();
        Type rtype = Type.getReturnType(cmd.getDesc());
        AsmOp returnop = getReturnOp(rtype);
        this.stackLocals = StackLocals.getInstance(localStack,labelmap,returnop);
        this.s2a = new String2Insn(js, labelmap, checker, returnop,opmap);
        this.printFlag = 0;
        this.expandMacro = 0;
        this.endif = 0;
        this.unique_directives = new HashMap<>();
    }

    public static JynxCodeHdr getInstance(MethodNode mv, JynxScanner js, OwnerNameDesc cmd,
            JynxLabelMap labelmap, Access access,ClassChecker checker, Map<String,JynxOp> opmap) {
        CHECK_SUPPORTS(Code);
        return new JynxCodeHdr(mv, js, checker, cmd, labelmap,access,opmap);
    }

    private static AsmOp getReturnOp(Type rtype) {
        char rtchar = rtype.getDescriptor().charAt(0);
        switch (rtchar) {
            case 'V':
                return AsmOp.asm_return;
            case 'Z':
            case 'B':
            case 'C':
            case 'S':
            case 'I':
                return AsmOp.asm_ireturn;
            case 'F':
                return AsmOp.asm_freturn;
            case 'D':
                return AsmOp.asm_dreturn;
            case 'J':
                return AsmOp.asm_lreturn;
            default:
                return AsmOp.asm_areturn;
        }
    }
    
    @Override
    public Context getContext() {
        return Context.CODE;
    }

    @Override
    public void visitDirective(Directive dir, JynxScanner js) {
        Line line = js.getLine();
        dir.checkUnique(unique_directives, line);
        switch(dir) {
            case dir_limit:
                setLimit(line);
                break;
            case dir_catch:
                setCatch(line);
                break;
            case dir_var:
                visitVar(line);
                break;
            case dir_line:
                visitLineNumber(line);
                break;
            case dir_stack:
                LOGGER().pushContext();
                visitStackFrame(line);
                LOGGER().popContext();
                break;
            case dir_print:
                setPrint(line);
                break;
            case state_opcode:
                visitInsn(line);
                break;
            case dir_if:
                visitIf(line);
                break;
            case end_if:
                visitEndIf();
                break;
            default:
                visitCommonDirective(dir, line, js);
                break;
        }
    }
    
    private void setCatch(Line line) {
        Token token = line.nextToken();
        String exception = token.is(res_all)?null:token.asString(); // all = finally
        String fromname = line.after(res_from);
        String toname = line.after(res_to);
        String usingname = line.after(res_using);
        line.noMoreTokens();
        JynxCatch jcatch =  JynxCatch.getInstance(line, fromname, toname, usingname, exception, labelmap);
        if (jcatch != null) {
            stackLocals.visitTryCatchBlock(jcatch);
            jcatch.accept(mnode);
        }
        ++tryct;
    }

    public void visitCode() {
        mnode.visitCode();
        labelmap.start(mnode, js.getLine());
    }

    private void setPrint(Line line) {
        ReservedWord rw  = line.nextToken().oneOf(res_stack, res_locals, res_on, res_off,res_label);
        switch (rw) {
            case res_stack:
                System.out.format("%s; = %s%n", line, stackLocals.stringStack());
                break;
            case res_locals:
                System.out.format("%s; = %s%n", line, stackLocals.stringLocals());
                break;
            case res_label:
                String lab = line.nextToken().asString();
                labelmap.printJynxlabel(lab, line);
                break;
            case res_on:
                ++printFlag;
                if (line.nextToken().is(res_expand)) {
                    expandMacro = printFlag;
                }
                break;
            case res_off:
                if (expandMacro == printFlag) {
                    expandMacro = 0;
                }
                --printFlag;
                assert printFlag >= 0;
                break;
            default:
                throw new AssertionError();
        }
        line.noMoreTokens();
    }
    
    private void visitIf(Line line) {
        Token rw = line.lastToken();
        rw.mustBe(res_reachable);
        if (stackLocals.isUnreachableForwards()) {
            js.skipNested(Directive.dir_if,Directive.end_if,Directive.dir_if,Directive.dir_line);
        } else {
            ++endif;
        }
    }
    
    private void visitEndIf() {
        assert endif > 0;
        --endif;
    }

    private void visitInsns(List<Instruction> instructions, Line line) {
        for (Instruction in:instructions) {
            boolean ok = stackLocals.visitInsn(in, line);
            if (ok) {
                if (expandMacro > 0) {
                    System.out.format(" +%s%n",in);
                }
                in.accept(mnode);
            }
        }
    }
    
    private void visitInsn(Line line) {
        String stackb = printFlag > 0?stackLocals.stringStack():"";
        String localsb = printFlag > 0?stackLocals.stringLocals():"";
        if (printFlag > 0) {
            System.out.println(line);
        }
        List<Instruction>  instructions = s2a.getInsts(line);
        visitInsns(instructions, line);
        if (printFlag > 0) {
            printStackLocals(stackb, localsb,line,instructions);
        }
    }
    
    private void printStackLocals(String stackb, String localsb, Line line, List<Instruction> instructions) {
        String stacka = stackLocals.stringStack();
        String localsa = stackLocals.stringLocals();
        System.out.format("; %s -> %s", stackb,stacka);
        if (!localsa.equals(localsb)) {
            System.out.format(" ; %s = %s",res_locals,localsa);
        }
        System.out.println();
    }
    
    private void visitLineNumber(Line line) {
        int lineno = line.lastToken().asUnsignedShort();
        line.noMoreTokens();
        if (OPTION(GENERATE_LINE_NUMBERS)) {
            LOG(M95,GENERATE_LINE_NUMBERS); // ".line directives ignored as %s specified"
            return;
        }
        stackLocals.visitLineNumber(line);
        Label label = new Label();  //  to get multiple line numbers. eg in jdk3/ArtificialStructures
        mnode.visitLabel(label);
        mnode.visitLineNumber(lineno, label);
    }

    private Object getAsmFrameType(FrameType ft,Line line) {
        Object frame;
        if (ft.extra()) {
            String arg = line.nextToken().asString(); // verification arg
            if (ft == FrameType.ft_Uninitialized) {
                frame = labelmap.codeUseOfJynxLabel(arg, line).asmlabel();
            } else {
                frame = arg;
            }
        } else {
            frame = ft.asmType();
        }
        return frame;
    }

    private void visitStackFrame(Line line) {
        List<Object> frame_local = new ArrayList<>();
        Token use = line.nextToken();
        if (use != Token.END_TOKEN) {
            use.mustBe(res_use);
            Token nstr = line.nextToken();
            int n;
            if (nstr.is(res_locals)) {
                n = localStack.size();
            } else {
                n = nstr.asUnsignedShort();
                line.lastToken().mustBe(res_locals);
                if (n > localStack.size()) {
                    LOG(M188,n, localStack.size());  // "n (%d) is greater than current local size(%d)"
                    n = localStack.size();
                }
            }
            int i = 0;
            for (Object obj:localStack) {
                if (i == n) {
                    break;
                }
                frame_local.add(obj);
                ++i;
            }
        }

        List<Object> frame_stack = new ArrayList<>();
        LinesIterator lines = new LinesIterator(js,Directive.end_stack);
        while (lines.hasNext()) {
            line = lines.next();
            Token token = line.firstToken();
            ReservedWord rw = token.expectOneOf(res_stack, res_locals);
            if (rw == null) {
                line.skipTokens();
                continue;
            }
            String type = line.nextToken().asString(); // verification type
            FrameType ft = FrameType.fromString(type);
            Object item = getAsmFrameType(ft,line);
            line.noMoreTokens();
            if (rw == res_stack) {
                frame_stack.add(item);
            } else {
                frame_local.add(item);
            }
        }
        Object[] stackarr = frame_stack.toArray();
        Object[] localarr = frame_local.toArray();
        stackLocals.visitFrame(stackarr, localarr,js.getLine());
        if (SUPPORTS(StackMapTable)) {
            mnode.visitFrame(Opcodes.F_NEW, localarr.length, localarr, stackarr.length, stackarr);
        }
        localStack = frame_local;
    }

    private void setLimit(Line line) {
        ReservedWord type = line.nextToken().oneOf(res_stack, res_locals);
        int num = line.lastToken().asUnsignedShort();
        if (type == res_locals) {
            stackLocals.locals().setLimit(num, line);
        } else {
            stackLocals.stack().setLimit(num, line);
        }
    }
    
    private void visitVar(Line line) {
        JynxVar jvar = JynxVar.getInstance(line, labelmap);
        vars.add(jvar);
    }
    
    private void acceptVars(MethodVisitor mv) {
        LOGGER().pushCurrent();
        for (JynxVar jvar:vars) {
            LOGGER().setLine(jvar.getLine().toString());
            boolean ok = stackLocals.visitVar(jvar);
            if (ok) {
                jvar.accept(mv);
            } else {
                LOG(M54,jvar.varnum()); // "variable %d has not been written to"
            }
        }
        LOGGER().popCurrent();
    }
    
    private void undefinedLabel(JynxLabel lr) {
        String usage = lr.used()
                .map(Line::toString)
                .collect(Collectors.joining(System.lineSeparator()));
        LOG(M266,lr.name(),usage); // "Label %s not defined; used in%n%s"
        mnode.visitLabel(lr.asmlabel());   // to prevent ASM error
    }

    private void unusedLabel(JynxLabel lr) {
        LOG(M272,lr.name(),lr.definedLine());    // "Label %s not used - defined in line:%n  %s"
    }
    
    public boolean visitEnd() {
        Line line = js.getLine();
        s2a.visitEnd();
        labelmap.end(mnode, line);
        stackLocals.visitEnd();
        acceptVars(mnode);
        labelmap.stream()
                .forEach(JynxLabel::checkPosition);
        labelmap.stream()
                .filter(lr->!lr.isDefined())
                .forEach(this::undefinedLabel);        
        if (OPTION(WARN_UNNECESSARY_LABEL)) {
            labelmap.stream()
                    .filter(lr->lr.isUnused())
                    .forEach(this::unusedLabel); 
        }
        boolean ok = LOGGER().numErrors() == errct_at_start;
        if (ok) {
            // must be called to generate stackmap etc.
            mnode.visitMaxs(stackLocals.stack().max(), stackLocals.locals().max());
        }

        return ok;
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation
        (int typeref, TypePath typepath, String desc, boolean visible) {
            Line line = js.getLine();
            AnnotationVisitor av;
            TypeRef tr = TypeRef.getInstance(typeref);
            switch (tr) {
                case trt_except:
                    av = visitTryCatchAnnotation(typeref, typepath, desc, visible);
                    break;
                 case tro_var:
                 case tro_resource:
                     av = visitLocalVariableAnnotation(line,typeref, typepath, desc, visible);
                     break;
                 default:
                     av = visitInsnAnnotation(tr, typeref, typepath, desc, visible);
                     break;
        }
        line.noMoreTokens();
        return av;
    }

    private AnnotationVisitor visitTryCatchAnnotation(int typeref, TypePath tp, String desc, boolean visible) {
        int index = TypeRef.getIndexFrom(typeref);
        if (index != 0 && index != tryct - 1) {
            // "index (%d) is not zero and does not refer to last %s directive (%d)"
            LOG(M335,index,Directive.dir_catch,tryct - 1);
        }
        if (index != tryct - 1) {
                TypeRef tr = TypeRef.getInstance(typeref);
                int[] indices = new int[]{tryct - 1};
                typeref = tr.getTypeRef(indices);
        }
        return mnode.visitTryCatchAnnotation(typeref, tp, desc, visible);
    }

    private static void checkAnnotatedInst(TypeRef tr, AsmOp lastjop) {
        EnumSet<AsmOp> lastjops = null;
        switch (tr) {
            case tro_cast:
                lastjops = EnumSet.of(AsmOp.asm_checkcast);
                break;
            case tro_instanceof:
                lastjops = EnumSet.of(AsmOp.asm_instanceof);
                break;
            case tro_new:
                lastjops = EnumSet.of(AsmOp.asm_new);
                break;
            case tro_argmethod:
                lastjops = EnumSet.of(AsmOp.asm_invokeinterface, AsmOp.asm_invokestatic, AsmOp.asm_invokevirtual);
                break;
            case tro_argnew:
                lastjops = EnumSet.of(AsmOp.asm_invokespecial);
                break;
        }
        if (lastjops != null) {
            if (lastjop == null || !lastjops.contains(lastjop)) {
                LOG(M232, lastjop, lastjops); // "Last instruction was %s: expected %s"
            }
        }
    }
    
    private AnnotationVisitor visitInsnAnnotation
        (TypeRef tr, int typeref, TypePath tp, String desc, boolean visible) {
            checkAnnotatedInst(tr, stackLocals.lastOp());
            return mnode.visitInsnAnnotation(typeref, tp, desc, visible);
    }

    private AnnotationVisitor visitLocalVariableAnnotation
        (Line line,int typeref, TypePath tp, String desc, boolean visible) {
            ArrayList<Integer> indexlist = new ArrayList<>();
            ArrayList<String> startlist = new ArrayList<>();
            ArrayList<String> endlist = new ArrayList<>();
            TokenArray array = TokenArray.getInstance(js, line);
            while(true) {
                Token token = array.firstToken();
                if (token.is(right_array)) {
                    break;
                }
                indexlist.add(token.asInt());
                startlist.add(array.nextToken().asString());
                endlist.add(array.nextToken().asString());
                array.noMoreTokens();
            }
            int[] index_arr = new int[indexlist.size()];
            Label[] start_arr = new Label[startlist.size()];
            Label[] end_arr = new Label[endlist.size()];
            for (int i = 0; i < index_arr.length; ++i) {
                index_arr[i] = indexlist.get(i);
                stackLocals.locals().typedVar(index_arr[i]);
                JynxLabel startref = labelmap.useOfJynxLabel(startlist.get(i), line);
                JynxLabel endref = labelmap.useOfJynxLabel(endlist.get(i), line);
                start_arr[i] = startref.asmlabel();
                end_arr[i] = endref.asmlabel();
            }
            return mnode.visitLocalVariableAnnotation(typeref, tp, start_arr, end_arr, index_arr, desc, visible);
    }

}

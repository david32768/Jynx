package asm;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.TypePath;

import static jvm.StandardAttribute.Code;
import static jvm.StandardAttribute.StackMapTable;
import static jynx.Global.*;
import static jynx.GlobalOption.*;
import static jynx.Message.*;
import static jynx.ReservedWord.*;

import jvm.Context;
import jvm.FrameType;
import jvm.JvmVersion;
import jvm.TypeRef;
import jynx.Directive;
import jynx.ReservedWord;
import jynx2asm.*;
import jynx2asm.frame.MethodParameters;
import jynx2asm.ops.JvmOp;

public class JynxCodeHdr implements ContextDependent {

    private final JynxScanner js;
    private List<Object> localStack;
    private final int errorsAtStart;

    private final StackLocals stackLocals;

    private final EnumMap<ReservedWord,Integer> options;
    private int printFlag = 0;
    
    private int tryct = 0;
    private final JynxLabelMap labelmap;
    private final List<JynxVar> vars;
    
    private final MethodNode mnode;

    private final String2Insn s2a;
    private int endif = 0;
    
    private final Map<Directive,Line> uniqueDirectives;
    
    private JynxCodeHdr(JynxScanner js, MethodNode mnode, StackLocals stackLocals,
            List<Object> localStack, String2Insn s2a) {
        
        this.js = js;
        this.errorsAtStart = LOGGER().numErrors();
        this.localStack = localStack;
        this.mnode = mnode;
        this.vars = new ArrayList<>();
        this.stackLocals = stackLocals;
        this.s2a = s2a;
        this.labelmap = s2a.getLabelMap();
        this.uniqueDirectives = new HashMap<>();
        this.options = new EnumMap<>(ReservedWord.class);
    }

    public static JynxCodeHdr getInstance(JynxScanner js, MethodNode mnode,
            MethodParameters parameters, String2Insn s2a) {
        
        CHECK_SUPPORTS(Code);
        StackLocals stackLocals = StackLocals.getInstance(parameters, s2a.getLabelMap());
        return new JynxCodeHdr(js, mnode, stackLocals, parameters.getInitFrame(), s2a);
    }

    @Override
    public Context getContext() {
        return Context.CODE;
    }

    @Override
    public void visitDirective(Directive dir, JynxScanner js) {
        Line line = js.getLine();
        dir.checkUnique(uniqueDirectives, line);
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
        JynxCatch jcatch =  labelmap.getCatch(fromname, toname, usingname, exception, line);
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
    
    private static final EnumSet<ReservedWord> PRINT_OPTIONS = EnumSet.of(res_expand,res_stack,res_locals,res_offset);

    private void setPrint(Line line) {
        ReservedWord rw  = line.nextToken().expectOneOf(res_on, res_off, res_label);
        switch (rw) {
            case res_label:
                String lab = line.nextToken().asString();
                String labelframe = labelmap.printJynxlabelFrame(lab, line);
                LOG(M995,labelframe); // "%s"
                break;
            case res_on:
                ++printFlag;
                Token token = line.nextToken();
                if (token == Token.END_TOKEN) {
                    PRINT_OPTIONS.forEach(opt -> options.putIfAbsent(opt,printFlag));
                } else {
                    while (token != Token.END_TOKEN) {
                        ReservedWord optrw = token.expectOneOf(PRINT_OPTIONS);
                        options.putIfAbsent(optrw,printFlag);
                        token = line.nextToken();
                    }
                }
                LOG(M293,options); // "print options = %s"
                Integer localsct = options.get(res_locals);
                if (localsct != null && localsct == 1) {
                    LOG(M294, res_locals, stackLocals.stringLocals()); // "%s = %s"
                }
                break;
            case res_off:
                if (printFlag > 0) {
                    PRINT_OPTIONS.stream()
                            .filter(opt -> (Objects.equals(printFlag, options.get(opt))))
                            .forEach(opt -> options.remove(opt));
                    --printFlag;
                    LOG(M329,options); // "print options = %s"
                } else {
                    // "%s nest level is already zero"
                    LOG(M328,Directive.dir_print);
                }
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
            js.skipNested(Directive.dir_if,Directive.end_if,
                    EnumSet.of(Directive.dir_line,Directive.dir_stack,Directive.end_stack));
        } else {
            ++endif;
        }
    }
    
    private void visitEndIf() {
        assert endif > 0;
        --endif;
    }

    private void visitInsn(Line line) {
        InstList instlist = new InstList(stackLocals,line,options);
        s2a.getInsts(instlist);
        instlist.accept(mnode);
    }
    
    private void visitLineNumber(Line line) {
        InstList instlist = new InstList(stackLocals,line,options);
        s2a.add(JvmOp.xxx_line, instlist);
        instlist.accept(mnode);
    }

    private Object getAsmFrameType(FrameType ft,Line line) {
        Object frame;
        if (ft.extra()) {
            String arg = line.nextToken().asString(); // verification arg
            switch (ft) {
                case ft_Uninitialized:
                    frame = labelmap.codeUseOfJynxLabel(arg, line).asmlabel();
                    break;
                case ft_Object:
                    NameDesc.FRAME_NAME.validate(arg);
                    frame = arg;
                    break;
                default:
                    throw new EnumConstantNotPresentException(ft.getClass(), ft.name());
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
                line.nextToken().mustBe(res_locals);
                if (n > localStack.size()) {
                    LOG(M188,n, localStack.size());  // "n (%d) is greater than current local size(%d)"
                    n = localStack.size();
                }
            }
            frame_local.addAll(localStack.subList(0, n));
        }
        line.noMoreTokens();
        Line dirline = line;

        List<Object> frame_stack = new ArrayList<>();
        try (LinesIterator lines = new LinesIterator(js,Directive.end_stack)) {
            while (lines.hasNext()) {
                line = lines.next();
                Token token = line.firstToken();
                ReservedWord rw = token.expectOneOf(res_stack, res_locals);
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
        }
        stackLocals.visitFrame(frame_stack, frame_local, dirline);
        if (SUPPORTS(StackMapTable) && OPTION(USE_STACK_MAP) && !OPTION(SYMBOLIC_LOCAL)) {
            Object[] stackarr = frame_stack.toArray();
            Object[] localarr = frame_local.toArray();
            mnode.visitFrame(Opcodes.F_NEW, localarr.length, localarr, stackarr.length, stackarr);
        }
        localStack = frame_local;
    }

    private void setLimit(Line line) {
        ReservedWord type = line.nextToken().expectOneOf(res_stack, res_locals);
        int num = line.lastToken().asUnsignedShort();
        if (type == res_locals) {
            stackLocals.locals().setLimit(num, line);
        } else {
            stackLocals.stack().setLimit(num, line);
        }
    }
    
    private void visitVar(Line line) {
        if (OPTION(SYMBOLIC_LOCAL)) {
            // "%s not supported if %s specified"
            LOG(M212,Directive.dir_var,SYMBOLIC_LOCAL);
            return;
        }
        JynxVar jvar = JynxVar.getInstance(line, labelmap);
        vars.add(jvar);
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
        stackLocals.acceptVarDirectives(mnode, vars);
        labelmap.checkCatch();
        labelmap.stream()
                .filter(lr->!lr.isDefined())
                .forEach(this::undefinedLabel);        
        if (OPTION(WARN_UNNECESSARY_LABEL)) {
            labelmap.stream()
                    .filter(lr->lr.isUnused())
                    .forEach(this::unusedLabel); 
        }
        boolean ok = LOGGER().numErrors() == errorsAtStart;
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
            TypeRef tr = TypeRef.fromASM(typeref);
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
        if (index < 0 || index >= tryct) {
            // "index (%d) is not a current try index [0,%d]"
            LOG(M335,index,tryct - 1);
        }
        return mnode.visitTryCatchAnnotation(typeref, tp, desc, visible);
    }

    private static void checkAnnotatedInst(TypeRef tr, JvmOp lastjop) {
        EnumSet<JvmOp> lastjops = null;
        switch (tr) {
            case tro_cast:
                lastjops = EnumSet.of(JvmOp.asm_checkcast);
                break;
            case tro_instanceof:
                lastjops = EnumSet.of(JvmOp.asm_instanceof);
                break;
            case tro_new:
                lastjops = EnumSet.of(JvmOp.asm_new);
                break;
            case tro_argmethod:
                lastjops = EnumSet.of(JvmOp.asm_invokeinterface, JvmOp.asm_invokestatic, JvmOp.asm_invokevirtual);
                break;
            case tro_argnew:
                lastjops = EnumSet.of(JvmOp.asm_invokespecial);
                break;
        }
        if (lastjops != null) {
            if (lastjop == null || !lastjops.contains(lastjop)) {
                if (JVM_VERSION().compareTo(JvmVersion.V9) < 0) {
                    LOG(M231, lastjop, lastjops); // "Last instruction was %s: expected %s"
                } else {
                    LOG(M232, lastjop, lastjops); // "Last instruction was %s: expected %s"
                }
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
            try (TokenArray array = line.getTokenArray()) {
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
            }
            int[] index_arr = new int[indexlist.size()];
            Label[] start_arr = new Label[startlist.size()];
            Label[] end_arr = new Label[endlist.size()];
            for (int i = 0; i < index_arr.length; ++i) {
                index_arr[i] = indexlist.get(i);
                stackLocals.visitVarAnnotation(index_arr[i]);
                JynxLabel startref = labelmap.useOfJynxLabel(startlist.get(i), line);
                JynxLabel endref = labelmap.useOfJynxLabel(endlist.get(i), line);
                start_arr[i] = startref.asmlabel();
                end_arr[i] = endref.asmlabel();
            }
            return mnode.visitLocalVariableAnnotation(typeref, tp, start_arr, end_arr, index_arr, desc, visible);
    }

}

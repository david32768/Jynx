package jynx2asm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static jynx.Global.LOG;
import static jynx.Global.OPTION;
import static jynx.Global.SUPPORTS;
import static jynx.GlobalOption.WARN_UNNECESSARY_LABEL;
import static jynx.Message.*;

import asm.instruction.Instruction;
import asm.instruction.LineInstruction;
import asm.JynxVar;
import jvm.Feature;
import jynx.GlobalOption;
import jynx.LogAssertionError;
import jynx2asm.handles.JynxHandle;
import jynx2asm.handles.MethodHandle;
import jynx2asm.ops.JvmOp;

public class StackLocals {

    public enum Last {
        OP("op"),
        LABEL("label"),
        LINE(".line"),
        FRAME(".stack"),
        ;
        
        private final String extname;

        private Last(String extname) {
            this.extname = extname;
        }
        
        @Override
        public String toString() {
            return extname;
        }
    }
    
    private final LocalVars locals;
    private final OperandStack stack;
    private final JynxLabelMap labelmap;

    private final JvmOp returnOp;
    private boolean returns;
    private boolean hasThrow;

    private boolean frameRequired;
    
    private Optional<JynxLabel> lastLab;
    private JvmOp lastop;
    private Last completion;
    private final List<JynxLabel> activeLabels;

    
    private StackLocals(LocalVars locals, OperandStack stack, JynxLabelMap labelmap, JvmOp returnop) {
        this.locals = locals;
        this.stack = stack;
        this.labelmap = labelmap;
        this.returnOp = returnop;
        this.returns = false;
        this.hasThrow = false;
        this.lastLab = Optional.empty();
        this.lastop = JvmOp.asm_nop;
        this.frameRequired = false;
        this.completion = Last.OP;
        this.activeLabels = new ArrayList<>();
    }
    
    public static StackLocals getInstance(List<Object> localstack, JynxLabelMap labelmap,
            JvmOp returnop, boolean isStatic) {
        LocalVars lv = LocalVars.getInstance(localstack,isStatic);
        OperandStack os = OperandStack.getInstance(localstack);
        return new StackLocals(lv, os, labelmap, returnop);
    }

    public LocalVars locals() {
        return locals;
    }

    public OperandStack stack() {
        return stack;
    }

    public JvmOp getReturnOp() {
        return returnOp;
    }

    public Optional<JynxLabel> lastLab() {
        return lastLab;
    }

    public JvmOp lastOp() {
        return lastLab.isPresent()?null:lastop;
    }

    private boolean isUnreachable() {
        return lastop.isUnconditional() && !lastLab.isPresent();
    }
    
    // unreachable unless only backward references 
    public boolean isUnreachableForwards() {
        boolean lastgoto = lastop.isUnconditional();
        boolean usedlab = lastLab.isPresent() && lastLab.get().isUsedInCode();
        return lastgoto && !usedlab;
    }
    
    public void visitTryCatchBlock(JynxCatch jcatch) {
        JynxLabel usingref = jcatch.usingLab();
        stack.checkCatch(usingref);
        usingref.addCatch(jcatch);
    }
    
    public void startBlock() {
        lastLab = Optional.empty();
        stack.startBlock();
        locals.startBlock();
        activeLabels.clear();
    }
    
    public void visitLabel(JynxLabel target) {
        if (target.isCatch()) {
            target.visitCatch();
        }
        if (lastLab.isPresent()) { // new label is alias
            JynxLabel base = lastLab.get();
            if (OPTION(WARN_UNNECESSARY_LABEL)) {
                LOG(M220,target.name(),base.name()); // "label %s is an alias for label %s"
            }
            labelmap.aliasJynxLabel(target.name(), target.definedLine(), base);
            target.aliasOf(base);
            locals.visitAlias(target, base,lastLab);
            stack.visitAlias(target, base);
        } else {
            stack.visitLabel(target);
            locals.visitLabel(target, lastLab);
            lastLab = Optional.of(target);
            activeLabels.add(target);
            frameRequired = lastop.isUnconditional();
            if (target.isUsedInCode()) {
                completion = Last.LABEL;
            }
        }
    }

    public void visitLineNumber(Line line) {
        if (lastLab.isPresent()) {
            JynxLabel jlabel = labelmap.weakUseOfJynxLabel(lastLab.get(), line);
        }
        completion = Last.LINE;
    }
    
    public void visitFrame(List<Object> stackarr, List<Object> localarr, Line line) {
        if (lastLab.isPresent()) {
            labelmap.weakUseOfJynxLabel(lastLab.get(), line);
        }
        stack.visitFrame(OperandStackFrame.getInstance(stackarr, true),lastLab);
        locals.visitFrame(OperandStackFrame.getInstance(localarr,false),lastLab);
        frameRequired = false;
        completion = Last.FRAME;
    }
    
    private void visitPreJvmOp(JvmOp asmop) {
        locals.preVisitJvmOp(lastLab);
        stack.preVisitJvmOp(lastLab);
        lastLab = Optional.empty();
        lastop = asmop;
    }

    private void visitPostJvmOp(JvmOp asmop) {
        boolean startblock = asmop.isUnconditional();
        if (startblock) {
            startBlock();
        }
        completion = Last.OP;
    }

    public boolean visitInsn(Instruction in, Line line) {
        if (in == null) {
            return false;
        }
        if (in instanceof LineInstruction) {
            visitLineNumber(line);
            return !isUnreachable();
        }
        JvmOp jvmop = in.resolve(this);
        if (jvmop.opcode() < 0) { // label or try
            in.adjust(this);
            return true;
        }
        if (jvmop.isReturn()) {
            if (jvmop == returnOp) {
                returns = true;
            } else {
                LOG(M191, returnOp, jvmop); // "method requires %s but found %s"
                return false;
            }
        }
        hasThrow |= jvmop == JvmOp.asm_athrow; 
        if (isUnreachable()) {
            if (jvmop.isUnconditional()) {
                LOG(M122,jvmop,lastop);  // "Instruction '%s' dropped as unreachable after '%s' without intervening label"
            } else {
                LOG(M121,jvmop,lastop);  // "Instruction '%s' dropped as unreachable after '%s' without intervening label"
            }
            return false;
        } else {
            if (jvmop == JvmOp.asm_new && lastLab.isPresent()) {
                labelmap.weakUseOfJynxLabel(lastLab.get(), line);
            }
            if (frameRequired && SUPPORTS(Feature.stackmap) && OPTION(GlobalOption.USE_STACK_MAP)) {
                    LOG(M124);  // "stack frame is definitely required here"
            }
            frameRequired = false; // to prevent multiple error messages
            visitPreJvmOp(jvmop);
            in.adjust(this);
            visitPostJvmOp(jvmop);
            return true;
        }
    }

    public boolean visitVarDirective(JynxVar jvar) {
        return locals().visitVarDirective(FrameElement.fromDesc(jvar.desc()), jvar.varnum());
    }
    
    public void visitVarAnnotation(int num) {
        locals.visitVarAnnotation(num);
    }
    
    public void visitEnd() {
        locals.visitEnd();
        if(!returns && (returnOp != JvmOp.asm_return || !hasThrow)) {
            LOG(M196,returnOp); // "no %s instruction found"
        }
        switch(completion) {
            case OP:
                if (!lastop.isUnconditional()) {
                    LOG(M208,completion,lastop); // "code not complete - last %s was %s"
                }
                break;
            case LABEL:
                JynxLabel jlabel = lastLab.get();
                LOG(M208,completion,jlabel.name()); // "code not complete - last %s was %s"
                break;
            case LINE:
            case FRAME:
                LOG(M209,completion);// "code not complete - last was %s"
                break;
            default:
                throw new AssertionError();
        }
    }

    private void updateLocal(JynxLabel label, LocalFrame osf) {
        label.updateLocal(osf);
    }
    

    private void updateLocal(JynxLabel label) {
        LocalFrame osf = locals.currentFrame();
        updateLocal(label,osf);
    }
    
    public void adjustLabel(JynxLabel label, boolean jsr) {
        stack.checkStack(label, jsr);
        updateLocal(label);
    }
    
    private void updateLocal(JynxLabel dflt,Collection<JynxLabel> labels) {
        LocalFrame osf = locals.currentFrame();
        updateLocal(dflt,osf);
        for (JynxLabel label:labels) {
            updateLocal(label,osf);
        }
    }

    private void checkStack(JynxLabel dflt, Collection<JynxLabel> labels) {
        OperandStackFrame osf = stack.currentFrame();
        stack.checkStack(dflt,osf);
        for (JynxLabel label:labels) {
            stack.checkStack(label,osf);
        }
    }
    

    public void adjustLabels(JynxLabel dflt, Collection<JynxLabel> labels) {
        checkStack(dflt, labels);
        updateLocal(dflt, labels);
    }

    public void adjustStackOperand(String desc) {
        stack.adjustOperand(desc);
    }
    
    public void adjustStackOperand(JvmOp jvmop, JynxHandle mh) {
        stack.adjustInvoke(jvmop, mh);
    }
    
    public void checkIncr(int var) {
        locals.checkChar('I', var);
    }
    
    public void adjustLoadStore(JvmOp jop, int var) {
        char ctype = jop.vartype();
        if (jop.isStoreVar()) {
            FrameElement fe = stack.storeType(ctype, var);
            locals.storeFrameElement(fe,var);
            for (JynxLabel lab:activeLabels) {
                lab.store(fe,var);
            }
        } else {
            FrameElement fe = locals.loadType(ctype,var);
            for (JynxLabel lab:activeLabels) {
                lab.load(fe,var);
            }
            stack.load(fe, var);
        }
    }
    
    public void adjustStack(JvmOp jop) {
        String opdesc = jop.desc();
        if (opdesc == null) {
            throw new AssertionError("" + jop);
        } else {
            stack.adjustDesc(opdesc);
        }
    }
    
    public void adjustStackOp(JvmOp jop) {
        if (jop.isStack()) {
            stack.adjustStackOp(jop);
        } else {
            throw new AssertionError("" + jop);
        }
    }
    
    public String stringLocals() {
        return locals.stringForm();
    }
    
    public String stringStack() {
        return stack.stringForm();
    }
}

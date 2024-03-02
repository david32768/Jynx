package jynx2asm.frame;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.objectweb.asm.Type;

import static jynx.Directive.dir_stack;
import static jynx.Global.*;
import static jynx.Message.*;
import static jynx.Message.M163;

import jynx.LogAssertionError;
import jynx.LogIllegalStateException;
import jynx2asm.FrameElement;
import jynx2asm.handles.JynxHandle;
import jynx2asm.JynxLabel;
import jynx2asm.LimitValue;
import jynx2asm.Line;
import jynx2asm.ops.JvmOp;

public class OperandStack {

    private static final int MAXSTACK = 1 << 16;

    private final Deque<FrameElement> stack;
    private final LimitValue stacksz;
    
    private int sz;
    private boolean startblock;
    
    private OperandStack() {
        this.stacksz = new LimitValue(LimitValue.Type.stack);
        this.stack = new ArrayDeque<>();
        this.sz = 0;
        this.startblock = false;
    }

    public static OperandStack getInstance() {
        return new OperandStack();
    }
    
    public void setLimit(int num, Line line) {
        stacksz.setLimit(num, line);        
    }
    
    public int max() {
        return stacksz.checkedValue();
    }
    
    public void startBlock() {
        clear();
        startblock = true;
    }

    private void clear() {
        sz = 0;
        stack.clear();
    }
    
    public FrameElement peekTOS() {
        if (stack.isEmpty()) {
            assert sz == 0;
            return FrameElement.ERROR;
        }
        return stack.peekLast();
    }
    
    private FrameElement pop() {
        if (stack.isEmpty()) {
            throw new LogIllegalStateException(M163); // "stack underflow"
        }
        FrameElement fe = stack.removeLast();
        sz -=fe.slots();
        return fe;
    }
    
    private FrameElement checkType(FrameElement tos, FrameElement required) {
        if (tos.matchStack(required)) {
            return tos;
        } else {
            LOG(M182,tos,required); // "top of stack is %s but required is %s"
            return required;
        }
    }
    
    private void pop(FrameElement required) {
        FrameElement fe = pop();
        checkType(fe, required);
    }

    private void push(FrameElement fe) {
        sz += fe.slots();
        if (sz >= MAXSTACK) {
            LOG(M164); // "stack overflow"
            throw new IllegalStateException();
        }
        stacksz.adjust(sz);
        stack.addLast(fe);
    }
    
    private void pushArray(FrameElement[] fes) {
        for (FrameElement fe:fes) {
            push(fe);
        }
    }
    
    private FrameElement[] pop32() {
        FrameElement tos = pop();
        if (tos.isTwo()) {
            LOG(M166,tos); // "top of stack(%s) is not a 32 bit type"
            tos = FrameElement.ERROR;
        }
        return new FrameElement[]{tos};
    }
    
    private FrameElement[] pop64() {
        FrameElement tos = pop();
        if (tos.isTwo()) {
            return new FrameElement[]{tos};
        }
        FrameElement nos = pop();
        if (nos.isTwo()) {
            LOG(M180, nos, nos); // "top of stack(%s) and next on stack('%c') are not both 32 bit types"
            nos = FrameElement.ERROR;
        }
        return new FrameElement[]{nos,tos};
    }
    
    private void setStack(OperandStackFrame osf) {
        clear();
        osf.stream()
                .forEach(this::push);
    }
    
    private void adjust(FrameElement[] parms, Optional<FrameElement> rfe) {
        for (int i = parms.length - 1; i >= 0; --i) {
            FrameElement required = parms[i];
            pop(required);
        }
        rfe.ifPresent(this::push);
    }

    public void adjustDesc(String desc) {
        assert desc.charAt(0) == '(';
        assert desc.charAt(desc.length() - 2) == ')';
        String parmstr = desc.substring(1, desc.length() - 2);
        FrameElement[] parms = parmstr.chars()
                .mapToObj(c -> FrameElement.fromStack((char)c))
                .toArray(FrameElement[]::new);
        char rtype = desc.charAt(desc.length() - 1);
        Optional<FrameElement> rfe = FrameElement.fromReturn(rtype);
        adjust(parms, rfe);
    }

    public void adjustInvoke(JvmOp jvmop, JynxHandle mh) {
        String desc = mh.desc();
        String stackdesc;
        switch (jvmop) {
            case asm_invokestatic:
                stackdesc = desc;
                break;
            case asm_invokespecial:
            case asm_invokeinterface:
            case asm_invokevirtual:
                String ownerL = mh.ownerL();
                stackdesc = String.format("(%s%s",ownerL,desc.substring(1));
                break;
            default:
                // "unexpected Op %s in this instruction"),
                throw new LogAssertionError(M908,jvmop.name());
        }
        adjustOperand(stackdesc);
    }
    
    public void adjustOperand(String desc) {
        Type mt = Type.getMethodType(desc);
        Type[] args = mt.getArgumentTypes();
        FrameElement[] parms = Stream.of(args)
                .map(FrameElement::fromType)
                .toArray(FrameElement[]::new);
        Type rtype = mt.getReturnType();
        Optional<FrameElement> rfe = FrameElement.fromReturnType(rtype);
        adjust(parms, rfe);
    }

    public void adjustStackOp(JvmOp op) {
        String manstr = op.stackManipulate();
        int arrowidx = manstr.indexOf("->");
        FrameElement[] tos = null;
        FrameElement[] nos = null;
        int idx = arrowidx;
        while (--idx >= 0) {
            switch (manstr.charAt(idx)) {
                case 't':
                    assert tos == null && nos == null;
                    tos = pop32();
                    break;
                case 'T':
                    assert tos == null && nos == null;
                    tos = pop64();
                    break;
                case 'n':
                    assert tos != null && nos == null;
                    nos = pop32();
                    break;
                case 'N':
                    assert tos != null && nos == null;
                    nos = pop64();
                    break;
                default:
                    throw new AssertionError();
            }
        }
        assert tos != null;
        String to = manstr.substring(arrowidx + 2);
        for (char ch:to.toCharArray()) {
            switch(ch) {
                case 't':
                case 'T':
                    pushArray(tos);
                    break;
                case 'n':
                case 'N':
                    assert nos != null;
                    pushArray(nos);
                    break;
                default:
                    throw new AssertionError();
            }
        }
    }

    public boolean visitLabel(JynxLabel label) {
        if (startblock) {
            OperandStackFrame osfstack = label.getStack();
            if (osfstack == null) {
            } else {
                setStack(osfstack);
                startblock = false;
            }
        } else {
            checkStack(label);
        }
        return startblock;
    }

    public void visitAlias(JynxLabel alias, JynxLabel base) {
        visitLabel(base);
    }
    
    public void visitFrame(List<Object> stackarr, Optional<JynxLabel> lastLab) {
        OperandStackFrame framesf = OperandStackFrame.getInstance(stackarr);
        OperandStackFrame osf = currentFrame();
        if (!startblock && !framesf.isEquivalent(osf)) {
            LOG(M184,osf,dir_stack,framesf); // "current stack is %s but %s is %s"
            startblock = true; // set so as to use following if; startblock is set to false at end of this metohd
        }
        if (startblock) {
            setStack(framesf);
            lastLab.ifPresent(lab->checkStack(lab,framesf));
        }
        startblock = false;
    }

    public void preVisitJvmOp(Optional<JynxLabel> lastLab) {
        if (startblock) {
            setStack(OperandStackFrame.EMPTY);
            lastLab.ifPresent(lab->checkStack(lab,OperandStackFrame.EMPTY));
            startblock = false;
        }
    }
    
    public void checkStack(JynxLabel label, OperandStackFrame osf) {
        label.updateStack(osf);
    }

    public OperandStackFrame currentFrame() {
        FrameElement[] array = stack.toArray(FrameElement[]::new);
        return new OperandStackFrame(array);
    }
    
    private void checkStack(JynxLabel label) {
        OperandStackFrame osf = currentFrame();
        checkStack(label,osf);
    }

    public void checkCatch(JynxLabel label) {
        checkStack(label,OperandStackFrame.EXCEPTION);
    }
    
    public void checkStack(JynxLabel label, boolean jsr) {
        if (jsr) {
            push(FrameElement.RETURN_ADDRESS);
        }
        checkStack(label);
        if (jsr) {
            pop();
        }
    }
    
    // aload cannot be used to load a ret address
    public void load(FrameElement fe,int var) {
        if (fe == FrameElement.RETURN_ADDRESS) { // ret instruction
            // intentionally no code
        } else {
            push(fe);
        }
    }
    
    public FrameElement storeType(char type) {
        FrameElement fe = pop();
        if (fe == FrameElement.RETURN_ADDRESS && type == 'A') {
            type = 'R';
        }
        FrameElement required = FrameElement.fromStack(type);
        fe = checkType(fe, required);
        return fe;
    }
    
    public String stringForm() {
        return currentFrame().stringForm();
    }

    @Override
    public String toString() {
        return stringForm();
    }

}

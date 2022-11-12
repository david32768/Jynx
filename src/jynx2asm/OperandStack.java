package jynx2asm;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.objectweb.asm.Type;

import static jynx.Directive.dir_stack;
import static jynx.Global.*;
import static jynx.Message.*;
import static jynx.Message.M163;

import jynx.LogAssertionError;
import jynx.LogIllegalStateException;
import jynx2asm.handles.JynxHandle;
import jynx2asm.ops.JvmOp;

public class OperandStack {

    private static final int MAXSTACK = 1 << 16;

    private final FrameElement[] stack;
    private final LimitValue stacksz;
    private final boolean isInit;
    
    private int endpos;
    private int sz;
    private boolean startblock;
    
    private OperandStack(boolean isinit) {
        this.isInit = isinit;
        this.stacksz = new LimitValue(LimitValue.Type.stack);
        this.stack = new FrameElement[MAXSTACK];
        this.endpos = 0;
        this.sz = 0;
        this.startblock = false;
    }

    public static OperandStack getInstance(List<Object> parms) {
        boolean isinit = !parms.isEmpty() && parms.get(0) == FrameElement.THIS;
        return new OperandStack(isinit);
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
        endpos = 0;
        sz = 0;
        Arrays.fill(stack, null);
    }
    
    public FrameElement peekTOS() {
        int pos = endpos - 1;
        if (pos < 0) {
            assert sz == 0;
            return FrameElement.ERROR;
        }
        return stack[pos];
    }
    
    private FrameElement pop() {
        int pos = endpos - 1;
        if (pos < 0) {
            throw new LogIllegalStateException(M163); // "stack underflow"
        }
        FrameElement fe = stack[pos];
        stack[pos] = null;
        --endpos;
        sz -=fe.slots();
        return fe;
    }
    
    private FrameElement checkType(char type, FrameElement fe) {
        char tos = fe.typeLetter();
        if (tos == type || type == 'A' && fe == FrameElement.THIS) {
            return fe;
        } else {
            FrameElement required = FrameElement.fromStack(type);
            LOG(M182,fe,required); // "top of stack is %s but required is %s"
            return required;
        }
    }
    
    private char popChar(char type) {
        FrameElement fe = pop();
        fe = checkType(type,fe);
        return fe.typeLetter(); 
    }

    private void push(FrameElement fe) {
        sz += fe.slots();
        if (sz >= MAXSTACK) {
            LOG(M164); // "stack overflow"
            throw new IllegalStateException();
        }
        stacksz.adjust(sz);
        stack[endpos] = fe;
        ++endpos;
    }
    
    private void pushChar(char x) {
        if (x == 'V') {
            return;
        }
        push(FrameElement.fromStack(x));
    }
    
    private void pushString(String str) {
        for (int i = 0; i < str.length(); ++i) {
            pushChar(str.charAt(i));
        }
    }
    
    private String pop32() {
        FrameElement fe = pop();
        char tos = fe.typeLetter();
        if (fe.isTwo()) {
            LOG(M166,tos); // "top of stack('%c') is not a 32 bit type"
            tos = FrameElement.ERROR.typeLetter();
        }
        return String.valueOf(tos);
    }
    
    private String pop64() {
        FrameElement fe = pop();
        char tos = fe.typeLetter();
        String result = String.valueOf(tos);
        if (fe.isTwo()) {
            return result;
        }
        fe = pop();
        char nos = fe.typeLetter();
        if (fe.isTwo()) {
            LOG(M180, tos, nos); // "top of stack('%c') and next on stack('%c') are not both 32 bit types"
            nos = FrameElement.ERROR.typeLetter();
        }
        result = nos + result;
        return result;
    }
    
    private void setStack(OperandStackFrame osf) {
        clear();
        osf.stream()
                .forEach(this::push);
    }
    
    public void adjust(String parms, char rt) {
        for (int i = parms.length() - 1; i >= 0; --i) {
            char parm = parms.charAt(i); 
            popChar(parm);
        }
        pushChar(rt);
    }

    public void adjustDesc(String desc) {
        assert desc.charAt(0) == '(';
        assert desc.charAt(desc.length() - 2) == ')';
        String parms = desc.substring(1, desc.length() - 2);
        adjust(parms,desc.charAt(desc.length() - 1));
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
    
    private char typeLetter(Type type) {
        if (type.equals(Type.VOID_TYPE)) {
            return 'V';
        }
        return FrameElement.fromType(type).typeLetter();
    }

    public void adjustOperand(String desc) {
        Type mt = Type.getMethodType(desc);
        Type[] args = mt.getArgumentTypes();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; ++i) {
            char parm = typeLetter(args[i]);
            sb.append(parm);
        }
        adjust(sb.toString(),typeLetter(mt.getReturnType()));
    }

    public void adjustStackOp(JvmOp op) {
        String manstr = op.stackManipulate();
        int arrowidx = manstr.indexOf("->");
        String tos = null;
        String nos = null;
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
                    pushString(tos);
                    break;
                case 'n':
                case 'N':
                    assert nos != null;
                    pushString(nos);
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
    
    public void visitFrame(OperandStackFrame framesf, Optional<JynxLabel> lastLab) {
        OperandStackFrame osf = new OperandStackFrame(stack, endpos);
        if (!startblock && !framesf.equals(osf)) {
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
        return new OperandStackFrame(stack, endpos);
    }
    
    private void checkStack(JynxLabel label) {
        OperandStackFrame osf = new OperandStackFrame(stack, endpos);
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
    
    public FrameElement storeType(char type,int var) {
        FrameElement fe = pop();
        if (fe == FrameElement.RETURN_ADDRESS && type == 'A') {
            type = 'R';
        }
        fe = checkType(type,fe);
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

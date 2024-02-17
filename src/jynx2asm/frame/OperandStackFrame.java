package jynx2asm.frame;

import java.util.List;

import jynx2asm.FrameClass;
import jynx2asm.FrameElement;

public class OperandStackFrame extends ConstantFrameArray {

    public static final OperandStackFrame EMPTY = new OperandStackFrame();
    public static final OperandStackFrame EXCEPTION = new OperandStackFrame(FrameElement.OBJECT);
    
    public OperandStackFrame(FrameElement... fes) {
        this(fes,fes.length);
    }
  
    public OperandStackFrame(FrameElement[] stack, int sz) {
        super(stack, sz, FrameClass.STACK);
    }

    public static OperandStackFrame getInstance(List<Object> objs) {
        FrameElement[] framestack = getFrameArray(objs);
        return new OperandStackFrame(framestack);
    }
    
}

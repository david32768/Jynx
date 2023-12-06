package jynx2asm.frame;

import java.util.List;

import jynx2asm.FrameClass;
import jynx2asm.FrameElement;

public class StackMapLocals extends ConstantFrameArray {

    public static final StackMapLocals EMPTY = new StackMapLocals();
    public static final StackMapLocals EXCEPTION = new StackMapLocals(FrameElement.EXCEPTION);
    
    public StackMapLocals(FrameElement... fes) {
        this(fes,fes.length);
    }
  
    public StackMapLocals(FrameElement[] stack, int sz) {
        super(stack, sz, FrameClass.MAPLOCALS);
    }

    public static StackMapLocals getInstance(List<Object> objs) {
        FrameElement[] framestack = getFrameArray(objs);
        return new StackMapLocals(framestack);
    }
    
}

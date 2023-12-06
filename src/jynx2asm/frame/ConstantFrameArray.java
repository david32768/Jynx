package jynx2asm.frame;

import java.util.List;
import jvm.FrameType;
import jynx2asm.FrameClass;
import jynx2asm.FrameElement;

public class ConstantFrameArray extends FrameArray {

    protected ConstantFrameArray(FrameElement[] array, int sz, FrameClass fc) {
        super(array, sz, fc);
    }

    @Override
    public final void set(int index, FrameElement fe) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public final void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void set(FrameArray fa) {
        throw new UnsupportedOperationException();
    }
    
    protected static FrameElement[]  getFrameArray(List<Object> objs) {
        FrameElement[] framestack = new FrameElement[objs.size()];
        int i = 0;
        for (Object obj:objs) {
            FrameType ft = FrameType.fromObject(obj);
            FrameElement fe =  FrameElement.fromFrame(ft);
            framestack[i++] = fe;
        }
        return framestack;
    }
    
}

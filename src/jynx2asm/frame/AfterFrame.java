package jynx2asm.frame;

import jynx2asm.FrameElement;

public class AfterFrame extends FrameArray {

    private final int limit;
    
    public AfterFrame(int limit) {
        super(limit);
        this.limit = limit;
    }

    @Override
    public FrameElement atUnchecked(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void set(int index, FrameElement fe) {
        int slots = fe.slots();
        if (index <= limit - slots && at(index) == FrameElement.UNUSED
                && (slots == 1 || at(index + 1)  == FrameElement.UNUSED)) {
            super.set(index, fe);
        }
    }

    @Override
    public void set(FrameArray fa) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public boolean isCompatibleWith(FrameArray fa2) {
        throw new UnsupportedOperationException();
    }
    
}

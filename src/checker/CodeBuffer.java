package checker;

import java.nio.ByteBuffer;
import jvm.Context;

public class CodeBuffer extends AbstractCodeBuffer {
    
    private int stackmapOffset;

    protected CodeBuffer(ConstantPool pool, ByteBuffer bb, String name,
            int maxlocal, CodeLabels labels) {
        super(pool, bb, name, maxlocal,  labels);
        stackmapOffset = -1;
    }
    
    @Override
    public CodeBuffer asCodeBuffer() {
        return this;
    }
    
    @Override
    public CodeBuffer attributeBuffer(Context context, String name, int size) {
        assert context ==  Context.CODE;
        return new CodeBuffer(pool, extract(size), name, maxlocal, labels);
    }
    
    public int nextLabel() {
        return labels.labelOffset(0, nextUnsignedShort());
    }
    
    public int nextEndOffset(int pc) {
        return labels.labelOffset(pc, nextUnsignedShort());
    }
    
    public int nextVar() {
        int lvindex = nextUnsignedShort();
        checkLocalVar(lvindex);
        return lvindex;
    }
    
    public void addDelta(int delta) {
        if (delta < 0 || delta > 2*Short.MAX_VALUE + 1) {
            throw new AssertionError();
        }
        ++delta; // as stackmappOffset is initialised to -1
        stackmapOffset = labels.labelOffset(stackmapOffset, delta);
    }

}

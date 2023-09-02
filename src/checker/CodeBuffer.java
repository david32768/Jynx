package checker;

import java.nio.ByteBuffer;
import java.util.BitSet;

import static jynx.Global.LOG;
import static jynx.Message.M501;
import static jynx.Message.M504;
import static jynx.Message.M518;
import static jynx.Message.M524;

import jvm.Context;

public class CodeBuffer extends AttributeBuffer {
    
    private final int maxlocal;
    private final int codesz;
    private final BitSet poslabels;
    private final BitSet actlabels;
    
    private int stackmapOffset;

    public CodeBuffer(ConstantPool pool, ByteBuffer bb, int maxlocal, int codesz) {
        this(pool, bb, maxlocal, codesz, new BitSet(codesz + 1), new BitSet(codesz + 1), -1);
    }

    private CodeBuffer(ConstantPool pool, ByteBuffer bb,
            int maxlocal, int codesz, BitSet poslabels, BitSet actlabels, int stackmapOffset) {
        super(pool, bb, Context.CODE);
        this.maxlocal = maxlocal;
        this.codesz = codesz;
        this.poslabels = poslabels;
        this.actlabels = actlabels;
        this.stackmapOffset = stackmapOffset;
    }
    
    @Override
    protected CodeBuffer duplicate() {
        return new CodeBuffer(pool(), bb().duplicate(), maxlocal, codesz, poslabels, actlabels, stackmapOffset);
    }

    @Override
    public CodeBuffer extract(int size) {
        CodeBuffer partbuff = duplicate();
        advance(size);
        partbuff.limit(position());
        return partbuff;
    }
    
    @Override
    public CodeBuffer asCodeBuffer() {
        return this;
    }
    
    @Override
     public AttributeBuffer attributeBuffer(Context context) {
        assert context == Context.CODE;
        return this;
    }
    
    
    private int labelOffset(int instoff, int broff) {
        int offset = Math.addExact(instoff, broff);
        if (offset < 0 || offset > codesz) {
            // "label offset (%d) is negative or greater than code size (%d)"
            LOG(M501, offset, codesz);
            offset = 0;
        } else if ((broff  < 0 || poslabels.get(codesz)) && !poslabels.get(offset)) {
            // "offset %d is not an instruction"
            LOG(M504, offset);
        }
        actlabels.set(offset);
        return offset;
    }
    
    public int nextBranchLabel(int instoff) {
        return labelOffset(instoff, nextInt());
    }
    
    public int nextIfLabel(int instoff) {
        return labelOffset(instoff, nextShort());
    }
    
    public int nextLabel() {
        return labelOffset(0, nextUnsignedShort());
    }
    
    public int nextEndOffset(int pc) {
        return labelOffset(pc, nextUnsignedShort());
    }
    
    public void checkLocalVar(int var) {
        if (var >= maxlocal) {
            // "local variable %d is >= max locals %d"
            LOG(M518, var, maxlocal);
        }
    }
    
    public int nextVar() {
        int lvindex = nextUnsignedShort();
        checkLocalVar(lvindex);
        return lvindex;
    }
    
    public void setPosLabel(int instoff) {
        assert instoff < codesz;
        poslabels.set(instoff);
    }
   
    public void align4(int offset) {
        int align = offset & 0x3;
        int padding = align == 0?0:4 - align;
        advance(padding );       
    }
    
    public void addDelta(int delta) {
        if (delta < 0 || delta > 2*Short.MAX_VALUE + 1) {
            throw new AssertionError();
        }
        ++delta; // as stackmappOffset is initialised to -1
        stackmapOffset = labelOffset(stackmapOffset, delta);
    }
    
    public void checkLabels() {
        BitSet badlabels = (BitSet)actlabels.clone();
        badlabels.andNot(poslabels);
        if (!badlabels.isEmpty()) {
            // "branches to middle of instruction - %s"
            LOG(M524,badlabels.toString());
        }
        poslabels.set(codesz);
    }
}

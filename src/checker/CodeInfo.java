package checker;

import java.util.BitSet;
import static jynx.Global.LOG;
import static jynx.Message.M501;
import static jynx.Message.M504;
import static jynx.Message.M518;
import static jynx.Message.M524;

public class CodeInfo {
    
    private final int maxlocal;
    private final int codesz;
    private final BitSet poslabels;
    private final BitSet actlabels;
    
    private Buffer buffer;
    private int stackmapOffset;

    public CodeInfo(int maxlocal, int codesz) {
        this (null, maxlocal, codesz, new BitSet(codesz + 1), new BitSet(codesz + 1), -1);
    }

    private CodeInfo(Buffer buffer, int maxlocal, int codesz, BitSet poslabels, BitSet actlabels, int stackmapOffset) {
        this.buffer = buffer;
        this.maxlocal = maxlocal;
        this.codesz = codesz;
        this.poslabels = poslabels;
        this.actlabels = actlabels;
        this.stackmapOffset = stackmapOffset;
    }
    
    public CodeInfo setBuffer(Buffer buffer) {
       this.buffer = buffer;
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
        return labelOffset(instoff, buffer.nextInt());
    }
    
    public int nextIfLabel(int instoff) {
        return labelOffset(instoff, buffer.nextShort());
    }
    
    public int nextLabel() {
        return labelOffset(0, buffer.nextUnsignedShort());
    }
    
    public int nextEndOffset(int pc) {
        return labelOffset(pc, buffer.nextUnsignedShort());
    }
    
    private void checkLocalVar(int var) {
        if (var >= maxlocal) {
            // "local variable %d is >= max locals %d"
            LOG(M518, var, maxlocal);
        }
    }
    
    public int nextVar() {
        int lvindex = buffer.nextUnsignedShort();
        checkLocalVar(lvindex);
        return lvindex;
    }
    
    public void setPosLabel(int instoff) {
        assert instoff < codesz;
        poslabels.set(instoff);
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

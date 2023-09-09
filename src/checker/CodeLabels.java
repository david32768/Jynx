package checker;

import java.util.BitSet;
import static jynx.Global.LOG;
import static jynx.Message.M501;
import static jynx.Message.M504;
import static jynx.Message.M524;

public class CodeLabels {
    
    private final BitSet poslabels;
    private final BitSet actlabels;
    private final int codesz;

    public CodeLabels(int codesz) {
        this.codesz = codesz;
        this.poslabels = new BitSet(codesz + 1);
        this.actlabels = new BitSet(codesz + 1);
    }
    
    public int labelOffset(int instoff, int broff) {
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
        
    public void checkLabels() {
        BitSet badlabels = (BitSet)actlabels.clone();
        badlabels.andNot(poslabels);
        if (!badlabels.isEmpty()) {
            // "branches to middle of instruction - %s"
            LOG(M524,badlabels.toString());
        }
        poslabels.set(codesz);
    }

    public void setPosLabel(int instoff) {
        assert instoff < codesz;
        poslabels.set(instoff);
    }
   
}

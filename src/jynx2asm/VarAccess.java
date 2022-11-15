package jynx2asm;

import java.util.BitSet;

import static jynx.Global.LOG;
import static jynx.Message.M223;
import static jynx.Message.M281;
import static jynx.Message.M307;
import static jynx.Message.M56;
import static jynx.Message.M60;
import static jynx.Message.M65;

public class VarAccess {
    
    private final BitSet readVars;
    private final BitSet writeVars;
    private final BitSet typedVars;
    private final BitSet frameVars;
    private final BitSet parmVars;

    private int parmsz;
    
    public VarAccess() {
        this.readVars = new BitSet();
        this.writeVars = new BitSet();
        this.typedVars = new BitSet();
        this.frameVars = new BitSet();
        this.parmVars = new BitSet();
    }

    public void completeInit(int parmsz) {
        assert writeVars.isEmpty();
        this.parmsz = parmsz;
        writeVars.or(frameVars);
    }
    
    private void set(BitSet bs, int num, FrameElement fe) {
        bs.set(num);
        if (fe.isTwo()) {
            bs.set(num + 1);
        }
    }
    
    public void setRead(int num, FrameElement fe) {
        set(readVars,num,fe);
    }

    public void setWrite(int num, FrameElement fe) {
        set(writeVars,num,fe);
        if (num < parmsz) {
            set(parmVars,num,fe);
        }
    }

    public void setFrame(int num, FrameElement fe) {
        if (fe == FrameElement.TOP) {
            return;
        }
        set(frameVars,num,fe);
    }

    public void setTyped(int num) {
        typedVars.set(num);
    }

    public boolean checkWritten(FrameElement fe, int num) {
        boolean ok = writeVars.get(num);
        if (fe.isTwo()) {
            ok &= writeVars.get(num + 1);
        }
        return ok;
    }
    
    private final static int MAX_GAP = 0;
    
    private String rangeString(int start,BitSet bitset) {
        StringBuilder sb = new StringBuilder();
        int last = -2;
        char spacer = ' ';
        for (int i = bitset.nextSetBit(start); i >= 0; i = bitset.nextSetBit(i+1)) {
            if (i == last + 1) {
                spacer = '-';
            } else {
                if (last >= 0 && spacer != ' ') {
                    sb.append(spacer).append(last);
                }
                spacer = ' ';
                sb.append(' ').append(i);
            }
            last = i;
            if (i == Integer.MAX_VALUE) {
                break; // or (i+1) would overflow
            }
        }
        if (spacer != ' ') {
            sb.append(spacer).append(last);
        }
        return sb.toString();
    }

    private String haveBeenWritten(BitSet x) {
        BitSet andnot = (BitSet)x.clone();
        andnot.andNot(writeVars);
        if (andnot.isEmpty()) {
            return "";
        } else {
            return rangeString(0,andnot);
        }
    }
    
    public void visitEnd() {
        int last = 0;
        for (int i = writeVars.nextSetBit(0); i >= 0; i = writeVars.nextSetBit(i+1)) {
            int gap = i - last - 1;
            if (gap > MAX_GAP) {
                LOG(M56, gap,last,i); // "gap %d between local variables: %d - %d"
            }
            last = i;
            if (i == Integer.MAX_VALUE) {
                break; // or (i+1) would overflow
            }
        }
        BitSet unreadvars = (BitSet)writeVars.clone();
        unreadvars.andNot(readVars);
        if (unreadvars.nextSetBit(parmsz) >= 0) {
            String ranges = rangeString(parmsz,unreadvars);
             // "local variables [%s ] are written but not read"
            LOG(M60,ranges);
        }
        String ranges = haveBeenWritten(readVars);
        if (!ranges.isEmpty()) {
            // "local variables [%s ] are read but not written"
            LOG(M65,ranges);
        }
        ranges = haveBeenWritten(typedVars);
        if (!ranges.isEmpty()) {
            // "Annotation for unknown variables [%s ]"
            LOG(M223,ranges);
        }
        ranges = haveBeenWritten(frameVars);
        if (!ranges.isEmpty()) {
            // "local variables [%s ] are in a frame but not written"
            LOG(M281,ranges);
        }
//        ranges = rangeString(0,parmVars);
//        if (!ranges.isEmpty()) {
//            // "parameters [%s ] are overwritten"
//            LOG(M307,ranges);
//        }
    }
    
}

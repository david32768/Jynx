package asm.instruction;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;

import static jvm.Constants.MAX_CODE;
import static jynx.Global.LOG;
import static jynx.Message.M224;
import static jynx.Message.M244;
import static jynx.Message.M256;
import static jynx.Message.M323;
import static jynx.Message.M340;
import static jynx.ReservedWord.res_default;
import static jynx2asm.ops.JvmOp.asm_tableswitch;
import static jynx2asm.ops.JvmOp.opc_switch;

import jynx.LogIllegalArgumentException;
import jynx2asm.JynxLabel;
import jynx2asm.ops.JvmOp;

public abstract class SwitchInstruction extends Instruction {

    private final int unpaddedLength;

    private int minPadding;
    private int maxPadding;
    
    protected SwitchInstruction(JvmOp jop, long unpaddedlength) {
        super(jop);
        if (unpaddedlength < 0 || unpaddedlength > UNPADDED_MAX) {
            // "size of %s is %d which exceeds %d"
            throw new LogIllegalArgumentException(M256,JvmOp.asm_tableswitch,unpaddedlength, UNPADDED_MAX);
        }
        this.unpaddedLength = (int)unpaddedlength; // definitely int after length test
        this.minPadding = 0;
        this.maxPadding = 3;
    }
    
    private static final int UNPADDED_MAX = MAX_CODE - 3 - 3; // 3 at start to align and 3 at end
    
    private int paddingForOffset(int offset) {
        return 3 - (offset % 4);
    }
    
    @Override
    public JvmOp resolve(int minoffset, int maxoffset) {
        minPadding = paddingForOffset(minoffset);
        maxPadding = minoffset == maxoffset?minPadding:3;
        return jvmop;
    }

    @Override
    public Integer minLength() {
        return minPadding + unpaddedLength;
    }

    @Override
    public Integer maxLength() {
        return maxPadding + unpaddedLength;
    }

    public static Instruction getInstance(JvmOp jvmop, JynxLabel dflt, SortedMap<Integer,JynxLabel> swmap) {
        if (swmap.isEmpty()) {
            if (jvmop == asm_tableswitch) {
                // "invalid %s as only has %s: case 0 -> %s added"
                LOG(M224, jvmop, res_default, dflt.name());
                swmap.put(0, dflt);
            } else {
                return new LookupInstruction(dflt,swmap);        
            }
        }
        
        int min = swmap.firstKey();
        int max = swmap.lastKey();
        long range = 1L + max - min;
        long tablesz = TableInstruction.minsize(range);
        if (jvmop == JvmOp.asm_tableswitch && tablesz > UNPADDED_MAX) {
            // "range of cases [%d, %d] is too big for %s, so %s substituted"
            LOG(M340, min, max, asm_tableswitch, JvmOp.asm_lookupswitch);
            jvmop = JvmOp.asm_lookupswitch;
        }
        
        if (jvmop == JvmOp.asm_tableswitch) {
            return lookupToTableSwitch(min, max, dflt, swmap);
        }
        
        long lookupsz = LookupInstruction.minsize(swmap.size());
        boolean consec = range == swmap.size();
        boolean tablesmaller = tablesz < lookupsz;
        if (jvmop == opc_switch && tablesmaller) {
            return lookupToTableSwitch(min, max, dflt, swmap);
        }
        
        // use lookupswitch
        if (jvmop == JvmOp.asm_lookupswitch) {
            if (consec && swmap.size() > 1) {
                // "%s could be used as entries are consecutive"
                LOG(M244,JvmOp.asm_tableswitch);
            } else if (tablesmaller) {
                // "by adding dflt entries %s (size %d) would still be smaller than %s (size %d); range = %d labels = %d"
                LOG(M323, JvmOp.asm_tableswitch, tablesz, JvmOp.asm_lookupswitch, lookupsz, range, swmap.size());
            }
        }
        return new LookupInstruction(dflt,swmap);
    }
    
    private static Instruction lookupToTableSwitch(int min, int max, JynxLabel dflt,SortedMap<Integer,JynxLabel> swmap) {
        assert max >= min;
        List<JynxLabel> labellist = new ArrayList<>();
        for (int i = min; i <= max; ++i) {
            JynxLabel label = swmap.getOrDefault(i, dflt);
            labellist.add(label);
        }
        return new TableInstruction(min, max, dflt, labellist);
    }
    
}

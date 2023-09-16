package asm.instruction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import static jvm.Constants.MAX_CODE;
import static jynx.Global.LOG;
import static jynx.Message.M224;
import static jynx.Message.M244;
import static jynx.Message.M256;
import static jynx.Message.M323;
import static jynx.Message.M331;
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
                LOG(M224,jvmop,res_default); // "invalid %s as only has %s"
            }
            return new LookupInstruction(dflt,swmap);        
        }
        long min = swmap.firstKey().longValue();
        long max = swmap.lastKey().longValue();
        long range = max - min + 1;
        if (jvmop == JvmOp.asm_tableswitch) {
            if (range == swmap.size()) {
                return lookupToTableSwitch(min, max, dflt, swmap);
            }
            // "%s changed to %s as entries are not consecutive"
            LOG(M331, asm_tableswitch,opc_switch);
            jvmop = opc_switch;
        }
        long lookupsz = LookupInstruction.minsize(swmap.size());
        long tablesz = TableInstruction.minsize(range);
        boolean consec = range == swmap.size();
        boolean tablesmaller = tablesz < lookupsz;
        if (jvmop == opc_switch && tablesmaller) {
            return lookupToTableSwitch(min, max, dflt, swmap);
        }
        if (consec && swmap.size() > 1) {
            // "%s could be used as entries are consecutive"
            LOG(M244,JvmOp.asm_tableswitch);
        } else if (tablesmaller) {
            // "by adding dflt entries %s (size %d) would still be smaller than %s (size %d); range = %d labels = %d"
            LOG(M323, JvmOp.asm_tableswitch, tablesz, JvmOp.asm_lookupswitch, lookupsz, range, swmap.size());
        }
        return new LookupInstruction(dflt,swmap);
    }
    
    private static Instruction lookupToTableSwitch(long min, long max, JynxLabel dflt,SortedMap<Integer,JynxLabel> swmap) {
        List<JynxLabel> labellist = new ArrayList<>();
        long lastkey = min - 1;
        for (Map.Entry<Integer,JynxLabel> me : swmap.entrySet()) {
            int key = me.getKey();
            for (long i = lastkey + 1; i < key; ++i) {
                labellist.add(dflt); // fill in gaps with dflt label
            }
            JynxLabel label = me.getValue();
            labellist.add(label);
            lastkey = key;
        }
        return new TableInstruction((int)min, (int)max, dflt, labellist);
    }
    
}

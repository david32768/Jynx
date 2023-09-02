package asm.instruction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import static jvm.Constants.MAX_CODE;
import static jynx.Global.LOG;
import static jynx.Message.M244;
import static jynx.Message.M256;
import static jynx.Message.M323;
import static jynx.Message.M331;
import static jynx2asm.ops.JvmOp.asm_tableswitch;
import static jynx2asm.ops.JvmOp.opc_switch;

import jynx.LogIllegalArgumentException;
import jynx2asm.JynxLabel;
import jynx2asm.ops.JvmOp;

public abstract class SwitchInstruction extends Instruction {

    private final int unpaddedLength;

    private int minPadding;
    private int maxPadding;
    
    public SwitchInstruction(JvmOp jop, long unpaddedlength) {
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
            return new LookupInstruction(jvmop,dflt,swmap);        
        }
        Integer min = swmap.firstKey();
        Integer max = swmap.lastKey();
        long range = max.longValue() - min.longValue() + 1;
        if (jvmop == JvmOp.asm_tableswitch) {
            if (range == swmap.size()) {
                return lookupToTableSwitch(min, max, dflt, swmap);
            }
            // "%s without low value changed to %s as entries are not consecutive"
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
        return new LookupInstruction(jvmop,dflt,swmap);
    }
    
    private static Instruction lookupToTableSwitch(int min, int max, JynxLabel dflt,SortedMap<Integer,JynxLabel> swmap) {
        JvmOp jvmop = JvmOp.asm_tableswitch;
        List<JynxLabel> labellist = new ArrayList<>();
        int lastkey = min - 1;
        for (Map.Entry<Integer,JynxLabel> me : swmap.entrySet()) {
            int key = me.getKey();
            for (int i = lastkey + 1; i < key; ++i) {
                labellist.add(dflt); // fill in gaps with dflt label
            }
            JynxLabel label = me.getValue();
            labellist.add(label);
            lastkey = key;
        }
        return new TableInstruction(jvmop, min, max, dflt, labellist);
    }
    
}

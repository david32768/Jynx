package asm2jynx;

import java.util.function.Function;
import java.util.List;

import org.objectweb.asm.tree.LabelNode;

import static jynx.Message.*;

import jvm.FrameType;
import jynx.LogIllegalArgumentException;

public class FrameTypeValue {

    private final FrameType ft;
    private final String value;

    public FrameTypeValue(FrameType ft, String value) {
        this.ft = ft;
        this.value = value;
    }
 
    public FrameType ft() {
        return ft;
    }
    
    public String value() {
        return value;
    }

    private static FrameTypeValue from(Object obj, Function<LabelNode,String> labnamefn) {
        if (obj instanceof Integer) {
            int type = (Integer)obj;
            return new FrameTypeValue(FrameType.fromAsmType(type),null);
        } else if (obj instanceof String) {
            return new FrameTypeValue(FrameType.ft_Object,(String)obj);
        } else if (obj instanceof LabelNode) {
            return new FrameTypeValue(FrameType.ft_Uninitialized,labnamefn.apply((LabelNode) obj));
        } else {
            // "invalid stack frame type - %s"
            throw new LogIllegalArgumentException(M37,obj.getClass().getName());
        }
    }

    public static FrameTypeValue[] fromList(List<Object> objs, Function<LabelNode,String> labnamefn) {
        FrameTypeValue[] stackarr = new FrameTypeValue[objs.size()];
        int i = 0;
        for (Object obj:objs) {
            stackarr[i] = FrameTypeValue.from(obj, labnamefn);
            ++i;
        }
        return stackarr;
    }
    
    @Override
    public String toString() {
        return String.format("%s %s",ft,value);
    }
    
}

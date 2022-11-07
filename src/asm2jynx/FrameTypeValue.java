package asm2jynx;

import java.util.function.Function;
import java.util.stream.Stream;

import org.objectweb.asm.Label;
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
        } else if (obj instanceof Label) {
            LabelNode labnode = new LabelNode((Label)obj);
            return new FrameTypeValue(FrameType.ft_Uninitialized,labnamefn.apply(labnode));
        } else {
            String classname = obj == null?"NULL":obj.getClass().getName();
            // "invalid stack frame type - %s"
            throw new LogIllegalArgumentException(M37,classname);
        }
    }

    public static FrameTypeValue[] from(Stream<Object> objstream, Function<LabelNode,String> labnamefn) {
        return objstream
                .map(obj -> from(obj, labnamefn))
                .toArray(FrameTypeValue[]::new);
    }
    
    @Override
    public String toString() {
        return String.format("%s %s",ft,value);
    }
    
}

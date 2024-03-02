package jvm;

import java.util.ArrayList;
import java.util.stream.Stream;

import static org.objectweb.asm.Opcodes.*;

import org.objectweb.asm.Label;
import org.objectweb.asm.Type;

import static jynx.Global.LOG;
import static jynx.Message.*;

import jynx.LogAssertionError;
import jynx2asm.handles.LocalMethodHandle;

public enum FrameType {

    // jvms 4.7.4
    // null means that extra parameter required
    ft_Top(0, TOP),  // top means 2nd slot for Long or Double; NOTE ASM does not include this after LONG and DOUBLE
    ft_Integer(1, INTEGER),
    ft_Float(2, FLOAT),
    ft_Double(3, DOUBLE),
    ft_Long(4, LONG),
    ft_Null(5, NULL),
    ft_UninitializedThis(6, UNINITIALIZED_THIS),
    ft_Object(7, null),
    ft_Uninitialized(8, null),
    ;

    private final int tag;
    private final Integer asmType;

    private FrameType(int tag, Integer asmType) {
        this.tag = tag;
        this.asmType = asmType;
        // "%s: asm value (%d) does not agree with jvm value(%d)"
        assert asmType == null || tag == asmType:M161.format(name(),asmType,tag);
    }

    public int tag() {
        return tag;
    }

    public boolean extra() {
        return asmType == null;
    }

    public Integer asmType() {
        return asmType;
    }

    public String externalName() {
        return name().substring(3);
    }
    
    @Override
    public String toString() {
        return externalName();
    }
    
    public static FrameType fromString(String token) {
        FrameType result = Stream.of(values())
                .filter(ft -> ft.externalName().equals(token))
                .findFirst()
                .orElse(ft_Top);
        if (result == ft_Top && !result.externalName().equals(token)) {
            LOG(M61,token,result);  // "invalid stack frame type(%s) - %s assumed"
        }
        return result;
    }
    
    public static FrameType fromAsmType(int type) {
        FrameType result =  Stream.of(values())
                .filter(ft -> ft.tag == type)
                .findFirst()
                .orElse(ft_Top);
        if (result.asmType == null || result == ft_Top && type != 0) {
            throw new LogAssertionError(M902,type); // "unknown ASM stack frame type (%d)"
        }
        return result;
    }
    
    public static FrameType fromJVMType(int type) {
        return Stream.of(values())
                .filter(ft -> ft.tag == type)
                .findFirst()
                .orElseThrow(() -> new LogAssertionError(M904,type)); // "unknown JVM stack frame type (%d)"
    }
    
    private static Object objectFrom(String tdesc) {
        switch(tdesc.charAt(0)) {
            case 'Z':
            case 'B':
            case 'S':
            case 'C':
            case 'I':
                return FrameType.ft_Integer.asmType();
            case 'J':
                return FrameType.ft_Long.asmType();
            case 'F':
                return FrameType.ft_Float.asmType();
            case 'D':
                return FrameType.ft_Double.asmType();
            case 'L':
                tdesc = tdesc.substring(1,tdesc.length() - 1);
                return tdesc;
            case '[':
                return tdesc;    
            default:
                throw new LogAssertionError(M901,tdesc,tdesc.charAt(0)); // "unknown ASM type %s as it starts with '%c'"
        }
    }

    public static FrameType fromObject(Object obj) {
        if (obj instanceof Integer) {
            return fromAsmType((int)obj);
        }
        if (obj instanceof String) {
            return ft_Object;
        }
        if (obj instanceof Label) {
            return ft_Uninitialized;
        }
        throw new LogAssertionError(M903,obj.getClass()); // "unknown class %s for ASM frametype"
    }
    
    // set classname == null for static method
    public static ArrayList<Object> getInitFrame(String classname, boolean isstatic, LocalMethodHandle lmh) {
        ArrayList<Object> localStack = new ArrayList<>();
        if (!isstatic) {
            if (lmh.isInit()) {
                localStack.add(ft_UninitializedThis.asmType());
            } else {
                localStack.add(classname);
            }
        }
        Type[] parmtypes = Type.getArgumentTypes(lmh.desc());
        for (Type type:parmtypes) {
            String tdesc = type.getDescriptor();
            localStack.add(objectFrom(tdesc));
        }
        return localStack;
    }
    
}

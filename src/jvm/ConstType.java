package jvm;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.EnumSet;
import java.util.function.Function;
import java.util.Optional;

import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.Type;

import static jvm.Context.*;
import static jynx.Message.M141;
import static jynx.Message.M146;
import static jynx.Message.M175;
import static jynx.Message.M183;

import jynx.LogIllegalArgumentException;
import jynx2asm.Token;

public enum ConstType {

    ct_boolean('Z', Boolean.class,EnumSet.of(FIELD,ANNOTATION),Token::asBoolean),
    ct_byte('B', Byte.class,EnumSet.of(FIELD,ANNOTATION),Token::asByte),
    ct_char('C', Character.class,EnumSet.of(FIELD,ANNOTATION),Token::asChar),
    ct_short('S', Short.class,EnumSet.of(FIELD,ANNOTATION),Token::asShort),
    
    ct_int('I', Integer.class,EnumSet.of(JVMCONSTANT,FIELD,ANNOTATION),Token::asInt),
    ct_long('J', Long.class,EnumSet.of(JVMCONSTANT,FIELD,ANNOTATION),Token::asLong),
    ct_float('F', Float.class,EnumSet.of(JVMCONSTANT,FIELD,ANNOTATION),Token::asFloat),
    ct_double('D', Double.class,EnumSet.of(JVMCONSTANT,FIELD,ANNOTATION),Token::asDouble),
    
    ct_string('s', String.class,EnumSet.of(JVMCONSTANT,FIELD,ANNOTATION),Token::asQuoted),

    ct_class('c', Class.class,EnumSet.of(JVMCONSTANT,ANNOTATION),Token::asType, Type.class),
    ct_method_handle('h', MethodHandle.class,EnumSet.of(JVMCONSTANT),Token::asHandle, Handle.class),
    ct_method_type('t',MethodType.class,EnumSet.of(JVMCONSTANT),Token::asMethodType, Type.class),
    ct_const_dynamic('k',ConstantDynamic.class,EnumSet.of(JVMCONSTANT),null),
    ct_enum('e',(new String[0]).getClass(),EnumSet.of(JVMCONSTANT,ANNOTATION),Token::asQuoted),
    ct_annotation('@',AnnotationNode.class,EnumSet.of(JVMCONSTANT,ANNOTATION),null),

    ct_object('o',Object.class,EnumSet.of(JVMCONSTANT),Token::getConst),    // must be last
    ;

    private final Class<?> klass;
    private final String desc;
    private final char jynx_desc;
    private final Class<?> ASMklass;
    private final EnumSet<Context> contexts;
    private final Function<Token,Object> tokenfn;
    
    private ConstType(char jynx_desc, Class<?> klass,EnumSet<Context> contexts,
            Function<Token,Object> tokenfn, Class<?> ASMklass) {
        this.klass = klass;
        this.contexts = contexts;
        if (Character.isUpperCase(jynx_desc)) {
            this.desc = String.valueOf(jynx_desc);
        } else {
            this.desc = "L" + klass.getName().replace('.','/') + ";";
        }
        this.jynx_desc = jynx_desc;
        this.ASMklass = ASMklass;
        this.tokenfn = tokenfn;
    }

    private ConstType(char jynx_desc, Class<?> klass,EnumSet<Context> contexts,
            Function<Token,Object> tokenfn) {
        this(jynx_desc, klass, contexts,tokenfn,klass);
    }

    public String getJynxDesc(boolean isArray) {
        String arrayind = isArray?"[":"";
        return arrayind + jynx_desc;
    }

    public String getDesc() {
        return desc;
    }

    public Class<?> getASMklass() {
        return ASMklass;
    }

    public boolean isPrimitive() {
        return Character.isUpperCase(jynx_desc);
    }
    
    public Object getValue(Token token) {
        return tokenfn.apply(token);
    }

    public boolean inContext(Context context) {
        return contexts.contains(context);
    }

    public static int getLDCsz(Object cst) {
        ConstType ct = getFromASM(cst,Context.JVMCONSTANT);
        int sz = 1;
        switch (ct) {
            case ct_double:
            case ct_long:
                sz = 2;
                break;
            case ct_const_dynamic:
                ConstantDynamic dyn = (ConstantDynamic) cst;
                sz = dyn.getSize();
                break;
        }
        return sz;
    }
    
    @Override
    public String toString() {
        return name().substring(3);
    }

    
    private static final EnumSet<ConstType> all = EnumSet.allOf(ConstType.class);
    private static final EnumSet<ConstType> subint  = EnumSet.of(ct_boolean,ct_byte,ct_char,ct_short);

    public Object toJvmValue(Object value) {
        if (subint.contains(this)) {
            if (value.equals(Boolean.TRUE)) {
                return 1;
            } else if (value.equals(Boolean.FALSE)) {
                return 0;
            } else if (value instanceof Character) {
                Character ch = (Character)value;
                return (int)ch;
            } else {
                return ((Number)value).intValue();
            }
        }
        return value;
    }

    private static ConstType getInstanceFromAsm(Object obj) {
        if (obj instanceof Type) {
            int sort = ((Type)obj).getSort();
            return sort == Type.METHOD?ct_method_type:ct_class;
        }
        return all.stream()
                .filter(ct ->ct.ASMklass.isInstance(obj))
                .findFirst()
                // "unknown constant; class = %s"
                .orElseThrow(()->new LogIllegalArgumentException(M141,obj.getClass().getName()));
    }
    
    private ConstType checkedContext(Context context) {
        if (!inContext(context)) {
            // "constant type = %s not valid in this context"
            throw new LogIllegalArgumentException(M146,this);
        }
        return this;
    }

    private static Optional<ConstType> getInstance(char c) {
        return all.stream()
                .filter(ct->ct.jynx_desc == c)
                .findFirst();
    }

    public static boolean isPrimitiveType(String desc) {
        if (desc.length() != 1) {
            return false;
        }
        Optional<ConstType> optct = getInstance(desc.charAt(0));
        if (optct.isPresent()) {
            return optct.get().isPrimitive();
        }
        return false;
    }
    
    public static ConstType getInstance(char c,Context context) {
        ConstType ct = getInstance(c)
                .orElseThrow(()->new LogIllegalArgumentException(M175,c)); // "unknown Jynx desc = %c"
        ct.checkedContext(context);
        return ct;
    }
    
    public static ConstType getFromDesc(String str,Context context) {
        Optional<ConstType> ctopt = all.stream()
                .filter(ct->ct.desc.equals(str))
                .findFirst();

        ConstType ct;
        if (!ctopt.isPresent() && context == Context.ANNOTATION) {
            ct = ct_annotation;
        } else {
            ct = ctopt
                .orElseThrow(()->new LogIllegalArgumentException(M183,str)); // "Type is not known - %s"
        }
        return ct.checkedContext(context);
    }
    
    public static ConstType getFromType(Type type,Context context) {
        String str = type.getDescriptor();
        return getFromDesc(str,context);
    }
    
    public static ConstType getFromASM(Object value, Context context) {
        ConstType ct = getInstanceFromAsm(value);
        return ct.checkedContext(context);
    }

}

package jynx2asm;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.lang.invoke.VolatileCallSite;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;

import static jvm.AttributeName.BootstrapMethods;
import static jynx.Global.*;
import static jynx.Message.M257;
import static jynx.Message.M259;
import static jynx.Message.M260;
import static jynx.Message.M50;
import static jynx.Message.M98;
import static jynx.ReservedWord.left_brace;
import static jynx.ReservedWord.right_array;
import static jynx.ReservedWord.right_brace;

import jvm.ConstantPoolType;
import jvm.ConstType;
import jvm.Context;
import jvm.Feature;
import jvm.HandleType;
import jynx.Global;
import jynx.LogIllegalArgumentException;
import jynx2asm.ops.JvmOp;

public class JynxConstantDynamic {
    
    private final static String2Object S2O = new String2Object();

    private final JynxScanner js;
    private final Line line;
    private final ClassChecker checker;

    public JynxConstantDynamic(JynxScanner js, Line line, ClassChecker checker) {
        this.js = js;
        this.line = line;
        this.checker = checker;
        Global.CHECK_SUPPORTS(BootstrapMethods);
    }

    private ConstantDynamic getConstantDynamic(ConstType ct) {
        ConstantDynamic cdv = getConstantDynamic4Load();
        String desc = cdv.getDescriptor();
        ConstType actual = ConstType.getFromDesc(desc, Context.JVMCONSTANT);
        if (ct != ConstType.ct_object && ct != actual) {
            LOG(M259,actual,ct); // "dynamic constant is %s but %s expected"
        }
        return cdv;
    }

    private Object getValue(Token token, ConstType ct) {
        Object value;
        if (token.is(left_brace)) {
            value = getConstantDynamic(ct);
        } else {
            line.nextToken(); // reread token
            value = token.getValue(ct);
            checker.mayBeHandle(value, line);
        }
        return value;
    }
    
    private Object[] getBootArgs(Type[] types) {
        List<Object> bootargs = new ArrayList<>();
        Type arraytype = null;
        for (Type type:types) {
            if (arraytype != null) {
                // "Array type must be last - type %s found after array type %s"
                LOG(M50,type,arraytype);
                return bootargs.toArray(new Object[0]);
            }
            if (type.getSort() == Type.ARRAY) {
                arraytype = type;
                ConstType ct = ConstType.getFromType(type.getElementType(),Context.JVMCONSTANT);
                while (true) {
                    Token token = line.peekToken();
                    if (token.is(right_brace)) {
                        break;
                    }
                    Object value = getValue(token, ct);
                    bootargs.add(value);
                }
            } else {
                ConstType ct = ConstType.getFromType(type,Context.JVMCONSTANT);
                Token token = line.peekToken();
                Object value = getValue(token, ct);
                bootargs.add(value);
            }
        }
        line.nextToken().mustBe(right_brace);
        int maxct = 251;
        if (bootargs.size() > maxct) {
            // "argument count %d is not in range [0,%d]"
            throw new LogIllegalArgumentException(M257,bootargs.size(), maxct);
        }
        return bootargs.toArray(new Object[0]);
    }

    private static final Type OBJECT_TYPE = Type.getType(Object.class);
    private static final Type OBJECT_ARRAY_TYPE = Type.getType(Object[].class);
    private static final Type STRING_TYPE = Type.getType(String.class);
    private static final Type LOOKUP_TYPE = Type.getType(MethodHandles.Lookup.class);
    private static final Type MT_TYPE = Type.getType(MethodType.class);
    private static final Type TD_TYPE = Type.getObjectType("java/lang/invoke/TypeDescriptor"); // java V12 class
    private static final Type CLASS_TYPE = Type.getType(Class.class);
    private static final Type CALLSITE_TYPE = Type.getType(CallSite.class);
    private static final Type CONSTANT_CALLSITE_TYPE = Type.getType(ConstantCallSite.class);
    private static final Type MUTABLE_CALLSITE_TYPE = Type.getType(MutableCallSite.class);
    private static final Type VOLATILE_CALLSITE_TYPE = Type.getType(VolatileCallSite.class);

    private Type[] checkBootParm3(String bsmdesc, List<Type> valid2) {
        Type[] types = Type.getArgumentTypes(bsmdesc);
        Type[] result = null;
        switch (types.length) {
            case 0:
                break;
            case 1:
                Type type0 = types[0];
                if (type0.equals(OBJECT_ARRAY_TYPE)) {
                    result = types;
                } else if (type0.equals(OBJECT_TYPE)) {
                    result = new Type[]{OBJECT_ARRAY_TYPE};
                }
                break;
            case 2:
                type0 = types[0];
                Type type1 = types[1];
                if (type1.equals(OBJECT_ARRAY_TYPE) && (type0.equals(OBJECT_TYPE) || type0.equals(LOOKUP_TYPE))) {
                    result = Arrays.copyOfRange(types, 1, types.length);
                }
                break;
            default:
                type0 = types[0];
                type1 = types[1];
                Type type2 = types[2];
                if ((type0.equals(OBJECT_TYPE) || type0.equals(LOOKUP_TYPE))  
                        && (type1.equals(OBJECT_TYPE) || type1.equals(STRING_TYPE))) {
                    if (type2.getSort() == Type.ARRAY) {
                        if (types.length == 2 && valid2.contains(type2.getElementType())) {
                            result = Arrays.copyOfRange(types, 2, types.length);
                        }
                    } else if (valid2.contains(type2)) {
                        result = Arrays.copyOfRange(types, 3, types.length);
                    }
                }
                break;
        }
        return result;
    }
                            
    private static final String INVOKE3 = LOOKUP_TYPE.getDescriptor() + STRING_TYPE.getDescriptor() + MT_TYPE.getDescriptor();

    private Type[] getTypes4Invoke(Handle bsm) {
        List<Type> valid2 = new ArrayList<>();
        valid2.add(OBJECT_TYPE);
        valid2.add(MT_TYPE);
        if (Global.SUPPORTS(Feature.typedesc)) {
            valid2.add(TD_TYPE);
        }
        String bsmspec = bsm.getDesc();
        Type[] result = checkBootParm3(bsmspec,valid2);
        Type rtype = Type.getReturnType(bsm.getDesc());
        if (!rtype.equals(CALLSITE_TYPE)
                && !rtype.equals(CONSTANT_CALLSITE_TYPE)
                && !rtype.equals(MUTABLE_CALLSITE_TYPE)
                && !rtype.equals(VOLATILE_CALLSITE_TYPE)
                && !rtype.equals(OBJECT_TYPE)) {
            // "return type (%s) of invoke bootstrap method is not ( a known subtype of ) CallSite or Object"
            LOG(M260,rtype);
        }
        if (result != null) {
            return result;
        }
        // "Unsupported version of %s: first parms are not compatible with:%n   %s"
        LOG(M98, JvmOp.asm_invokedynamic,INVOKE3);
        return new Type[0];
    }
    
    private final static String LDC3 = LOOKUP_TYPE.getDescriptor() + STRING_TYPE.getDescriptor() + CLASS_TYPE.getDescriptor();

    private Type[] getAndCheckTypes4Load(Handle bsm) {
        HandleType ht = HandleType.of(bsm);
        if (ht.isField()) {
            return new Type[0];
        }
        List<Type> valid2 = new ArrayList<>();
        valid2.add(OBJECT_TYPE);
        valid2.add(CLASS_TYPE);
        String bsmdesc = bsm.getDesc();
        Type[] result = checkBootParm3(bsmdesc, valid2);
        if (result != null) {
            return result;
        }
        // "Unsupported version of %s: first parms are not compatible with:%n   %s"
        LOG(M98, JvmOp.asm_ldc,LDC3);
        return new Type[0];
    }
    
    private Object[] getSimpleBootArgs(String bootdescplus,String[] bootparms){
        List<Object> arglist = new ArrayList<>();
        int bootparmct = 0;
        if (!bootdescplus.isEmpty()) {
            Type arraytype = null;
            Type[] types = Type.getArgumentTypes("(" + bootdescplus + ")V");
            for (Type type:types) {
                if (arraytype != null) {
                    // "Array type must be last - type %s found after array type %s"
                    throw new LogIllegalArgumentException(M50,type,arraytype);
                }
                if (type.getSort() == Type.ARRAY) {
                    if (bootparmct < bootparms.length) {
                        throw new AssertionError();
                    }
                    arraytype = type;
                    ConstType ct = ConstType.getFromType(type.getElementType(),Context.JVMCONSTANT);
                    TokenArray tokenarr = TokenArray.getInstance(js, line);
                    while (true) {
                        Token token = tokenarr.firstToken();
                        if (token.is(right_array)) {
                            break;
                        }
                        Object value = token.getValue(ct);
                        checker.mayBeHandle(value, line);
                        arglist.add(value);
                    }
                } else {
                    ConstType ct = ConstType.getFromType(type,Context.JVMCONSTANT);
                    Token token;
                    if (bootparmct < bootparms.length) {
                        token = Token.getInstance(bootparms[bootparmct]);
                        ++bootparmct;
                    } else {
                        token = line.nextToken();
                    }
                    Object value = token.getValue(ct);
                    checker.mayBeHandle(value, line);
                    arglist.add(value);
                }
            }
            int maxct = 251;
            if (arglist.size() > maxct) {
                // "argument count %d is not in range [0,%d]"
                throw new LogIllegalArgumentException(M257,arglist.size(), maxct);
            }
        }
        return arglist.toArray(new Object[0]);
    }
    
    public ConstantDynamic getConstantDynamic4Load() {
        Global.CHECK_CAN_LOAD(ConstantPoolType.CONSTANT_Dynamic);
        line.nextToken().mustBe(left_brace);
        String name;
        String desc;
        name = line.nextToken().asString();
        desc = line.nextToken().asString();
        NameDesc.FIELD_NAME.validate(name);
        NameDesc.FIELD_DESC.validate(desc);
        String bootname = line.nextToken().asString();
        Handle bsm = S2O.parseHandle(bootname);
        Type[] types = getAndCheckTypes4Load(bsm);
        Object[] bsmArgs = getBootArgs(types);
        return new ConstantDynamic(name, desc, bsm, bsmArgs);
    }

    public ConstantDynamic getConstantDynamic4Invoke() {
        line.nextToken().mustBe(left_brace);
        String name;
        String desc;
        String namedesc = line.nextToken().asString();
        int index = namedesc.indexOf('(');
        if (index >= 0) {
            name = namedesc.substring(0,index);
            desc = namedesc.substring(index);

        } else {
            name = namedesc;
            desc = line.nextToken().asString();
        }
        desc = Global.TRANSLATE_DESC(desc);
        NameDesc.METHOD_ID.validate(name); // not <init> or <clinit>
        NameDesc.DESC.validate(desc);
        String bootname = line.nextToken().asString();
        Handle bsm = S2O.parseHandle(bootname);
        checker.mayBeHandle(bsm, line);
        Type[] types = getTypes4Invoke(bsm);
        Object[] bsmArgs = getBootArgs(types);
        return new ConstantDynamic(name, desc, bsm, bsmArgs);
    }

    private static final String ARRAY_BOOT_DESC_FORMAT = "(" + INVOKE3 + "%s)Ljava/lang/invoke/CallSite;";

    public ConstantDynamic getSimple(String name, String desc, String bootmethod,
            String bootdescplus,String... bootparms) {
        Object[] bootargs = getSimpleBootArgs(bootdescplus,bootparms);
        NameDesc.METHOD_ID.validate(name); // not <init> or <clinit>
        desc = Global.TRANSLATE_DESC(desc);
        NameDesc.DESC.validate(desc);
        String bootdesc = String.format(ARRAY_BOOT_DESC_FORMAT,bootdescplus);
        String bootstrap = "ST:" + bootmethod + bootdesc;
        Handle bootstrapmethod = S2O.parseHandle(bootstrap);
        return new ConstantDynamic(name, desc, bootstrapmethod, bootargs);
    }

}

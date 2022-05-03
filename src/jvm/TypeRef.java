package jvm;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.stream.Stream;

import static org.objectweb.asm.TypeReference.*;

import static jynx.Directive.*;
import static jynx.Global.LOG;
import static jynx.Message.*;

import jynx.Directive;
import jynx.LogIllegalArgumentException;

public enum TypeRef {

    trc_param(dir_param_type_annotation, Context.CLASS, CLASS_TYPE_PARAMETER, 1),
    trm_param(dir_param_type_annotation, Context.METHOD, METHOD_TYPE_PARAMETER, 1),
    trc_extends(dir_extends_type_annotation, Context.CLASS, CLASS_EXTENDS, 2),
    trc_param_bound(dir_param_bound_type_annotation, Context.CLASS, CLASS_TYPE_PARAMETER_BOUND, 1, 1),
    trm_param_bound(dir_param_bound_type_annotation, Context.METHOD, METHOD_TYPE_PARAMETER_BOUND, 1, 1),
    trf_field(dir_field_type_annotation, Context.FIELD, org.objectweb.asm.TypeReference.FIELD),
    trm_return(dir_return_type_annotation, Context.METHOD, METHOD_RETURN),
    trm_receiver(dir_receiver_type_annotation, Context.METHOD, METHOD_RECEIVER),
    trm_formal(dir_formal_type_annotation, Context.METHOD, METHOD_FORMAL_PARAMETER, 1),
    trm_throws(dir_throws_type_annotation, Context.METHOD, THROWS, 2),
    tro_var(dir_var_type_annotation, Context.CODE, LOCAL_VARIABLE),
    tro_resource(dir_resource_type_annotation, Context.CODE, RESOURCE_VARIABLE),
    trt_except(dir_except_type_annotation, Context.CATCH, EXCEPTION_PARAMETER, 2),
    tro_instanceof(dir_instanceof_type_annotation, Context.CODE, INSTANCEOF),
    tro_new(dir_new_type_annotation, Context.CODE, NEW),
    tro_newref(dir_newref_type_annotation, Context.CODE, CONSTRUCTOR_REFERENCE),
    tro_methodref(dir_methodref_type_annotation, Context.CODE, METHOD_REFERENCE),
    tro_cast(dir_cast_type_annotation, Context.CODE, CAST, -2, 1),
    tro_argnew(dir_argnew_type_annotation, Context.CODE, CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT, -2, 1),
    tro_argmethod(dir_argmethod_type_annotation, Context.CODE, METHOD_INVOCATION_TYPE_ARGUMENT, -2, 1),
    tro_argnewref(dir_argnewref_type_annotation, Context.CODE, CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT, -2, 1),
    tro_argmethodref(dir_argmethodref_type_annotation, Context.CODE, METHOD_REFERENCE_TYPE_ARGUMENT, -2, 1),;

    private final Directive dir;
    private final Context context;
    private final int sort;
    private final int shiftamt;
    private final int mask;
    private final int shiftamt2;
    private final int mask2;
    private final int numind;
    private final int unusedmask;
    private final int len1;
    private final int len2;

    private TypeRef(Directive dir, Context context, int sort, int... len) {
        this.dir = dir;
        String dirstr = dir.toString();
        int index = dirstr.indexOf("_type_annotation");
        assert name().substring(4).equals(dirstr.substring(1,index)):
                String.format("%s %s",name().substring(4),dirstr.substring(1,index));
        char type = name().charAt(2);
        switch (type) {
            case 'c':
                this.context = Context.CLASS;
                break;
            case 'f':
                this.context = Context.FIELD;
                break;
            case 'm':
                this.context = Context.METHOD;
                break;
            case 'o':
                this.context = Context.CODE;
                break;
            case 't':
                this.context = Context.CATCH;
                break;
            default:
                throw new AssertionError("unknown type " + type);
        }
        if (this.context != context) {
            String msg = String.format("typeref = %s context = %s implied context = %s",name(),context,this.context);
            throw new AssertionError(msg);
        }
        this.sort = sort;
        int unused = 0;
        assert len.length <= 2;
        len = Arrays.copyOf(len, 2);
        int lenarr1 = len[0];
        int lenarr2 = len[1];
        if (lenarr1 < 0) {
            unused = -lenarr1;
            lenarr1 = lenarr2;
            lenarr2 = 0;
        }
        assert (lenarr1 >= 0 && lenarr2 >= 0 && lenarr1 + lenarr2 <= 3);
        this.len1 = lenarr1;
        this.len2 = lenarr2;
        this.mask = (1 << lenarr1 * 8) - 1;
        this.mask2 = (1 << lenarr2 * 8) - 1;
        this.shiftamt = mask == 0 ? 0 : 24 - 8 * (unused + lenarr1);
        this.shiftamt2 = mask2 == 0 ? 0 : 24 - 8 * (lenarr1 + lenarr2);
        if (mask == 0) {
            numind = 0;
        } else if (mask2 == 0) {
            numind = 1;
        } else {
            numind = 2;
        }
        this.unusedmask = ~((-1 << 24) | (mask << shiftamt) | (mask2 << shiftamt2));
    }

    public Directive getDirective() {
        return dir;
    }

    public int getNumberIndices() {
        return numind;
    }

    private int getIndex(int typeref) {
        assert numind >= 1;
        int result = typeref >> shiftamt;
        return result & mask;
    }

    private int getBound(int typeref) {
        assert numind >= 2;
        int result = typeref >> shiftamt2;
        return result & mask2;
    }

    private static int shiftIndex(int index, int shiftamt, int mask) {
        if ((index & mask) != index) {
            // "type index (%d) outside range [0 - %d]"
            throw new LogIllegalArgumentException(M108,index,mask);
        }
        return index << shiftamt;
    }

    public int getTypeRef(int... index) {
        if (index.length != numind) {
            // "expected equal values for index length = %d numind = %d"
            throw new LogIllegalArgumentException(M150, index.length, numind);
        }
        int result = sort << 24;
        if (numind == 0) {
            return result;
        }
        result |= shiftIndex(index[0], shiftamt, mask);
        if (numind == 1) {
            return result;
        }
        result |= shiftIndex(index[1], shiftamt2, mask2);
        return result;
    }

    public Context context() {
        return context;
    }
    
    public int[] getBytes(int typref) {
        int n = len1 + len2 + 1;
        int[] result = new int[n];
        result[0] = sort;
        int i = 1;
        if (numind > 0) {
            int index = getIndex(typref);
            if (len1 == 2) {
                result[i] = index >> 8;
                ++i;
            }
            result[i] = index & 0xff;
            ++i;
            if (numind > 1) {
                result[i] = getBound(typref);
            }
        }
        return result;
    }
    
    public String debugString() {
        return String.format("%02x %d %#x %d %#x %#x %d %s%n",
                    sort,shiftamt,mask,shiftamt2,mask2, unusedmask,numind,this);
    }
    
    public String getTyperef(int typeref) {
        StringBuilder sb = new StringBuilder();
        if ((typeref & unusedmask) != 0) {
            // "unused field(s) in typeref not zero"
            throw new LogIllegalArgumentException(M202);
        }
        if (numind > 0) {
            if (sb.length() != 0) {
                sb.append(' ');
            }
            sb.append(getIndex(typeref));
        }
        if (numind > 1) {
            if (sb.length() != 0) {
                sb.append(' ');
            }
            sb.append(getBound(typeref));
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return name().substring(4);
    }

    public static TypeRef getInstance(Directive dir, Context context) {
        Context acctype = context == Context.COMPONENT? Context.FIELD:context;
        Optional<TypeRef> opttr = Stream.of(values())
                .filter(tr->tr.dir == dir && tr.context == acctype)
                .findAny();
        // "invalid type annotation directive - %s"
        return opttr.orElseThrow(()->new LogIllegalArgumentException(M170,dir));
    }
    
    public static TypeRef getInstance(int typeref) {
        int sort = typeref >> 24;
        for (TypeRef tr : values()) {
            if (tr.sort == sort) {
                return tr;
            }
        }
        // "invalid type ref sort - %d"
        throw new LogIllegalArgumentException(M178,sort);
    }

    public static int getIndexFrom(int typeref) {
        TypeRef tr = getInstance(typeref);
        return tr.getIndex(typeref);
    }

    public static void checkInst(TypeRef tr, AsmOp lastjop) {
        EnumSet<AsmOp> lastjops = null;
        switch (tr) {
            case tro_cast:
//                lastjops = EnumSet.of(JOp.opc_checkcast); // specification was vague on what instruction it may appear on
                break;
            case tro_instanceof:
//                lastjops = EnumSet.of(JOp.opc_instanceof); // specification was vague on what instruction it may appear on
                break;
            case tro_new:
                lastjops = EnumSet.of(AsmOp.asm_new);
                break;
            case tro_argmethod:
                lastjops = EnumSet.of(AsmOp.asm_invokeinterface, AsmOp.asm_invokestatic, AsmOp.asm_invokevirtual);
                break;
            case tro_argnew:
                lastjops = EnumSet.of(AsmOp.asm_invokespecial);
                break;
        }
        if (lastjops != null) {
            if (lastjop == null || !lastjops.contains(lastjop)) {
                LOG(M232, lastjop, lastjops); // "Last instruction was %s: expected %s"
            }
        }
    }

}

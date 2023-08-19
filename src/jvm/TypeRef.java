package jvm;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import static org.objectweb.asm.TypeReference.*;

import static jynx.Directive.*;
import static jynx.Message.*;

import jynx.Directive;
import jynx.LogIllegalArgumentException;

public enum TypeRef {

    // Table 4.7.20-A
    trc_param(0x00,dir_param_type_annotation, Context.CLASS, CLASS_TYPE_PARAMETER, 1),
    trm_param(0x01,dir_param_type_annotation, Context.METHOD, METHOD_TYPE_PARAMETER, 1),
    trc_extends(0x10,dir_extends_type_annotation, Context.CLASS, CLASS_EXTENDS, 2),
    trc_param_bound(0x11,dir_param_bound_type_annotation, Context.CLASS, CLASS_TYPE_PARAMETER_BOUND, 1, 1),
    trm_param_bound(0x12,dir_param_bound_type_annotation, Context.METHOD, METHOD_TYPE_PARAMETER_BOUND, 1, 1),
    trf_field(0x13,dir_field_type_annotation, Context.FIELD, org.objectweb.asm.TypeReference.FIELD),
    trm_return(0x14,dir_return_type_annotation, Context.METHOD, METHOD_RETURN),
    trm_receiver(0x15,dir_receiver_type_annotation, Context.METHOD, METHOD_RECEIVER),
    trm_formal(0x16,dir_formal_type_annotation, Context.METHOD, METHOD_FORMAL_PARAMETER, 1),
    trm_throws(0x17,dir_throws_type_annotation, Context.METHOD, THROWS, 2),
    // Table 4.7.20-B
    tro_var(0x40,dir_var_type_annotation, Context.CODE, LOCAL_VARIABLE),
    tro_resource(0x41,dir_resource_type_annotation, Context.CODE, RESOURCE_VARIABLE),
    trt_except(0x42,dir_except_type_annotation, Context.CATCH, EXCEPTION_PARAMETER, 2),
    tro_instanceof(0x43,dir_instanceof_type_annotation, Context.CODE, INSTANCEOF),
    tro_new(0x44,dir_new_type_annotation, Context.CODE, NEW),
    tro_newref(0x45,dir_newref_type_annotation, Context.CODE, CONSTRUCTOR_REFERENCE),
    tro_methodref(0x46,dir_methodref_type_annotation, Context.CODE, METHOD_REFERENCE),
    tro_cast(0x47,dir_cast_type_annotation, Context.CODE, CAST, -2, 1),
    tro_argnew(0x48,dir_argnew_type_annotation, Context.CODE, CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT, -2, 1),
    tro_argmethod(0x49,dir_argmethod_type_annotation, Context.CODE, METHOD_INVOCATION_TYPE_ARGUMENT, -2, 1),
    tro_argnewref(0x4a,dir_argnewref_type_annotation, Context.CODE, CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT, -2, 1),
    tro_argmethodref(0x4b,dir_argmethodref_type_annotation, Context.CODE, METHOD_REFERENCE_TYPE_ARGUMENT, -2, 1),;

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

    private TypeRef(int value, Directive dir, Context context, int sort, int... len) {
        this.dir = dir;
        String dirstr = dir.toString();
        int index = dirstr.indexOf("_type_annotation");
        assert name().substring(4).equals(dirstr.substring(1,index)):
                String.format("%s %s",name().substring(4),dirstr.substring(1,index));
        char type = name().charAt(2);
        char expected;
        switch (context) {
            case CLASS:
                expected = 'c';
                break;
            case FIELD:
                expected = 'f';
                break;
            case METHOD:
                expected = 'm';
                break;
            case CODE:
                expected = 'o';
                break;
            case CATCH:
                expected = 't';
                break;
            default:
                throw new EnumConstantNotPresentException(context.getClass(), context.name());
        }
        assert type == expected:
                String.format("typeref = %s context = %s type = '%c' expected = '%c;",
                        name(),context,type,expected);
        this.context = context;
        this.sort = sort;
        assert value == sort;
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
    
    public String getTypeRefString(int typeref) {
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
    
    public static TypeRef fromASM(int typeref) {
        int sort = typeref >> 24;
        return fromJVM(sort);
    }

    public static TypeRef fromJVM(int sort) {
        return Stream.of(values())
                .filter(tr->tr.sort == sort)
                .findAny()
                // "invalid type ref sort - %d"
                .orElseThrow(() -> new LogIllegalArgumentException(M178,sort));
    }

    public static int getIndexFrom(int typeref) {
        TypeRef tr = fromASM(typeref);
        return tr.getIndex(typeref);
    }

}

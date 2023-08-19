package checker;

import static jynx.Global.JVM_VERSION;
import static jynx.Global.LOG;
import static jynx.Message.M507;
import static jynx.Message.M516;
import static jynx.Message.M518;
import static jynx.Message.M519;
import static jynx.Message.M521;
import static jynx.Message.M523;

import jvm.AttributeEntry;
import jvm.ConstantPoolType;
import jvm.Context;
import jvm.FrameType;
import jvm.JvmVersion;
import jvm.StandardAttribute;
import jvm.TypeRef;
import jynx.LogIllegalArgumentException;

public class SimpleAttribute extends AbstractAttribute {

    public SimpleAttribute(Context context, StandardAttribute attr, Buffer buffer) {
        super(attr, context, attr.name(), buffer);
    }

    @Override
    public void checkCPEntries(int codesz, int maxlocals) {
        int ct = itemCount();
        AttributeEntry[] entries = attr.entries();
        for (int i = 0; i < ct; ++i) {
            for (AttributeEntry entry:entries) {
                if (!entry.isCP()) {
                    switch(entry) {
                        case LABEL_LENGTH:
                            int pc = buffer.nextLabel(codesz);
                            int length = buffer.nextUnsignedShort();
                            if (pc + length > codesz) {
                                // "label offset %d length %d is > code size %d"
                                LOG(M516, pc, length, codesz);
                            }
                            break;
                        case LABEL:
                            buffer.nextLabel(codesz);
                            break;
                        case LV_INDEX:
                            int lvindex = buffer.nextUnsignedShort();
                            if (lvindex >= maxlocals) {
                                // "local variable %d is >= max locals %d"
                                LOG(M518, lvindex, maxlocals);
                            }
                            break;
                        case ACCESS:
                        case USHORT:
                            buffer.nextUnsignedShort();
                            break;
                        case ANNOTATION:
                            checkAnnotation();
                            break;
                        case DEFAULT_ANNOTATION:
                            checkElementValue();
                            break;
                        case PARAMETER_ANNOTATION:
                            checkParameterAnnotation();
                            break;
                        case TYPE_ANNOTATION:
                            checkTypeAnnotation();
                            break;
                        case INLINE_UTF8:
                            CPEntry.fromUTF8CP(buffer.bb());
                            break;
                        case FRAME:
                            checkStackFrame();
                            break;
                        case BOOTSTRAP:
                            checkBootstrap();
                            break;
                        default:
                            throw new EnumConstantNotPresentException(entry.getClass(), entry.name());
                    }
                    continue;
                }
                int cpindex = buffer.nextUnsignedShort();
                if (entry.isOptional() && cpindex == 0) {
                    continue;
                }
                CPEntry cp = pool.getEntry(cpindex);
                ConstantPoolType cptype = cp.getType();
                if (!entry.contains(cptype)) {
                    // "cpentry type %s is invalid for %s"
                    LOG(M519, cptype, entry);
                }
            }
        }
    }

    private int itemCount() {
        int ct;
        switch (attr.type()) {
            case FIXED:
                ct = 1;
                break;
            case ARRAY1:
                ct = buffer.nextUnsignedByte();
                break;
            case ARRAY:
                ct = buffer.nextUnsignedShort();
                break;
            default:
                throw new AssertionError();
        }
        return ct;
    }
    
    private void checkBootstrap() {
        CPEntry methodcp = buffer.nextCPEntry(ConstantPoolType.CONSTANT_MethodHandle);
        JvmVersion jvmversion = JVM_VERSION();
        int argct = buffer.nextUnsignedShort();
        CPEntry[] entries = new CPEntry[1 + argct];
        entries[0] = methodcp;
        for (int k = 0; k < argct; ++k) {
            CPEntry argcp = buffer.nextCPEntry();
            ConstantPoolType cptk = argcp.getType();
            if (!cptk.isLoadableBy(jvmversion)) {
                // "boot argument %s is not loadable by %s"
                LOG(M507, cptk, jvmversion);
            }
            entries[k + 1] = argcp;
        }
        pool.addBootstraps(entries);
    }

    private void checkAnnotation() {
        buffer.nextCPEntry(ConstantPoolType.CONSTANT_Utf8); // type
        checkAnnotationValues();
    }

    private void checkAnnotationValues() {
        int valuect = buffer.nextUnsignedShort();
        for (int i = 0; i < valuect; ++i) {
            buffer.nextCPEntry(ConstantPoolType.CONSTANT_Utf8); // name
            checkElementValue();
        }
    }

    private void checkParameterAnnotation() {
        int valuect = buffer.nextUnsignedShort();
        for (int i = 0; i < valuect; ++i) {
            checkAnnotation();
        }
    }

    private void checkArrayValues() {
        int valuect = buffer.nextUnsignedShort();
        for (int i = 0; i < valuect; ++i) {
            checkElementValue();
        }
    }

    private void checkElementValue() {
        char tag = (char)buffer.nextUnsignedByte();
        switch(tag) {
            case 'B':
            case 'C':
            case 'I':
            case 'S':
            case 'Z':
                buffer.nextCPEntry(ConstantPoolType.CONSTANT_Integer);
                break;
            case 'J':
                buffer.nextCPEntry(ConstantPoolType.CONSTANT_Long);
                break;
            case 'F':
                buffer.nextCPEntry(ConstantPoolType.CONSTANT_Float);
                break;
            case 'D':
                buffer.nextCPEntry(ConstantPoolType.CONSTANT_Double);
                break;
            case 's':
                buffer.nextCPEntry(ConstantPoolType.CONSTANT_Utf8);
                break;
            case 'e':
                buffer.nextCPEntry(ConstantPoolType.CONSTANT_Utf8); // type
                buffer.nextCPEntry(ConstantPoolType.CONSTANT_Utf8); // name
                break;
            case 'c':
                buffer.nextCPEntry(ConstantPoolType.CONSTANT_Utf8); // name
                break;
            case '@':
                checkAnnotation();
                break;
            case '[':
                checkArrayValues();
                break;
            default:
                String msg = String.format("unknown annotation tag '%c'", tag);
                throw new IllegalArgumentException(msg);
        }
    }

    private void checkTypeAnnotation() {
        int target_type = buffer.nextUnsignedByte();
        TypeRef typeref = TypeRef.fromJVM(target_type);
        Context expected = typeref.context();
        if (expected == Context.CATCH) {
            expected = Context.CODE;
        }
        if (expected != context && expected != Context.FIELD && context != Context.COMPONENT) {
            // "typeref %s (%#x) not valid in context %s"
            throw new LogIllegalArgumentException(M523, typeref, target_type, context);
        }
        switch(typeref) {
            case trc_param:
            case trm_param:
                // type_parameter_target
                buffer.nextUnsignedByte();
                break;
            case trc_extends:
                // supertype_target
                buffer.nextUnsignedShort();
                break;
            case trc_param_bound:
            case trm_param_bound:
                // type_parameter_bound_target
                buffer.nextUnsignedByte();
                buffer.nextUnsignedByte();
                break;
            case trf_field:
            case trm_return:
            case trm_receiver:
                // empty_target
                break;
            case trm_formal:
                // formal_parameter_target
                buffer.nextUnsignedByte();
                break;
            case trm_throws:
                // throws_target
                buffer.nextUnsignedShort();
                break;
            case tro_var:
            case tro_resource:
                // local_var_target
                int table_length = buffer.nextUnsignedShort();
                for (int i = 0; i < table_length; ++i) {
                    buffer.nextUnsignedShort();
                    buffer.nextUnsignedShort();
                    buffer.nextUnsignedShort();
                }
                break;
            case trt_except:
                // catch_target
                buffer.nextUnsignedShort();
                break;
            case tro_instanceof:
            case tro_new:
            case tro_newref:
            case tro_methodref:
                // offset_target
                buffer.nextUnsignedShort();
                break;
            case tro_cast:
            case tro_argnew:
            case tro_argmethod:
            case tro_argnewref:
            case tro_argmethodref:
                // type_argument target
                buffer.nextUnsignedShort();
                buffer.nextUnsignedByte();
                break;
            default:
                throw new EnumConstantNotPresentException(typeref.getClass(),typeref.name());
        }
        // type path
        int path_length = buffer.nextUnsignedByte();
        for (int i = 0; i < path_length; ++i) {
            int type_path_kind = buffer.nextUnsignedByte();
            if (type_path_kind > 4) {
                
            }
            int type_argument_index = buffer.nextUnsignedByte();
            if (type_argument_index != 0 && type_path_kind != 3) {
                
            }
        }
        checkAnnotation();
    }

    private void checkStackFrame() {
        int tag = buffer.nextUnsignedByte();
        if (tag < 64) { // same frame
            return;
        }
        if (tag < 128) { // same_locals_1_stack_item_frame
            checkVerificationTypeInfo(1);
            return;
        }
        switch (tag) {
            default:  // future use
                // "invalid tag %d"
                throw new LogIllegalArgumentException(M521,tag);
            case 247: // same_locals_1_stack_item_frame_extended
                buffer.nextUnsignedShort();
                checkVerificationTypeInfo(1);
                break;
            case 248: // chop
            case 249:
            case 250:
                buffer.nextUnsignedShort();
                break;
            case 251: // same_frame_extended
                buffer.nextUnsignedShort();
                break;
            case 252: // append_frame
            case 253:
            case 254:
                buffer.nextUnsignedShort();
                checkVerificationTypeInfo(tag - 251);
                break;
            case 255: // full_frame
                buffer.nextUnsignedShort();
                int locals = buffer.nextUnsignedShort();
                checkVerificationTypeInfo(locals);
                int stack = buffer.nextUnsignedShort();
                checkVerificationTypeInfo(stack);
                break;
        }
    }
    
    private void checkVerificationTypeInfo(int size) {
        for (int i = 0; i <size; ++i) {
            int tag = buffer.nextUnsignedByte();
            FrameType ft = FrameType.fromJVMType(tag);
            if (ft.asmType() == null) {
                buffer.nextUnsignedShort();
            }
        }
    }
}

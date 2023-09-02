package checker;

import java.util.function.BiConsumer;

import static jynx.Message.M523;

import jvm.AttributeEntry;
import jvm.ConstantPoolType;
import jvm.Context;
import jvm.TypeRef;
import jynx.LogIllegalArgumentException;

public class AnnotationEntry {

    private final AttributeBuffer buffer;
    private final IndentPrinter ptr;

    public AnnotationEntry(AttributeBuffer buffer, IndentPrinter ptr) {
        this.buffer = buffer;
        this.ptr = ptr;
    }

    public static BiConsumer<IndentPrinter,AttributeBuffer> get(AttributeEntry entry) {
        switch(entry) {
            case ANNOTATION:
                return AnnotationEntry::annotationCheck;
            case DEFAULT_ANNOTATION:
                return AnnotationEntry::defaultAnnotationCheck;
            case PARAMETER_ANNOTATION:
                return AnnotationEntry::parameterAnnotationCheck;
            case TYPE_ANNOTATION:
                return AnnotationEntry::typeAnnotationCheck;
            default:
                throw new EnumConstantNotPresentException(entry.getClass(), entry.name());
        }
    }
    
    private static void annotationCheck(IndentPrinter ptr, AttributeBuffer buffer) {
        AnnotationEntry x = new AnnotationEntry(buffer, ptr);
        x.checkAnnotation();
    }
    
    private static void defaultAnnotationCheck(IndentPrinter ptr, AttributeBuffer buffer) {
        AnnotationEntry x = new AnnotationEntry(buffer,ptr);
        x.checkElementValue();
    }
    
    private static void parameterAnnotationCheck(IndentPrinter ptr, AttributeBuffer buffer) {
        AnnotationEntry x = new AnnotationEntry(buffer,ptr);
        x.checkParameterAnnotation();
    }
    
    private static void typeAnnotationCheck(IndentPrinter ptr, AttributeBuffer buffer) {
        AnnotationEntry x = new AnnotationEntry(buffer,ptr);
        x.checkTypeAnnotation();
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
        Context context = buffer.context();
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
                buffer.asCodeBuffer().nextLabel();
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

}
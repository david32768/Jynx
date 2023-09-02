package checker;

import java.util.EnumMap;
import java.util.function.BiConsumer;

import static jynx.Global.JVM_VERSION;
import static jynx.Global.LOG;
import static jynx.Global.OPTION;
import static jynx.Message.M507;
import static jynx.Message.M521;

import jvm.AttributeEntry;
import jvm.ConstantPoolType;
import jvm.FrameType;
import jvm.JvmVersion;
import jynx.GlobalOption;
import jynx.LogIllegalArgumentException;

public class AttributeChecker {

    private AttributeChecker() {
    }
    
    private static final EnumMap<AttributeEntry, BiConsumer<IndentPrinter,AttributeBuffer>> MAP;
    
    static {
        MAP = new EnumMap<>(AttributeEntry.class);
        for (AttributeEntry entry:AttributeEntry.values()) {
            switch(entry) {
                case ANNOTATION:
                case DEFAULT_ANNOTATION:
                case PARAMETER_ANNOTATION:
                case TYPE_ANNOTATION:
                    MAP.put(entry, AnnotationEntry.get(entry));
                    break;
                case LABEL:
                    MAP.put(entry, (ptr,buffer) -> buffer.asCodeBuffer().nextLabel());
                    break;
                case LV_INDEX:
                    MAP.put(entry, (ptr,buffer) -> buffer.asCodeBuffer().nextVar());
                    break;
                case ACCESS:
                case USHORT:
                    MAP.put(entry, (ptr,buffer) -> buffer.nextUnsignedShort());
                    break;
                case LABEL_LENGTH:
                    MAP.put(entry, (ptr,buffer) -> checkLabelLength(buffer.asCodeBuffer()));
                    break;
                case INLINE_UTF8:
                    MAP.put(entry, (ptr,buffer) -> CPEntry.fromUTF8CP(buffer.bb()));
                    break;
                case FRAME:
                    MAP.put(entry, (ptr,buffer) -> checkStackFrame(buffer.asCodeBuffer()));
                    break;
                case BOOTSTRAP:
                    MAP.put(entry, AttributeChecker::checkBootstrap);
                    break;
            }
        }
    }
    
    public static void check(AttributeEntry entry, IndentPrinter ptr, AttributeBuffer buffer) {
        assert !entry.isCP();
        BiConsumer<IndentPrinter,AttributeBuffer> checker = MAP.get(entry);
        if (checker == null) {
                throw new EnumConstantNotPresentException(entry.getClass(), entry.name());
        } else {
            checker.accept(ptr, buffer);
        }
    }
    
    private static void checkLabelLength(CodeBuffer buffer) {
        int start = buffer.nextLabel();
        int end = buffer.nextEndOffset(start);
    }

    private static void checkBootstrap(IndentPrinter ptr, AttributeBuffer buffer) {
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
        if (OPTION(GlobalOption.DETAIL)) {
            buffer.pool().addBootstraps(entries, ptr);
        } else {
            buffer.pool().addBootstraps(entries);
        }
    }

    private static void checkStackFrame(CodeBuffer buffer) {
        int tag = buffer.nextUnsignedByte();
        int delta;
        if (tag < 64) { // same frame
            delta = tag;
        } else if (tag < 128) { // same_locals_1_stack_item_frame
            delta = tag - 64;
            checkVerificationTypeInfo(buffer,1);
        } else {
            switch (tag) {
                default:  // future use
                    // "invalid tag %d"
                    throw new LogIllegalArgumentException(M521,tag);
                case 247: // same_locals_1_stack_item_frame_extended
                    delta = buffer.nextUnsignedShort();
                    checkVerificationTypeInfo(buffer,1);
                    break;
                case 248: // chop
                case 249:
                case 250:
                    delta = buffer.nextUnsignedShort();
                    break;
                case 251: // same_frame_extended
                    delta = buffer.nextUnsignedShort();
                    break;
                case 252: // append_frame
                case 253:
                case 254:
                    delta = buffer.nextUnsignedShort();
                    checkVerificationTypeInfo(buffer,tag - 251);
                    break;
                case 255: // full_frame
                    delta = buffer.nextUnsignedShort();
                    int locals = buffer.nextUnsignedShort();
                    checkVerificationTypeInfo(buffer,locals);
                    int stack = buffer.nextUnsignedShort();
                    checkVerificationTypeInfo(buffer,stack);
                    break;
            }
        }
        buffer.addDelta(delta);
    }
    
    private static void checkVerificationTypeInfo(CodeBuffer buffer,int size) {
        for (int i = 0; i <size; ++i) {
            int tag = buffer.nextUnsignedByte();
            FrameType ft = FrameType.fromJVMType(tag);
            if (ft.asmType() == null) {
                buffer.nextUnsignedShort();
            }
        }
    }
}

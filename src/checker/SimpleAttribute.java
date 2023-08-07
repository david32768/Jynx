package checker;

import java.io.PrintWriter;
import java.util.Objects;

import static jynx.Global.LOG;
import static jynx.Message.M516;
import static jynx.Message.M518;
import static jynx.Message.M519;

import jvm.AttributeEntry;
import jvm.AttributeName;
import jvm.ConstantPoolType;

public class SimpleAttribute extends AbstractAttribute {

    public SimpleAttribute(int level, AttributeName attr, Buffer buffer) {
        super(attr,level,attr.name(),buffer);
    }

    @Override
    public void checkCPEntries(PrintWriter pw, ConstantPool pool, int codesz, int maxlocals) {
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
                        case INLINE_UTF8:
                        case ANNOTATION:
                        case TYPE_ANNOTATION:
                        case FRAME:
                            if (entries.length != 1) {
                                throw new AssertionError();
                            }
                            buffer.advanceToLimit();
                            return;
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
                Objects.nonNull(cp);
                ConstantPoolType cptype = cp.getType();
                if (!entry.contains(cptype)) {
                    // "cpentry type %s is invalid for %s"
                    LOG(M519, cptype, entry);
                }
            }
        }
    }

}

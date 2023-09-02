package checker;

import static jynx.Global.LOG;
import static jynx.Message.M519;

import jvm.AttributeEntry;
import jvm.ConstantPoolType;
import jvm.StandardAttribute;

public class SimpleAttribute extends AttributeInstance {

    public SimpleAttribute(StandardAttribute attr, AttributeBuffer buffer) {
        super(attr, attr.name(), buffer);
    }

    @Override
    public void checkCPEntries(IndentPrinter ptr) {
        int ct = itemCount();
        AttributeEntry[] entries = attr.entries();
        for (int i = 0; i < ct; ++i) {
            for (AttributeEntry entry:entries) {
                if (!entry.isCP()) {
                    AttributeChecker.check(entry, ptr, buffer);
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
    
}

package checker;

import java.util.Optional;
import static jynx.Global.LOG;
import static jynx.Message.M506;
import static jynx.Message.M519;

import jvm.AttributeEntry;
import jvm.ConstantPoolType;
import jvm.StandardAttribute;
import jynx.LogIllegalArgumentException;

public class SimpleAttribute extends AttributeInstance {

    public SimpleAttribute(StandardAttribute attr, AttributeBuffer buffer) {
        super(attr, buffer);
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
                Optional<CPEntry> optentry = buffer.nextOptCPEntry();
                if (!optentry.isPresent() && entry.isOptional()) {
                        continue;
                }
                // "non-optional constant pool entry is missing; expected %s"
                CPEntry cp = optentry.orElseThrow(() -> new LogIllegalArgumentException(M506,entry));
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

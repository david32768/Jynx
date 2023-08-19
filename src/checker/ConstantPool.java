package checker;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static jynx.Message.M505;
import static jynx.Message.M514;
import static jynx.Message.M515;
import static jynx.Message.M520;
import static jynx.Message.M522;

import jvm.ConstantPoolType;
import jvm.HandleType;
import jvm.JvmVersion;
import jynx.LogIllegalArgumentException;
import jynx.LogIllegalStateException;

public class ConstantPool {

    private final CPEntry[] entries;
    private final JvmVersion jvmVersion;
    private final List<CPEntry[]> bootstraps;

    private int maxboot;

    private ConstantPool(CPEntry[] entries, JvmVersion jvmversion) {
        this.entries = entries.clone();
        this.jvmVersion = jvmversion;
        this.bootstraps = new ArrayList<>();
        this.maxboot = -1;
    }

    public void addBootstraps(CPEntry[] bootstrap) {
        this.bootstraps.add(bootstrap);
    }
    
    public int last() {
        int last = entries.length - 1;
        return entries[last] == null? last - 1: last;
    }

    public static ConstantPool getInstance(ByteBuffer bb, JvmVersion jvmversion) {
        int entryct = Short.toUnsignedInt(bb.getShort());
        CPEntry[] entries = new CPEntry[entryct];
        for (int i = 1; i < entryct;++i) {
            CPEntry cp  = CPEntry.fromConstantPool(bb);
            entries[i] = cp;
            ConstantPoolType type = cp.getType();
            if (type.usesTwoSlots()) {
                ++i;
            }
        }
        return new ConstantPool(entries,jvmversion);
    }

    public ConstantPoolType getType(int index) {
        return getEntry(index).getType();
    }
    
    public Object getValue(int index) {
        return getEntry(index).getValue();
    }
    
    public CPEntry getEntry(int index) {
        if (index < 1 || index >= entries.length) {
            // "CP index %d is not in [1,%d]"
            throw new LogIllegalArgumentException(M520, index, entries.length - 1);
        }
        CPEntry result = entries[index];
        if (result == null) {
            CPEntry previous = entries[index - 1]; 
            if (previous != null && previous.getType().usesTwoSlots()) {
                // "CPIndex %d is invalid as points to middle of %s entry"
                throw new LogIllegalArgumentException(M522, index , previous.getType());
            } else {
                throw new AssertionError();
            }
        }
        return result;
    }

    public int getMaxboot() {
        return maxboot;
    }
    
    private void checkPoolType (int base,int index,EnumSet<ConstantPoolType> expected) {
        ConstantPoolType actual = getType(index);
        if (!expected.contains(actual)){
            // "entry %d (%s) refers to index %d (%s) but expected to be in %s"
            throw new LogIllegalArgumentException(M514, base,getType(base), index, actual, expected);
        }
    }
    
    private void checkPoolType (int base,int index, ConstantPoolType expected) {
        checkPoolType(base,index,EnumSet.of(expected));
    }
    
    public void check() {
        for (int i = 1; i < entries.length; ++i) {
            CPEntry cp = entries[i];
            if (cp == null) {
                continue;
            }
            ConstantPoolType.EntryType et = cp.getType().getEntryType();
            Object value = cp.getValue();
            switch(et) {
                case INDIRECT:
                    int[] indices = (int[])value;
                    ConstantPoolType[] types = cp.getType().getPool();
                    if (indices.length != types.length) {
                        // "number of cp entries (%d) does not equal number required for type (5d)"
                        throw new LogIllegalArgumentException(M515, indices.length, types.length);
                    }
                    for (int j = 0; j < indices.length; ++j) {
                        checkPoolType(i,indices[j],types[j]);
                    }
                    break;
                case HANDLE:
                    indices = (int[])value;
                    int tag = indices[0];
                    int index = indices[1];
                    HandleType ht = HandleType.getInstance(tag);
                    checkPoolType(i,index,ht.getValidCPT(jvmVersion));
                    break;
                case BOOTSTRAP:
                    indices = (int[])value;
                    types = cp.getType().getPool();
                    int bootstrap = indices[0];
                    maxboot = Math.max(maxboot,bootstrap);
                    checkPoolType(i,indices[1],types[0]);
                    break;
                case LONG:
                case DOUBLE:
                    ++i;
                    break;
            }
        }
    }
    
    public void checkBootstraps() {
        int ct = bootstraps.size();
        if (maxboot >= 0 && ct <= maxboot) {
            // "maximum bootcp used by constant pool (%d)  is greater than supplied in attribute (size = %d)"
            throw new LogIllegalStateException(M505, maxboot, ct);
        }
    }
    
    private String toString(int index) {
        CPEntry cpe = getEntry(index);
        return stringValue(cpe);
    }
    
    public String stringValue(CPEntry cpe) {
        Object value = cpe.getValue();
        switch(cpe.getType().getEntryType()) {
            case INDIRECT:
                int[] indices = (int[])value;
                StringBuilder sb = new StringBuilder();
                boolean first = true;
                for (int i:indices) {
                    if (!first) {
                        sb.append(' ');
                    }
                    sb.append(toString(i));
                    first = false;
                }
                return sb.toString();
            case HANDLE:
                indices = (int[])value;
                HandleType ht = HandleType.getInstance(indices[0]);
                return String.format("%s %s",ht,toString(indices[1]));
            case BOOTSTRAP:
                indices = (int[])value;
                return String.format("BOOTSTRAP %d %s",indices[0],toString(indices[1]));
        }
        return value.toString();
    }

    public void printCP(IndentPrinter ptr) {
        ptr.println("CONSTANT POOL ENTRIES");
        IndentPrinter entryptr = ptr.shift();
        for (int i = 0; i < entries.length; ++i) {
            CPEntry cp = entries[i];
            if (cp == null) {
                continue;
            }
            entryptr.println("%4d %-24s %s", i,cp.getType(),stringValue(cp));
        }
    }

    public void printBoot(IndentPrinter ptr) {
        if (bootstraps.isEmpty()) {
            return;
        }
        ptr.println("BOOTSTRAP ENTRIES");
        IndentPrinter bootptr = ptr.shift();
        IndentPrinter methodptr = bootptr.shift();
        IndentPrinter argptr = methodptr.shift();
        for (int i = 0; i < bootstraps.size(); ++i) {
            CPEntry[] boots = bootstraps.get(i);
            CPEntry methodcp = boots[0];
            bootptr.println("BOOTSTRAP %d",i);
            methodptr.println(stringValue(methodcp));
            for (int k = 1; k < boots.length; ++k) {
                CPEntry argcp = boots[k];
                argptr.println("%d %-24s %s",k - 1,argcp.getType(),stringValue(argcp));
            }
        }
    }
}

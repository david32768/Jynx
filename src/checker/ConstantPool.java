package checker;

import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.EnumSet;

import static jynx.Message.M514;
import static jynx.Message.M515;
import static jynx.Message.M520;

import jvm.ConstantPoolType;
import jvm.HandleType;
import jvm.JvmVersion;
import jynx.LogIllegalArgumentException;

public class ConstantPool {

    private final CPEntry[] entries;
    private final JvmVersion jvmVersion;
    private int maxboot;

    private ConstantPool(CPEntry[] entries, JvmVersion jvmversion) {
        this.entries = entries;
        this.jvmVersion = jvmversion;
        this.maxboot = 0;
    }

    public static ConstantPool getInstance(ByteBuffer bb, JvmVersion jvmversion) {
        int entryct = Short.toUnsignedInt(bb.getShort());
        CPEntry[] entries = new CPEntry[entryct];
        for (int i = 1; i < entryct;++i) {
            CPEntry cp  = CPEntry.getInstance(bb);
            entries[i] = cp;
            ConstantPoolType type = cp.getType();
            if (type == ConstantPoolType.CONSTANT_Long || type == ConstantPoolType.CONSTANT_Double) {
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
        if (index < 0 || index >= entries.length) {
            // "CP index %d is not in [0,%d]"
            throw new LogIllegalArgumentException(M520, index, entries.length - 1);
        }
        return entries[index];
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
                for (int i:indices) {
                    sb.append(toString(i)).append(' ');
                }
                return sb.toString();
            case HANDLE:
                indices = (int[])value;
                HandleType ht = HandleType.getInstance(indices[0]);
                return String.format("%s %s",ht,toString(indices[1]));
            case BOOTSTRAP:
                indices = (int[])value;
                return String.format("bootstrap %d %s",indices[0],toString(indices[1]));
        }
        return value.toString();
    }
    
    public void print(PrintWriter pw) {
        for (int i = 0; i < entries.length; ++i) {
            CPEntry cp = entries[i];
            if (cp != null) {
                pw.format("%4d %-24s %s%n", i,cp.getType(),toString(i));
            }
        }
    }
}

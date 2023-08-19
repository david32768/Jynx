package checker;

import java.nio.ByteBuffer;
import java.util.Optional;

import static jynx.Global.LOG;
import static jynx.Message.M501;
import static jynx.Message.M506;
import static jynx.Message.M509;

import jvm.ConstantPoolType;
import jynx.LogIllegalArgumentException;

public class Buffer {

    private final ConstantPool pool;
    private final ByteBuffer bb;

    public Buffer(ConstantPool pool, ByteBuffer bb) {
        this.pool = pool;
        this.bb = bb;
    }

    public ByteBuffer bb() {
        return bb;
    }

    public ConstantPool pool() {
        return pool;
    }
    
    public int nextUnsignedByte() {
        return Byte.toUnsignedInt(bb.get());
    }
    
    public int nextShort() {
        return bb.getShort();
    }
    
    public int nextUnsignedShort() {
        return Short.toUnsignedInt(bb.getShort());
    }
    
    public int nextLabel(int codesz) {
        int offset = nextUnsignedShort();
        if (offset > codesz) {
            // "label offset (%d) > code size (%d)"
            LOG(M501, offset, codesz);
        }
        return offset;
    }
    
    public int nextSize() {
        int size = bb.getInt();
        if (size < 0 || size > bb.remaining()) {
            // "size (%#x) is greater than (%#x) remaining"
            LOG(M509,Integer.toUnsignedLong(size), bb.remaining());
            size = bb.remaining();
        }
        return size;
    }
    
    public CPEntry nextCPEntry() {
        return pool.getEntry(nextUnsignedShort());
    }
    
    public CPEntry nextCPEntry(ConstantPoolType cptype) {
        CPEntry cp = nextCPEntry();
        ConstantPoolType actualcpt = cp.getType();
        if (actualcpt != cptype) {
            // "CP entry is %s but should be %s"
            throw new LogIllegalArgumentException(M506, actualcpt.toString(), cptype.toString());
        }
        return cp;
    }
    
    public Optional<CPEntry> nextOptCPEntry(ConstantPoolType cptype) {
        int cpindex = nextUnsignedShort();
        if (cpindex == 0) {
            return Optional.empty();
        }
        CPEntry cp = pool.getEntry(cpindex);
        ConstantPoolType actualcpt = cp.getType();
        if (actualcpt != cptype) {
            // "CP entry is %s but should be %s"
            throw new LogIllegalArgumentException(M506, actualcpt.toString(), cptype.toString());
        }
        return Optional.of(cp);
    }
    
    public void advance(int increment) {
        assert increment >= 0;
        int position = bb.position();
        position = Math.addExact(position,increment);
        bb.position(position);
    }
    
    public Object nextPoolValue() {
        int index = nextUnsignedShort();
        return pool.getValue(index);
    }

    public ConstantPoolType nextPoolType() {
        int methodref = nextUnsignedShort();
        return pool.getType(methodref);
    }
    
    public Buffer duplicate() {
        return new Buffer(pool,bb.duplicate());
    }
    
    public int position() {
        return bb.position();
    }
    
    public void advanceToLimit() {
        bb.position(bb.limit());
    }
    
    public boolean hasRemaining() {
        return bb.hasRemaining();
    }
    
    public int remaining() {
        return bb.remaining();
    }

    public int limit() {
        return bb.limit();
    }
    
    public void limit(int limit) {
        bb.limit(limit);
    }
}

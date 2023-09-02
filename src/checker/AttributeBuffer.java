package checker;

import java.nio.ByteBuffer;

import jvm.Context;

public class AttributeBuffer extends Buffer {

    private final Context context;

    public AttributeBuffer(ConstantPool pool, ByteBuffer bb, Context context) {
        super(pool, bb);
        this.context = context;
    }

    @Override
    protected AttributeBuffer duplicate() {
        return new AttributeBuffer(pool(), bb().duplicate(), context);
    }
    
    @Override
    public AttributeBuffer extract(int size) {
        AttributeBuffer partbuff = duplicate();
        advance(size);
        partbuff.limit(position());
        return partbuff;
    }

    public Context context() {
        return context;
    }
        
}

package checker;

import java.nio.ByteBuffer;

import jvm.Context;

public class AttributeBuffer extends Buffer {

    private final Context context;
    private final String name;

    public AttributeBuffer(ConstantPool pool, ByteBuffer bb, Context context, String name) {
        super(pool, bb);
        this.context = context;
        this.name = name;
    }

    public Context context() {
        return context;
    }

    public String name() {
        return name;
    }
        
    public int level() {
        switch(context) {
                case COMPONENT:
                case CODE:
                    return 2;
                case CLASS:
                    return 0;
                default:
                    return 1;
        }
    }
    
    public CodeBuffer codeBuffer(IndentPrinter ptr) {
        assert context == Context.METHOD;
        nextShort(); // max stack
        int maxlocals = nextShort(); //max locals
        int codesz = nextSize(); // code length
        InstBuffer instbuff = new InstBuffer(pool, extract(codesz), maxlocals, codesz);
        instbuff.checkInsn(ptr.shift());
        CodeBuffer result = instbuff.codeBuffer(bb.duplicate());
        bb.position(bb.limit());
        return result;
    }
    
}

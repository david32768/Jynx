package checker;

import java.nio.ByteBuffer;

import static jynx.Global.LOG;
import static jynx.Message.M518;

import jvm.Context;
import jvm.StandardAttribute;

public abstract class AbstractCodeBuffer extends AttributeBuffer {
    
    protected final int maxlocal;
    protected final CodeLabels labels;
    
    protected AbstractCodeBuffer(ConstantPool pool, ByteBuffer bb, int maxlocal, int codesz) {
        this(pool, bb, StandardAttribute.Code.toString(), maxlocal, new CodeLabels(codesz));
    }
    
    protected AbstractCodeBuffer(ConstantPool pool, ByteBuffer bb, String name,
            int maxlocal, CodeLabels labels) {
        super(pool, bb, Context.CODE, name);
        this.maxlocal = maxlocal;
        this.labels = labels;
    }
    
    public void checkLocalVar(int var) {
        if (var >= maxlocal) {
            // "local variable %d is >= max locals %d"
            LOG(M518, var, maxlocal);
        }
    }
    
}

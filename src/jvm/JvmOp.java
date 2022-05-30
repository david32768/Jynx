package jvm;

import jynx2asm.ops.JynxOp;

public interface JvmOp extends JynxOp {

    public AsmOp getBase();
    public Feature feature();

    @Override
    default JvmVersionRange range() {
        return feature().range();
    }
    
    default boolean isImmediate() {
        Integer length = length();
        return length != null && length == 1;
    }

}

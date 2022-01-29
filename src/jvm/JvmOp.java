package jvm;

import jynx2asm.ops.JynxOp;

public interface JvmOp extends JynxOp {

    public AsmOp getBase();
    public Feature feature();

    @Override
    public Integer length();

    default Integer minLength() {
        return length();
    }
    
    default Integer maxLength() {
        return length();
    }
    
    default boolean isImmediate() {
        Integer length = length();
        return length != null && length == 1;
    }
    
}

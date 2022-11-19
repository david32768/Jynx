package jynx2asm.ops;

import jvm.Feature;
import jvm.JvmVersioned;
import jvm.JvmVersionRange;

public interface JynxOp extends JvmVersioned {

    @Override
    default JvmVersionRange range() {
        return Feature.unlimited.range();
    }
    
    default public Integer length() {
        return null; // unknown
    }
    
    default public IndentType indentType() {
        return IndentType.NONE;
    }
    
    public boolean isExternal();

}

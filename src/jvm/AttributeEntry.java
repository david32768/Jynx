package jvm;

import java.util.EnumSet;

import static jvm.ConstantPoolType.*;

public enum AttributeEntry {
    
    CONSTANT(CONSTANT_Integer, CONSTANT_Long,CONSTANT_Float,CONSTANT_Double,CONSTANT_String),
    CLASSNAME(CONSTANT_Class),
    OPT_CLASSNAME(CONSTANT_Class),
    UTF8(CONSTANT_Utf8),
    OPT_UTF8(CONSTANT_Utf8),
    OPT_NAME_TYPE(CONSTANT_NameAndType),
    PACKAGENAME(CONSTANT_Package),
    LABEL,
    ACCESS,
    USHORT,
    ;
    
    private final boolean optional;
    private final EnumSet<ConstantPoolType> cptypes;

    private AttributeEntry() {
        this.optional = name().startsWith("OPT_");
        this.cptypes = null;
    }

    private AttributeEntry(ConstantPoolType cptype1, ConstantPoolType... cptypes) {
        this.optional = name().startsWith("OPT_");
        this.cptypes = EnumSet.of(cptype1,cptypes);
    }
    
    public boolean isOptional() {
        return optional;
    }

    public EnumSet<ConstantPoolType> getCpSet() {
        return cptypes;
    }
    
}

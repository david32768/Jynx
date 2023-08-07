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
    STRING(CONSTANT_String),
    LABEL,
    LABEL_LENGTH,
    ACCESS,
    USHORT,
    LV_INDEX,
    ANNOTATION,
    TYPE_ANNOTATION,
    FRAME,
    INLINE_UTF8,
    ;
    
    private final boolean optional;
    private final EnumSet<ConstantPoolType> cptypes;

    private AttributeEntry() {
        this.optional = name().startsWith("OPT_");
        this.cptypes = EnumSet.noneOf(ConstantPoolType.class);
    }

    private AttributeEntry(ConstantPoolType cptype1, ConstantPoolType... cptypes) {
        this.optional = name().startsWith("OPT_");
        this.cptypes = EnumSet.of(cptype1,cptypes);
    }
    
    public boolean isOptional() {
        return optional;
    }

    public boolean isCP() {
        return !cptypes.isEmpty();
    }
    
    public boolean contains(ConstantPoolType cptype) {
        return cptypes.contains(cptype);
    }

    @Override
    public String toString() {
        return String.format("%s %s", name(), cptypes);
    }
    
}

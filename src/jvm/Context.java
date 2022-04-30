package jvm;

import java.util.EnumSet;

public enum Context {

    // use access flags
    CLASS,
    INNER_CLASS,
    
    FIELD,
    METHOD,
    PARAMETER,
    MODULE,
    EXPORT,
    OPEN,
    REQUIRE,
    
    INIT_METHOD,    // used in AccessFlag and Access

    // do not use access flags
    COMPONENT,

    CODE,
    CATCH,
    
    ANNOTATION,
    JVMCONSTANT,
    
    ATTRIBUTE,
    
    ;

    private final static EnumSet<Context> basic = 
            EnumSet.of(CLASS,INNER_CLASS,FIELD,METHOD,PARAMETER,MODULE,EXPORT,OPEN,REQUIRE);
    
    public boolean usesAccessFlags() {
        return basic.contains(this);
    }
}

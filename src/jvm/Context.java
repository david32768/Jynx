package jvm;

import java.util.EnumSet;

public enum Context {
    
    CLASS,
    INNER_CLASS,
    
    FIELD,
    METHOD,
    PARAMETER,
    MODULE,
    REQUIRE,
    COMPONENT,
    
    CODE,
    
    ANNOTATION,
    JVMCONSTANT,
    
    RECORD,
    ATTRIBUTE,
    
    INIT_METHOD,    // used in AccessFlag and Access
    
    ;

    private final static EnumSet<Context> basic = 
            EnumSet.of(CLASS,INNER_CLASS,FIELD,METHOD,PARAMETER,MODULE,REQUIRE,COMPONENT);
    
    public boolean isBasic() {
        return basic.contains(this);
    }
}

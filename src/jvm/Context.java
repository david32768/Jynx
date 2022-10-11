package jvm;

public enum Context {

    // use access flags
    CLASS(true),
    INNER_CLASS(true),
    
    FIELD(true),
    METHOD(true),
    PARAMETER(true),
    MODULE(true),
    EXPORT(true),
    OPEN(true),
    REQUIRE(true),
    
    INIT_METHOD,    // used in AccessFlag, Access and CheckPresent

    // do not use access flags
    COMPONENT,

    CODE,
    CATCH,
    
    ANNOTATION,
    JVMCONSTANT,
    
    ATTRIBUTE,
    
    ;

    private final boolean hasAccessFlags;

    private Context() {
        this(false);
    }
    
    private Context(boolean hasAccessFlags) {
        this.hasAccessFlags = hasAccessFlags;
    }
    
    
    public boolean usesAccessFlags() {
        return hasAccessFlags;
    }
}

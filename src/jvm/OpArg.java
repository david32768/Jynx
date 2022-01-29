package jvm;

public enum OpArg {
    
    arg_atype(2),
    arg_byte(2),
    arg_callsite(5),
    arg_class(3),
    arg_constant(2),
    arg_dir(0),    // pseudo instructions e.g. .line
    arg_field(3),
    arg_incr(3),
    arg_interface(5),
    arg_label(3),
    arg_lookupswitch(null),
    arg_marray(4),
    arg_method(3),
    arg_none(1),
    arg_short(3),
    arg_tableswitch(null),
    arg_var(2),
    ;

    private final Integer length;

    private OpArg(Integer length) {
        this.length = length;
    }

    public Integer length() {
        return length;
    }
    
    @Override
    public String toString() {
        return name().substring(4);
    }

}

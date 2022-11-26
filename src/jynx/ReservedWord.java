package jynx;

import java.util.Optional;
import java.util.stream.Stream;

public enum ReservedWord {
    res_method,
    res_signature(ReservedWordType.TOKEN,true),
    res_outer(ReservedWordType.NAME,true),
    res_innername(ReservedWordType.NAME,true),

    res_is(ReservedWordType.NAME),

    res_from(ReservedWordType.LABEL),
    res_to(ReservedWordType.LABEL),
    res_using(ReservedWordType.LABEL),

    res_stack,
    res_locals,
    res_use,
    res_reachable,
    res_label,
    
    res_default(ReservedWordType.LABEL),
    res_main(ReservedWordType.TOKEN,true),
    res_all,    // finally -> .catch all
    res_typepath(ReservedWordType.TOKEN,true),

    res_on,
    res_off,
    res_expand,
    res_lineno,
    res_with,

    res_common,
    res_subtypes,

    array_at("[@"),
    equals_sign("="),
    res_visible,
    res_invisible,
    dot_annotation(".annotation"),
    dot_annotation_array(".annotation_array"),
    right_arrow("->",ReservedWordType.LABEL),
    left_brace("{"),
    right_brace("}"),
    left_array("["),
    right_array("]"),
    dot_array(".array"),
    comma(","),
    comment(";"),
;

    private final String external_name;
    private final ReservedWordType rwtype;
    private final boolean optional;

    private ReservedWord() {
        this(null, ReservedWordType.TOKEN, false);
    }

    private ReservedWord(String external_name) {
        this(external_name,ReservedWordType.TOKEN, false);
    }

    private ReservedWord(ReservedWordType rwtype) {
        this(null, rwtype, false);
    }

    private ReservedWord(ReservedWordType rwtype, boolean optional) {
        this(null, rwtype, optional);
    }

    private ReservedWord(String external_name, ReservedWordType rwtype) {
        this(external_name, rwtype, false);
    }

    private ReservedWord(String external_name, ReservedWordType rwtype, boolean optional) {
        this.external_name = external_name == null?name().substring(4):external_name;
        this.rwtype = rwtype;
        this.optional = optional;
    }

    public boolean isOptional() {
        return optional;
    }

    public String externalName() {
        return external_name;
    }

    public ReservedWordType rwtype() {
        return rwtype;
    }

    @Override
    public String toString() {
        return external_name;
    }

    
    public static Optional<ReservedWord> getOptInstance(String str) {
        return Stream.of(values())
                .filter(res ->res.external_name.equals(str))
                .findAny();
    }
    
}

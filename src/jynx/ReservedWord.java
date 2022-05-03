package jynx;

import jynx2asm.Token;

public enum ReservedWord {
    res_method,
    res_signature('q',true),
    res_outer('n',true),
    res_innername('n',true),

    res_is('n'),

    res_from('l'),
    res_to('l'),
    res_using('l'),

    res_stack,
    res_locals,
    res_use,
    res_reachable,
    res_label,
    
    res_default,
    res_main(true),
    res_all,    // finally -> .catch all
    res_typepath(true),
    res_labels('l'),

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
    colon(":",'l'),
    left_brace("{"),
    right_brace("}"),
    left_array("["),
    right_array("]"),
    dot_array(".array"),
    comma(","),
;

    private final String external_name;
    private final char type;
    private final boolean optional;

    private ReservedWord(String external_name, char type, boolean optional) {
        this.external_name = external_name == null?name().substring(4):external_name;
        this.type = type;
        this.optional = optional;
    }

    private ReservedWord(String external_name, char type) {
        this(external_name,type,false);
    }

    
    private ReservedWord(char type,boolean optional) {
        this(null,type,optional);
    }

    private ReservedWord(char type) {
        this(null,type,false);
    }

    private ReservedWord(boolean optional) {
        this(null,' ',optional);
    }

    
    private ReservedWord(String external_name) {
        this(external_name,' ');
    }

    private ReservedWord() {
        this(null,' ',false);
    }

    public boolean isOptional() {
        return optional;
    }

    @Override
    public String toString() {
        return external_name;
    }

    
    public static ReservedWord getOptInstance(String str) {
        for (ReservedWord res:values()) {
            if (res.external_name.equals(str)) {
                return res;
            }
        }
        return null;
    }
    
    public boolean isString(String str) {
        return external_name.equals(str);
    }
    
    public String token2string(Token token) {
        switch(type) {
            case 'n':
                return token.asName();
            case 'q':
                return token.asQuoted();
            case 'l':
            default:
                return token.asString();
        }
    }

    public String stringify(String string) {
        switch(type) {
            case 'n':
                return StringUtil.escapeName(string);
            case 'q':
                return StringUtil.QuoteEscape(string);
            case 'l':
                return string;
            default:
                return StringUtil.visible(string);
        }
    }

    public boolean requiresLabel() {
        return type == 'l';
    }
    
}

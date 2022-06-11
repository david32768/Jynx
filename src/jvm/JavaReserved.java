package jvm;

import java.util.Arrays;
import java.util.Optional;

public enum JavaReserved {
    
    // Java reserved words
    ABSTRACT,
    ASSERT(Feature.assertions),
    BOOLEAN,
    BREAK,
    BYTE,
    CASE,
    CATCH,
    CHAR,
    CLASS,
    CONST,
    CONTINUE,
    DEFAULT,
    DO,
    DOUBLE,
    ELSE,
    ENUM(Feature.enums),
    EXTENDS,
    FINAL,
    FINALLY,
    FLOAT,
    FOR,
    GOTO,
    IF,
    IMPLEMENTS,
    IMPORT,
    INSTANCEOF,
    INT,
    INTERFACE,
    LONG,
    NATIVE,
    NEW,
    PACKAGE,
    PRIVATE,
    PROTECTED,
    PUBLIC,
    RETURN,
    SHORT,
    STATIC,
    STRICTFP(Feature.strictfp_rw),
    SUPER,
    SWITCH,
    SYNCHRONISED,
    THIS,
    THROW,
    THROWS,
    TRANSIENT,
    TRY,
    VOID,
    VOLATILE,
    WHILE,
    UNDERLINE(Feature.underline," "),
    
    // contextual keywords
    EXPORTS(Feature.modules,true),
    MODULE(Feature.modules,true),
    NON_SEALED(Feature.sealed,true,"non-sealed"),
    OPEN(Feature.modules,true),
    OPENS(Feature.modules,true),
    PERMITS(Feature.sealed,true),
    PROVIDES(Feature.modules,true),
    RECORD(Feature.record,true),
    REQUIRES(Feature.modules,true),
    SEALED(Feature.sealed,true),
    TO(Feature.modules,true),
    TRANSITIVE(Feature.modules,true),
    USES(Feature.modules,true),
    VAR(Feature.var_type,true),
    WITH(Feature.modules,true),
    YIELD(Feature.switch_expression,true),

    // literal values
    TRUE,
    FALSE,
    NULL,
    ;
    
    private final String word;
    private final Feature feature;
    private final boolean contextual;

    private JavaReserved() {
        this(Feature.unlimited,false,null);
    }

    private JavaReserved(Feature feature) {
        this(feature,false,null);
    }

    private JavaReserved(Feature feature,boolean contextual) {
        this(feature,contextual,null);
    }

    private JavaReserved(Feature feature, String word) {
        this(feature,false,word);
    }

    private JavaReserved(Feature feature,boolean contextual, String word) {
        this.word = word == null?name().toLowerCase():word;
        this.feature = feature;
        this.contextual = contextual;
    }

    public Feature feature() {
        return feature;
    }

    public boolean isContextual() {
        return contextual;
    }
    
    public static Optional<JavaReserved> of(String str) {
        return Arrays.stream(values())
                .filter(jr -> jr.word.equals(str))
                .findAny();
    }
}

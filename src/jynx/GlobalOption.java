package jynx;

import java.util.Optional;
import java.util.stream.Stream;

import static jynx.Message.*;

public enum GlobalOption {

    // information
    HELP(M1,'h'), // "display help message"
    VERSION(M2,'V'), //"display version information"

    SYSIN(M7), // "use SYSIN as input file"
    USE_STACK_MAP(M19), // "use user stack map instead of ASM generated"
    WARN_UNNECESSARY_LABEL(M10), // "warn if label unreferenced or alias"
    WARN_STYLE(M15), // "warn if names non-standard"
    GENERATE_LINE_NUMBERS(M9), // "generate line numbers"
    WARN_INDENT(M14), // "check indent for structured code"
    BASIC_VERIFIER(M16), // "use ASM BasicVerifier"
    SIMPLE_VERIFIER(M17), // "use ASM SimpleVerifier (default)"
    ALLOW_CLASS_FORNAME(M11), // "let simple verifier use Class.forName()"
    CHECK_METHOD_REFERENCES(M8), // "check that called methods exist (on class path)"
    PREPEND_CLASSNAME(M18), // "prepend class name to methods and fields if neccessary"
    // DEBUG options - may change
    __EXIT_IF_ERROR(M13), // "exit if error"
    __TREAT_WARNINGS_AS_ERRORS(M25), // "treat warnings as errors"
    __PRINT_STACK_TRACES(M23), // "print stack trace of exceptions"

    ;

    private final String msg;
    private final Character abbrev;

    private GlobalOption(Message msg) {
        this.msg = msg.format();
        this.abbrev = null;
    }

    private GlobalOption(Message msg, char abbrev) {
        this.msg = msg.format();
        this.abbrev = abbrev;
    }

    private final static String OPTION_PREFIX = "--";
    private final static String ABBREV_PREFIX = "-";

    public String argName() {
        return OPTION_PREFIX + name();
    }
    
    private String abbrevArgName() {
        if (abbrev == null) {
            return argName();
        }
        return ABBREV_PREFIX + abbrev;
    }
    
    public static boolean mayBeOption(String option) {
        return option.startsWith(ABBREV_PREFIX);
    }
    
    public boolean isArg(String option) {
        return argName().equalsIgnoreCase(option)
                || abbrevArgName().equals(option);
    }
    
    public static Optional<GlobalOption> optInstance(String str) {
        return Stream.of(values())
                .filter(g -> g.name().equalsIgnoreCase(str))
                .findFirst();
    }

    public static Optional<GlobalOption> optArgInstance(String str) {
        return Stream.of(values())
                .filter(g -> g.isArg(str))
                .findFirst();
    }
    
    public static void print() {
        Global.LOG(M6); // "Options are:%n"
        for (GlobalOption opt:values()) {
            if (!opt.name().startsWith("_")) {
                System.err.println(String.format(" %s %s",opt.argName(),opt.msg));
            }
        }
        System.err.println();
    }
    
}

package jynx;

import java.util.EnumSet;
import java.util.Optional;
import java.util.stream.Stream;

import static jynx.Message.*;

public enum GlobalOption {

    // information
    HELP(M1), // "display help message"
    VERSION(M2), //"display version information"

    USE_STACK_MAP(M19), // "use user stack map instead of ASM generated"
    WARN_UNNECESSARY_LABEL(M10), // "warn if label unreferenced or alias"
    WARN_STYLE(M15), // "warn if names non-standard"
    GENERATE_LINE_NUMBERS(M9), // "generate line numbers"
    WARN_INDENT(M14), // "check indent for structured code"
    BASIC_VERIFIER(M16), // "use ASM BasicVerifier"
    SIMPLE_VERIFIER(M17), // "use ASM SimpleVerifier (default)"
    // DEBUG options - may change
    __EXIT_IF_ERROR(M13), // "exit if error"
    __TREAT_WARNINGS_AS_ERRORS(M25), // "treat warnings as errors"
    __PRINT_STACK_TRACES(M23), // "print stack trace of exceptions"

    ;

    private final String msg;

    private GlobalOption(Message msg) {
        this.msg = msg.format();
    }

    final static String OPTION_PREFIX = "--";

    String argName() {
        return OPTION_PREFIX + name();
    }
    
    public boolean isInfo() {
        return this == HELP || this == VERSION;
    }

    public static Optional<GlobalOption> optInstance(String str) {
        return Stream.of(values())
                .filter(g -> g.name().equalsIgnoreCase(str))
                .findFirst();
    }

    public static Optional<GlobalOption> optArgInstance(String str) {
        return Stream.of(values())
                .filter(g->!g.isInfo())
                .filter(g -> g.argName().equalsIgnoreCase(str))
                .findFirst();
    }
    
    public static void print() {
        Global.LOG(M7); // "%nOptions are:%n"
        for (GlobalOption opt:values()) {
            if (!opt.name().startsWith("_")) {
                System.err.println(String.format(" %s %s",opt.argName(),opt.msg));
            }
        }
        System.err.println();
    }
    
}

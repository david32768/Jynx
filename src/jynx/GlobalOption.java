package jynx;

import java.util.EnumSet;
import java.util.Optional;
import java.util.stream.Stream;

import static jynx.MainOption.*;
import static jynx.Message.*;

public enum GlobalOption {

    // information
    HELP(M1, 'h'), // "display help message"
    VERSION(M2, 'V'), //"display version information"

    SYSIN(M7, ASSEMBLY), // "use SYSIN as input file"
    USE_STACK_MAP(M19, ASSEMBLY, ROUNDTRIP), // "use supplied stack map instead of ASM generated"
    WARN_UNNECESSARY_LABEL(M10, ASSEMBLY), // "warn if label unreferenced or alias"
    WARN_STYLE(M15, ASSEMBLY), // "warn if names non-standard"
    GENERATE_LINE_NUMBERS(M9, ASSEMBLY), // "generate line numbers"
    BASIC_VERIFIER(M16, ASSEMBLY, ROUNDTRIP), // "use ASM BasicVerifier instead of ASM SimpleVerifier"
    ALLOW_CLASS_FORNAME(M11, ASSEMBLY, ROUNDTRIP), // "let simple verifier use Class.forName() for non-java classes"
    CHECK_REFERENCES(M8, ASSEMBLY), // "check that called methods or used fields exist (on class path)"
    VALIDATE_ONLY(M51, ASSEMBLY), // "do not output class file"
    TRACE(M23, ASSEMBLY), // "print (ASMifier) trace"
    SYMBOLIC_LOCAL(M44, ASSEMBLY), // "local variables are symbolic not absolute integers"
    
    SKIP_CODE(M39, DISASSEMBLY), // "do not produce code"
    SKIP_DEBUG(M29, DISASSEMBLY), // "do not produce debug info"
    SKIP_FRAMES(M30, DISASSEMBLY, ROUNDTRIP), // "do not produce stack map"
    SKIP_ANNOTATIONS(M18, DISASSEMBLY), // "do not produce annotations"
    DOWN_CAST(M14, DISASSEMBLY), // "if necessary reduces JVM release to maximum supported by ASM version"
    
    DEBUG(M13, ASSEMBLY, DISASSEMBLY), // "exit with stack trace if error"
    VERBOSE(M27, ASSEMBLY, DISASSEMBLY), // "print all log messages"

    CP(M17, STRUCTURE),  // "prints constant pool at end"
    // may change
    __TREAT_WARNINGS_AS_ERRORS(M25, ASSEMBLY), // "treat warnings as errors"
    
    // internal
    __EXIT_IF_ERROR(null, ASSEMBLY), // "exit if error"
    __PRINT_STACK_TRACES(null, ASSEMBLY), // "print stack trace of exceptions"


    __STRUCTURED_LABELS(null, ASSEMBLY), // labels are numeric level
    __UNSIGNED_LONG(null, ASSEMBLY), // allow unsigned long i.e. > Long.MAX_VALUE
    __WARN_INDENT(null, ASSEMBLY), // "check indent for structured code"
    ;

    private final String msg;
    private final Character abbrev;
    private final EnumSet<MainOption> main;

    private GlobalOption(Message msg, MainOption main1, MainOption... mains) {
        this.msg = msg == null?null:msg.format();
        this.abbrev = null;
        this.main = EnumSet.of(main1, mains);
    }

    private GlobalOption(Message msg, char abbrev) {
        this.msg = msg.format();
        this.abbrev = abbrev;
        this.main = EnumSet.noneOf(MainOption.class);
    }
    
    public boolean isExternal() {
        return msg != null;
    }

    private final static String OPTION_PREFIX = "--";
    private final static String ABBREV_PREFIX = "-";

    private static boolean isEqual(String myname, String option) {
        return option.replace('-', '_').equalsIgnoreCase(myname);
    }
    
    public static boolean mayBeOption(String option) {
        return option.startsWith(ABBREV_PREFIX);
    }
    
    public boolean isArg(String option) {
        assert OPTION_PREFIX.length() > ABBREV_PREFIX.length();
        if (option.startsWith(OPTION_PREFIX)) {
            return isEqual(name(),option.substring(OPTION_PREFIX.length()));
        } else if (abbrev != null && option.startsWith(ABBREV_PREFIX)) {
            return isEqual("" + abbrev,option.substring(ABBREV_PREFIX.length()));
        }
        return false;
    }
    
    public String asArg() {
        return OPTION_PREFIX + name();
    }
    
    public boolean isRelevent(MainOption context) {
        return main.contains(context);
    }
    
    public static Optional<GlobalOption> optInstance(String str) {
        return Stream.of(values())
                .filter(GlobalOption::isExternal)
                .filter(g -> isEqual(g.name(),str))
                .findFirst();
    }

    public static Optional<GlobalOption> optArgInstance(String str) {
        return Stream.of(values())
                .filter(GlobalOption::isExternal)
                .filter(g -> g.isArg(str))
                .findFirst();
    }
    
    public static EnumSet<GlobalOption> getValidFor(MainOption main) {
        EnumSet<GlobalOption> result = EnumSet.noneOf(GlobalOption.class);
        Stream.of(values())
                .filter(opt->!opt.name().startsWith("_"))
                .filter(opt->opt.isRelevent(main))
                .forEach(result::add);
        return result;
    }
    
    public String description() {
        return String.format("%s%s %s",OPTION_PREFIX,name(),msg);
    }
    
}

package jynx;

import java.util.function.Function;
import java.util.Optional;
import java.util.stream.Stream;

import static jynx.Message.*;
import jynx2asm.NameDesc;

public enum GlobalOption {

    // information
    HELP("h", M1), // "display help message"
    VERSION("V", M2), //"display version information"

    SYSIN("", M7), // "use SYSIN as input file"
    USE_STACK_MAP(M19), // "use supplied stack map instead of ASM generated"
    WARN_UNNECESSARY_LABEL(M10), // "warn if label unreferenced or alias"
    WARN_STYLE(M15), // "warn if names non-standard"
    GENERATE_LINE_NUMBERS(M9), // "generate line numbers"
    BASIC_VERIFIER(M16), // "use ASM BasicVerifier instead of ASM SimpleVerifier"
    ALLOW_CLASS_FORNAME(M11), // "let simple verifier use Class.forName() for non-java classes"
    CHECK_REFERENCES(M8), // "check that called methods or used fields exist (on class path)"
    VALIDATE_ONLY(M51), // "do not output class file"
    TRACE(M23), // "print (ASMifier) trace"
    SYMBOLIC_LOCAL(M44), // "local variables are symbolic not absolute integers"
    USE_CLASSFILE(M74), // "use java.lang.classfile"
    VALHALLA(M601), // "Valhalla - limited support; may change"
    
    SKIP_CODE(M39), // "do not produce code"
    SKIP_DEBUG(M29), // "do not produce debug info"
    SKIP_FRAMES(M30), // "do not produce stack map"
    SKIP_ANNOTATIONS(M18), // "do not produce annotations"
    DOWN_CAST(M14), // "if necessary reduces JVM release to maximum supported by ASM version"
    GENERIC_SWITCH(M148), // "generate generic switch instruction"
    
    DEBUG(M13), // "print stack trace(s)"
    DETAIL(M17),  // "prints constant pool, instructions and other detail"
    INCREASE_MESSAGE_SEVERITY(M25), // "treat warnings as errors etc."
    SUPPRESS_WARNINGS(M27), // "suppress warnings"
    
    // internal

    __STRUCTURED_LABELS(null), // labels are numeric level
    __WARN_INDENT(null), // "check indent for structured code"
    ;

    private final String msg;
    private final String abbrev;

    
    private GlobalOption(Message msg) {
        this(null, msg);
    }

    private GlobalOption(String abbrev, Message msg) {
        this.msg = msg == null? null: msg.format();
        this.abbrev = abbrev;
        // "abbrev '%s' for option %s has invalid name"
        assert abbrev == null || abbrev.isEmpty() || NameDesc.OPTION.isValid(abbrev):M334.format(abbrev,name());
        // "option '%s' has invalid name"
        assert msg == null && name().startsWith("__") || NameDesc.OPTION.isValid(name()):M336.format(name());
    }

    public boolean isExternal() {
        return msg != null;
    }

    private static boolean unique(Function<GlobalOption,String> strfn) {
       String[] abbrevs = Stream.of(values())
                .map(strfn)
                .filter(a -> a != null)
                .map(a -> a.replace('-', '_').toLowerCase())
                .toArray(String[]::new);
       return abbrevs.length == Stream.of(abbrevs)
               .distinct()
               .count();
    }
    
    static {
        // "GlobalOption abbreviations are not unique after transform"
        assert unique(opt->opt.abbrev): M332.format();
        // "GlobalOption names are not unique after transform"
        assert unique(opt->opt.name()): M333.format();
    }
    
    private final static String OPTION_PREFIX = "--";
    private final static String ABBREV_PREFIX = "-";

    private static boolean isEqual(String myname, String option, String prefix) {
        return myname != null && option.startsWith(prefix)
                && option
                    .substring(prefix.length())
                    .replace('-', '_')
                    .equalsIgnoreCase(myname);
    }
    
    public static boolean mayBeOption(String option) {
        return option.startsWith(ABBREV_PREFIX);
    }
    
    public boolean isArg(String option) {
        return isEqual(name(), option, OPTION_PREFIX) || isEqual(abbrev, option, ABBREV_PREFIX);
    }
    
    public String asArg() {
        return OPTION_PREFIX + name();
    }
    
    public static Optional<GlobalOption> optInstance(String str) {
        return Stream.of(values())
                .filter(GlobalOption::isExternal)
                .filter(g -> isEqual(g.name(), str, ""))
                .findFirst();
    }

    public static Optional<GlobalOption> optArgInstance(String str) {
        return Stream.of(values())
                .filter(GlobalOption::isExternal)
                .filter(g -> g.isArg(str))
                .findFirst();
    }
    
    public String description() {
        return String.format("%s%s %s",OPTION_PREFIX,name(),msg);
    }
    
}

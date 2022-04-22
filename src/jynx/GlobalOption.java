package jynx;

import java.util.EnumSet;
import java.util.Optional;
import java.util.stream.Stream;

import static com.github.david32768.jynx.Main.MainOption.*;
import static jynx.Message.*;

import com.github.david32768.jynx.Main;

public enum GlobalOption {

    // information
    HELP(M1,'h'), // "display help message"
    VERSION(M2,'V'), //"display version information"

    SYSIN(M7,ASSEMBLY), // "use SYSIN as input file"
    USE_STACK_MAP(M19,ASSEMBLY), // "use user stack map instead of ASM generated"
    WARN_UNNECESSARY_LABEL(M10,ASSEMBLY), // "warn if label unreferenced or alias"
    WARN_STYLE(M15,ASSEMBLY), // "warn if names non-standard"
    GENERATE_LINE_NUMBERS(M9,ASSEMBLY), // "generate line numbers"
    WARN_INDENT(M14,ASSEMBLY), // "check indent for structured code"
    BASIC_VERIFIER(M16,ASSEMBLY), // "use ASM BasicVerifier"
    SIMPLE_VERIFIER(M17,ASSEMBLY), // "use ASM SimpleVerifier (default)"
    ALLOW_CLASS_FORNAME(M11,ASSEMBLY), // "let simple verifier use Class.forName()"
    CHECK_METHOD_REFERENCES(M8,ASSEMBLY), // "check that called methods exist (on class path)"
    PREPEND_CLASSNAME(M18,ASSEMBLY), // "prepend class name to methods and fields if neccessary"
    VALIDATE_ONLY(M51,ASSEMBLY), // "do not output class file"
    
    SKIP_CODE(M39,DISASSEMBLY), // "do not produce code"
    SKIP_DEBUG(M29,DISASSEMBLY), // "do not produce debug info"
    SKIP_FRAMES(M30,DISASSEMBLY), // "do not produce stack map"
    
    // DEBUG options - may change
    __EXIT_IF_ERROR(M13,ASSEMBLY), // "exit if error"
    __TREAT_WARNINGS_AS_ERRORS(M25,ASSEMBLY), // "treat warnings as errors"
    __PRINT_STACK_TRACES(M23,ASSEMBLY), // "print stack trace of exceptions"

    ;

    private final String msg;
    private final Character abbrev;
    private final EnumSet<Main.MainOption> main;

    private GlobalOption(Message msg, Main.MainOption main1, Main.MainOption... mains) {
        this.msg = msg.format();
        this.abbrev = null;
        this.main = EnumSet.of(main1, mains);
    }

    private GlobalOption(Message msg, char abbrev) {
        this.msg = msg.format();
        this.abbrev = abbrev;
        this.main = EnumSet.noneOf(Main.MainOption.class);
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
    
    public boolean isRelevent(Main.MainOption context) {
        return main.contains(context);
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
    
    public static void print(Main.MainOption main) {
        Global.LOG(M6); // "Options are:%n"
        Stream.of(values())
                .filter(opt->!opt.name().startsWith("_"))
                .filter(opt->opt.main.contains(main))
                .forEach(opt->System.err.println(String.format(" %s %s",opt.argName(),opt.msg)));
        System.err.println();
    }
    
}

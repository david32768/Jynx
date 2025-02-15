package jynx;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static jynx.GlobalOption.*;
import static jynx.Message.M6;

public enum MainOption {
    
    ASSEMBLY("jynx",
            " {options} %s_file",
            "produces a class file from a %s file",
            "",
            EnumSet.of(SYSIN, USE_STACK_MAP, WARN_UNNECESSARY_LABEL, WARN_STYLE, 
                    GENERATE_LINE_NUMBERS, BASIC_VERIFIER, ALLOW_CLASS_FORNAME,
                    CHECK_REFERENCES, VALIDATE_ONLY, TRACE, SYMBOLIC_LOCAL,
                    DEBUG, INCREASE_MESSAGE_SEVERITY, SUPPRESS_WARNINGS,
                    VALHALLA, GENERIC_SWITCH,
                    __STRUCTURED_LABELS, __WARN_INDENT)
    ),
    DISASSEMBLY("2jynx",
            " {options}  class-name|class_file > %s_file",
            "produces a %s file from a class",
            String.format("any %s options are added to %s directive",
                    ASSEMBLY.extname.toUpperCase(), Directive.dir_version),
            EnumSet.of(SKIP_CODE, SKIP_DEBUG, SKIP_FRAMES, SKIP_ANNOTATIONS, DOWN_CAST,
                    VALHALLA,
                    DEBUG, INCREASE_MESSAGE_SEVERITY)
    ),
    TOJYNX("tojynx",
            " {options}  class-name|class_file > %s_file",
            "produces a %s file from a class",
            String.format("any %s options are added to %s directive",
                    ASSEMBLY.extname.toUpperCase(), Directive.dir_version),
            EnumSet.of(SKIP_CODE, SKIP_DEBUG, SKIP_FRAMES, SKIP_ANNOTATIONS, DOWN_CAST,
                    VALHALLA, SKIP_STACK,
                    DEBUG, INCREASE_MESSAGE_SEVERITY)
    ),
    ROUNDTRIP("roundtrip",
            " {options}  class-name|class_file",
            String.format("checks that %s followed by %s produces an equivalent class (according to ASM Textifier)",
                    DISASSEMBLY.extname.toUpperCase(), ASSEMBLY.extname.toUpperCase()),
            "",
            EnumSet.of(USE_STACK_MAP, USE_CLASSFILE, BASIC_VERIFIER, ALLOW_CLASS_FORNAME,
                    SKIP_FRAMES, DOWN_CAST, DEBUG, SUPPRESS_WARNINGS)
    ),
    STRUCTURE("structure",
            " {options}  class-name|class_file",
            "prints a skeleton of class structure",
            "",
            EnumSet.of(DETAIL, DEBUG, VALHALLA)
    ),
    ;

    private final static int JYNX_VERSION = 0;
    private final static int JYNX_RELEASE = 23;
    private final static int JYNX_BUILD = 4;
    public final static String SUFFIX = ".jx";


    private final int version;
    private final int release;
    private final int build;
    private final String extname;
    private final String usage;
    private final String longdesc;
    private final String adddesc;
    private final EnumSet<GlobalOption> options;

    private MainOption(String extname,
            String usage, String longdesc, String adddesc, EnumSet<GlobalOption> options) {
        this.extname = extname;
        this.usage = " " + extname.toLowerCase() + String.format(usage, SUFFIX);
        this.longdesc = String.format(longdesc, SUFFIX);
        this.adddesc = String.format(adddesc, SUFFIX);
        this.options = options;
        this.version = JYNX_VERSION;
        this.release = JYNX_RELEASE;
        this.build = JYNX_BUILD;
    }

    public boolean run(Optional<String> optname) {
        var main = MainOptionService.find(this);
        return main.call(optname);
    }
    
    public String extname() {
        return extname;
    }

    public String version() {
        return String.format("%d.%d.%d", version, release, build);
    }

    public boolean usesOption(GlobalOption opt) {
        return options.contains(opt)
                || this == DISASSEMBLY && ASSEMBLY.usesOption(opt) && opt != SYSIN;
    }

    public void appUsageSummary() {
        System.err.println(usage);
        System.err.format("   (%s)%n", longdesc);
        if (adddesc.isEmpty()) {
            System.err.println();
        } else {
            System.err.format("   (%s)%n%n", adddesc);
        }
        System.err.println();
    }

    public void appUsage() {
        appUsageSummary();
        Global.LOG(M6); // "Options are:%n"
        for (GlobalOption opt:options) {
            if (opt.isExternal()) {
                System.err.println(" " + opt.description());            
            }
        }
        System.err.println();
    }

    public static Optional<MainOption> getInstance(String str) {
        return Arrays.stream(values())
                .filter(mo->mo.extname.equalsIgnoreCase(str))
                .findAny();
    }

    public static String mains() {
        return Stream.of(values())
                .map(MainOption::extname)
                .collect(Collectors.joining("|", "[", "]"));
    }
}

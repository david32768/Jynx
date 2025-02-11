package jynx;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.function.Predicate;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static jynx.Global.CLASS_NAME;
import static jynx.Global.LOG;
import static jynx.Global.OPTION;
import static jynx.GlobalOption.*;
import static jynx.Message.M116;
import static jynx.Message.M222;
import static jynx.Message.M6;
import static jynx.Message.M97;

import asm2jynx.JynxDisassemble;
import checker.Structure;
import jynx2asm.JynxClass;
import jynx2asm.JynxScanner;
import roundtrip.RoundTrip;

public enum MainOption {
    
    ASSEMBLY(MainOption::j2a,"jynx",
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
    DISASSEMBLY(MainOption::a2j,"2jynx",
            " {options}  class-name|class_file > %s_file",
            "produces a %s file from a class",
            String.format("any %s options are added to %s directive",
                    ASSEMBLY.extname.toUpperCase(), Directive.dir_version),
            EnumSet.of(SKIP_CODE, SKIP_DEBUG, SKIP_FRAMES, SKIP_ANNOTATIONS, DOWN_CAST,
                    VALHALLA,
                    DEBUG, INCREASE_MESSAGE_SEVERITY)
    ),
    TOJYNX(MainOption::tojynx,"tojynx",
            " {options}  class-name|class_file > %s_file",
            "produces a %s file from a class",
            String.format("any %s options are added to %s directive",
                    ASSEMBLY.extname.toUpperCase(), Directive.dir_version),
            EnumSet.of(SKIP_CODE, SKIP_DEBUG, SKIP_FRAMES, SKIP_ANNOTATIONS, DOWN_CAST,
                    VALHALLA, SKIP_STACK,
                    DEBUG, INCREASE_MESSAGE_SEVERITY)
    ),
    ROUNDTRIP(MainOption::a2j2a,"roundtrip",
            " {options}  class-name|class_file",
            String.format("checks that %s followed by %s produces an equivalent class (according to ASM Textifier)",
                    DISASSEMBLY.extname.toUpperCase(), ASSEMBLY.extname.toUpperCase()),
            "",
            EnumSet.of(USE_STACK_MAP, BASIC_VERIFIER, ALLOW_CLASS_FORNAME,
                    SKIP_FRAMES, DOWN_CAST, DEBUG, SUPPRESS_WARNINGS)
    ),
    STRUCTURE(MainOption::structure,"structure",
            " {options}  class-name|class_file",
            "prints a skeleton of class structure",
            "",
            EnumSet.of(DETAIL, DEBUG, VALHALLA)
    ),
    ;

    private final static int JYNX_VERSION = 0;
    private final static int JYNX_RELEASE = 23;
    private final static int JYNX_BUILD = 3;
    private final static String SUFFIX = ".jx";


    private final int version;
    private final int release;
    private final int build;
    private final Predicate<Optional<String>> fn;
    private final String extname;
    private final String usage;
    private final String longdesc;
    private final String adddesc;
    private final EnumSet<GlobalOption> options;

    private MainOption(Predicate<Optional<String>> fn, String extname,
            String usage, String longdesc, String adddesc, EnumSet<GlobalOption> options) {
        this.fn = fn;
        this.extname = extname;
        this.usage = " " + extname.toLowerCase() + String.format(usage, SUFFIX);
        this.longdesc = String.format(longdesc, SUFFIX);
        this.adddesc = String.format(adddesc, SUFFIX);
        this.options = options;
        this.version = JYNX_VERSION;
        this.release = JYNX_RELEASE;
        this.build = JYNX_BUILD;
    }

    public Predicate<Optional<String>> fn() {
        return fn;
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

    private static boolean tojynx(Optional<String> optfname) {
        throw new UnsupportedOperationException();
    }
    
    private static boolean a2j(Optional<String> optfname) {
        String fname = optfname.get();
        PrintWriter pw = new PrintWriter(System.out);
        return JynxDisassemble.a2jpw(pw,fname);
    }
    
    private static boolean j2a(Optional<String> optfname) {
        if (optfname.isPresent() == OPTION(SYSIN)) {
            LOG(M222,SYSIN); // "either option %s is specified or file name is present but not both"
            return false;
        }
        String fname = optfname.orElse("SYSIN");
        try {
            JynxScanner scanner;
            if (optfname.isPresent()) {
                if (!fname.endsWith(SUFFIX)) {
                    LOG(M97,fname,SUFFIX); // "file(%s) does not have %s suffix"
                    return false;
                }
                Path pathj = Paths.get(fname);
                scanner = JynxScanner.getInstance(pathj);
            } else {
                scanner = JynxScanner.getInstance(System.in);
            }
            return assemble(fname, scanner);
        } catch (IOException ex) {
            LOG(ex);
            return false;
        }
    }

    private static boolean assemble(String fname, JynxScanner scanner) throws IOException {
        byte[] ba = JynxClass.getBytes(fname,scanner);
        if (ba == null) {
            return false;
        }
        if (OPTION(VALIDATE_ONLY)) {
            return true;
        }
        String cname = CLASS_NAME();
        int index = cname.lastIndexOf('/');
        String cfname = cname.substring(index + 1);
        cfname += ".class";
        Path pathc = Paths.get(cfname);
        if (!OPTION(SYSIN)) {
            Path parent = Paths.get(fname).getParent();
            if (parent != null) {
                pathc = parent.resolve(pathc);
            }
        }
        Files.write(pathc, ba);
        LOG(M116,pathc,ba.length); // "%s created - size %d bytes"
        return true;
    }
    
    private static boolean a2j2a(Optional<String> optfname) {
        return RoundTrip.roundTrip(optfname);
    }

    private static boolean structure(Optional<String> optfname) {
        PrintWriter pw = new PrintWriter(System.out);
        return Structure.PrintClassStructure(optfname.get(),pw);
    }

    public static String mains() {
        return Stream.of(values())
                .map(MainOption::extname)
                .collect(Collectors.joining("|", "[", "]"));
    }
}

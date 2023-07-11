package jynx;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.function.Predicate;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static jynx.Constants.SUFFIX;
import static jynx.Global.CLASS_NAME;
import static jynx.Global.LOG;
import static jynx.Global.OPTION;
import static jynx.GlobalOption.SYSIN;
import static jynx.GlobalOption.VALIDATE_ONLY;
import static jynx.Message.M116;
import static jynx.Message.M222;
import static jynx.Message.M6;
import static jynx.Message.M97;

import asm2jynx.JynxDisassemble;
import jynx2asm.JynxClass;
import jynx2asm.JynxScanner;
import roundtrip.RoundTrip;

public enum MainOption {
    
    ASSEMBLY(MainOption::j2a,"jynx",
            " {options} " + SUFFIX + "_file",
            "produces a class file from a " + SUFFIX + " file",
            ""),
    DISASSEMBLY(MainOption::a2j,"2jynx",
            " {options}  class-name|class_file > " + SUFFIX + "_file",
            "produces a " + SUFFIX + " file from a class",
            String.format("any %s options are added to %s directive",
                    ASSEMBLY.extname.toUpperCase(), Directive.dir_version)),
    ROUNDTRIP(MainOption::a2j2a,"roundtrip",
            " {options}  class-name|class_file",
            String.format("checks that %s followed by %s produces an equivalent class (according to ASM Textifier)",
                    DISASSEMBLY.extname.toUpperCase(), ASSEMBLY.extname.toUpperCase()),
            ""),
    ;

    private final Predicate<Optional<String>> fn;
    private final String extname;
    private final String usage;
    private final String longdesc;
    private final String adddesc;

    private MainOption(Predicate<Optional<String>> fn, String extname,
            String usage, String longdesc, String adddesc) {
        this.fn = fn;
        this.extname = extname;
        this.usage = " " + extname.toLowerCase() + usage;
        this.longdesc = longdesc;
        this.adddesc = adddesc;
    }

    public Predicate<Optional<String>> fn() {
        return fn;
    }

    public String extname() {
        return extname;
    }

    public String version() {
        return String.format("Jynx %s %s",this.name(),Constants.version(OPTION(GlobalOption.DEBUG)));
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
        for (GlobalOption opt:GlobalOption.getValidFor(this)) {
            System.err.println(" " + opt.description());            
        }
        System.err.println();
    }

    public static Optional<MainOption> getInstance(String str) {
        return Arrays.stream(values())
                .filter(mo->mo.extname.equalsIgnoreCase(str))
                .findAny();
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

    public static String mains() {
        return Stream.of(values())
                .map(MainOption::extname)
                .collect(Collectors.joining("|", "[", "]"));
    }
}

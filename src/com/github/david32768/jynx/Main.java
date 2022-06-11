package com.github.david32768.jynx;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.function.Predicate;
import java.util.Optional;

import static jynx.Global.*;
import static jynx.GlobalOption.*;
import static jynx.Message.*;

import asm2jynx.JynxDisassemble;
import jvm.JvmVersion;
import jynx.GlobalOption;
import jynx2asm.JynxClass;
import jynx2asm.JynxScanner;

public class Main {
    
    private final static String SUFFIX = ".jx";
    private final static String JAVA_SUFFIX = ".java";

    private final static int JYNX_VERSION = 0;
    private final static int JYNX_RELEASE = 12;
    private final static int JYNX_BUILD = 22;
    
    private static String version() {
        return String.format("%d+%d-%d",JYNX_VERSION,JYNX_RELEASE,JYNX_BUILD);
    }

    private static void outputVersion() {
        LOG(M0,version(),JvmVersion.MAX_VERSION); // "Jynx version %s; maximum Java version is %s"
    }

    public enum MainOption {
        
        ASSEMBLY(Main::j2a,"jynx",
                " {options} " + SUFFIX + "_file",
                "produces a class file from a " + SUFFIX + " file"),
        DISASSEMBLY(Main::a2j,"2jynx",
                " {options}  class-name|class_file > " + SUFFIX + "_file",
                "produces a " + SUFFIX + " file from a class"),
        ;

        private final Predicate<Optional<String>> fn;
        private final String extname;
        private final String usage;
        private final String longdesc;

        private MainOption(Predicate<Optional<String>> fn, String extname, String usage, String longdesc) {
            this.fn = fn;
            this.extname = extname;
            this.usage = " " + extname.toLowerCase() + usage;
            this.longdesc = longdesc;
        }

        public String usage() {
            return usage;
        }

        public String extname() {
            return extname;
        }

        public String longdesc() {
            return longdesc;
        }
        
        public String version() {
            return String.format("Jynx %s %s",this.name(),Main.version());
        }
        
        private static Optional<MainOption> getInstance(String str) {
            return Arrays.stream(values())
                    .filter(mo->mo.extname.equalsIgnoreCase(str))
                    .findAny();
        }
        
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
                if (!fname.endsWith(SUFFIX) && !fname.endsWith(JAVA_SUFFIX)) {
                    LOG(M97,fname,SUFFIX); // "file(%s) does not have %s suffix"
                    return false;
                }
                Path pathj = Paths.get(fname);
                scanner = JynxScanner.getInstance(pathj,fname.endsWith(JAVA_SUFFIX));
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
    
    private static boolean a2j(Optional<String> optfname) {
        String fname = optfname.get();
        PrintWriter pw = new PrintWriter(System.out);
        return JynxDisassemble.a2jpw(pw,fname);
    }
    
    private static void appUsage() {
        LOG(M12); // "%nUsage:%n"
        for (MainOption mo:MainOption.values()) {
            System.err.println(mo.usage());
            System.err.format("   (%s)%n%n",mo.longdesc());
            GlobalOption.print(mo);
        }
    }

    private static boolean mainz(String[] args) {
        if (args.length == 0) {
            appUsage();
            return false;
        }
        String option = args[0];
        if (args.length == 1) {
            if (VERSION.isArg(option)) {
                outputVersion();
                return false;
            }
            if (HELP.isArg(option)) {
                appUsage();
                return false;
            }
        }
        Optional<MainOption> mainopt = MainOption.getInstance(option);
        if (!mainopt.isPresent()) {
            LOG(M26,option); // "invalid main-option name - %s"
            return false;
        }
        MainOption main = mainopt.get();
        args = Arrays.copyOfRange(args, 1, args.length);
        if (args.length == 0) {
            // "no args have been specified for main option %s"
            LOG(M28,main.extname());
            return false;
        }
        newGlobal(main, EnumSet.noneOf(GlobalOption.class));
        Optional<String> optname = setOptions(args);
        if (LOGGER().numErrors() != 0) {
            LOG(M3); // "program terminated because of errors"
            appUsage();
            return false;
        }
        boolean success = main.fn.test(optname);
        if (!success) {
            LOG(M298,main.name(),CLASS_NAME()); // "%s of %s failed"
        }
        return success;
    }
    
    public static void main(String[] args) {
        boolean success = mainz(args);
        if (!success) {
            System.exit(1);
        }
    }
    
}

package jynx;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import java.util.Scanner;

import static jynx.Global.*;
import static jynx.GlobalOption.*;
import static jynx.Message.*;

import jvm.JvmVersion;
import jynx2asm.JynxClass;

public class Main {
    
    public final static String SUFFIX = ".jx";
     
    private static void version() {
        LOG(M0,JvmVersion.MAX_VERSION); // "Jynx version 0.9.7; maximum Java version is %s"
    }
    
    private static void usage() {
        LOG(M5,SUFFIX); // "%nUsage: {options} %s-file"
        GlobalOption.print();
    }

    private static Scanner getScanner(String fname) throws IOException{
        Path pathj = Paths.get(fname);
        if (System.getSecurityManager() == null && (pathj.isAbsolute() || fname.contains(".."))) {
            // "%nNo security manager installed and filename is absolute or contains ..%n filename = %s"
            throw new SecurityException(M103.format(fname));
        }
        return new Scanner(pathj);
    }
    
    private static boolean j2aSysin() {
        byte[] ba = JynxClass.getBytes("SYSIN", new Scanner(System.in));
        return ba != null;
    }
    
    private static boolean j2a(String fname) {
        if (!fname.endsWith(SUFFIX)) {
            LOG(M97,fname,SUFFIX); // "file(%s) does not have %s suffix"
            return false;
        }
        Path pathc = Paths.get(fname.replace(SUFFIX,".class"));
        boolean success;
        try {
            byte[] ba = JynxClass.getBytes(fname,  getScanner(fname));
            if (ba == null) {
                return false;
            }
            String cname = CLASS_NAME();
            int index = cname.lastIndexOf('/');
            String cfname = cname.substring(index + 1);
            cfname += ".class";
            String pathcname = pathc.getFileName().toString();
            if (!cfname.equals(pathcname)) {
                LOG(M112,pathcname,cfname); // "output file(%s) is not %s"
            }
            Files.write(pathc, ba);
            LOG(M116,pathc,ba.length); // "%s created - size %d bytes"
            success = true;
        } catch (IOException ex) {
            LOG(ex);
            success = false;
        }
        return success;
    }

    public static Optional<String> setOptions(String[] args) {
        int i = 0;
        String[] remainder = new String[0];
        for (; i < args.length; ++i) {
            String argi = args[i];
            if (argi.isEmpty()) {
                continue;
            }
            if (GlobalOption.mayBeOption(argi)) {
                Optional<GlobalOption> opt = GlobalOption.optArgInstance(argi);
                if (opt.isPresent()) {
                    GlobalOption option = opt.get();
                    ADD_OPTION(option);
                } else {
                    LOG(M32,argi); // "%s is not a valid option"
                }
            } else {
                remainder = Arrays.copyOfRange(args, i, args.length);
                if (remainder.length == 1) {
                    return Optional.of(args[i]);
                }
                break;
            }
        }
        if (remainder.length == 0) {
            LOG(M218); //"SYSIN will be used as input. No file will be produced"
        } else {
            LOG(M219,Arrays.asList(remainder)); // "wrong number of parameters after options %s"
        }
        return Optional.empty();
    }

    private static boolean mainx(String[] args) {
        if (args.length == 0) {
            usage();
            return false;
        }
        if (args.length == 1) {
            String option = args[0];
            if (VERSION.isArg(option)) {
                version();
                return true;
            }
            if (HELP.isArg(option)) {
                usage();
                return true;
            }
        }
        Optional<String> optname = setOptions(args);
        if (!(optname.isPresent() ^ OPTION(SYSIN))) {
            LOG(M222,SYSIN); // "either option %s is specified or file name is present but not both"
        }
        if (LOGGER().numErrors() != 0) {
            LOG(M3); // "program terminated because of errors"
            usage();
            return false;
        }
        newGlobal("assembly",OPTIONS());
        boolean success;
        if (optname.isPresent()) {
            success = j2a(optname.get());
        } else {
            success = j2aSysin();
        }
        if (!success) {
            LOG(M298,CLASS_NAME()); // "assembly of %s failed"
        }
        return success;
    }
    
    public static void main(String[] args) {
        boolean success = mainx(args);
        if (!success) {
            System.exit(1);
        }
    }
    
}

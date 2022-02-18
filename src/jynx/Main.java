package jynx;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static jynx.Global.*;
import static jynx.GlobalOption.*;
import static jynx.Message.*;

import jvm.JvmVersion;
import jynx2asm.JynxClass;

public class Main {
    
    private final static String SUFFIX = ".jx";
     
    private static void version() {
        LOG(M0,JvmVersion.MAX_VERSION); // "Jynx version 0.9.7; maximum Java version is %s"
    }
    
    private static void usage() {
        LOG(M5,SUFFIX); // "%nUsage: {options} %s-file"
        GlobalOption.print();
    }

    private static List<String> getLines(String fname) throws IOException{
        Path pathj = Paths.get(fname);
        if (System.getSecurityManager() == null && (pathj.isAbsolute() || fname.contains(".."))) {
            // "%nNo security manager installed and filename is absolute or contains ..%n filename = %s"
            throw new SecurityException(M103.format(fname));
        }
        return Files.readAllLines(pathj);
    }
    
    private static boolean j2a(String fname) {
        boolean success = false;
        if (!fname.endsWith(SUFFIX)) {
            LOG(M97,fname,SUFFIX); // "file(%s) does not have %s suffix"
            return false;
        }
        Path pathc = Paths.get(fname.replace(SUFFIX,".class"));
        try {
            List<String> lines = getLines(fname);
            JynxClass jclass = JynxClass.getInstance(fname, lines);
            byte[] ba = jclass.toByteArray();
            if (ba != null) {
                String cname = jclass.getClassName();
                int index = cname.lastIndexOf('/');
                String cfname = cname.substring(index + 1);
                cfname += ".class";
                String pathcname = pathc.getFileName().toString();
                if (!cfname.equals(pathcname)) {
                    LOG(M112,pathcname,cfname); // "output file(%s) is not %s"
                }
                Files.write(pathc, ba);
                System.out.println(M116.format(pathc,ba.length)); // "%s created - size %d bytes"
                success = true;
            }
        } catch (IOException ex) {
            LOG(ex);
        } catch (RuntimeException rtex) {
            rtex.printStackTrace();
            LOG(M123,fname,rtex); // "compilation of %s failed because of %s"
        }
        return success;
    }

    static String[] splitNL(String str) {
        return str.split("(\r\n|\n)");
    }
    
    
    private static Optional<String> setOptions(String[] args) {
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
                if (remainder.length != 1) {
                    break;
                } else {
                    return Optional.of(args[i]);
                }
            }
        }
        LOG(M219,Arrays.asList(remainder)); // "wrong number of parameters after options %s"
        return Optional.empty();
    }

    public static int mainx(String[] args) throws IOException {
        if (args.length == 0) {
            usage();
            return 0;
        }
        if (args.length == 1) {
            String option = args[0];
            if (VERSION.isArg(option)) {
                version();
                return 0;
            }
            if (HELP.isArg(option)) {
                usage();
                return 0;
            }
        }
        Optional<String> optname = setOptions(args);
        if (!optname.isPresent() || LOGGER().numErrors() != 0) {
            LOG(M3); // "program terminated because of errors"
            usage();
            return 1;
        }
        newGlobal(OPTIONS());
        String fname = optname.get();
        boolean success = j2a(fname);
        System.err.println();
        if (!success) {
            System.out.println(M298.format(fname)); // "assembly of %s failed"
            System.out.println();
            return 1;
        }
        return 0;
    }
    
    public static void main(String[] args) throws IOException {
        int retcode = mainx(args);
        System.exit(retcode);
    }
    
}

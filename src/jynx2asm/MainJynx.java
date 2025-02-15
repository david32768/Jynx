package jynx2asm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static jynx.Global.CLASS_NAME;
import static jynx.Global.LOG;
import static jynx.Global.OPTION;
import static jynx.GlobalOption.SYSIN;
import static jynx.GlobalOption.VALIDATE_ONLY;
import static jynx.Message.M116;
import static jynx.Message.M222;
import static jynx.Message.M97;

import jynx.MainOption;
import jynx.MainOptionService;

public class MainJynx implements MainOptionService {

    @Override
    public MainOption main() {
        return MainOption.ASSEMBLY;
    }

    @Override
    public boolean call(Optional<String> optfname) {
        if (optfname.isPresent() == OPTION(SYSIN)) {
            LOG(M222,SYSIN); // "either option %s is specified or file name is present but not both"
            return false;
        }
        String fname = optfname.orElse("SYSIN");
        try {
            JynxScanner scanner;
            if (optfname.isPresent()) {
                if (!fname.endsWith(MainOption.SUFFIX)) {
                    LOG(M97, fname, MainOption.SUFFIX); // "file(%s) does not have %s suffix"
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
    
}

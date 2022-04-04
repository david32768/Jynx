package jynx;

import java.io.PrintWriter;
import java.util.Optional;

import static jynx.Global.OPTIONS;
import static jynx.Message.M299;

import asm2jynx.JynxDisassemble;

public class Disassemble {
    
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: jynx.Disassemble [options] class_file|class_name");
            System.exit(1);
        }
        Optional<String> optstr = Main.setOptions(args);
        if (!optstr.isPresent()) {
            System.err.println("Usage: jynx.Disassemble [options] class_file|class_name");
            System.exit(1);
        }
        Global.newGlobal("disassembly",OPTIONS());
        String fname = optstr.get();
        PrintWriter pw = new PrintWriter(System.out);
        boolean success = JynxDisassemble.a2jpw(pw,fname);
        System.err.println();
        if (!success) {
            System.out.println(M299.format(args[args.length - 1])); // "disassembly of %s failed"
            System.out.println();
            System.exit(1);
        }
    }
}

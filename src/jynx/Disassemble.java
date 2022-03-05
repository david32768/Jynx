package jynx;

import java.io.PrintWriter;
import java.util.EnumSet;
import java.util.Optional;

import static jynx.Message.M299;

import asm2jynx.JynxDisassemble;

public class Disassemble {
    
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: jynx.Disassemble [options] class_file|class_name");
            System.exit(1);
        }
        Global.newGlobal("disassembly",EnumSet.noneOf(GlobalOption.class));
        Optional<String> optstr = Main.setOptions(args);
        if (!optstr.isPresent()) {
            System.err.println("Usage: jynx.Disassemble [options] class_file|class_name");
            System.exit(1);
        }
        String fname = optstr.get();
        PrintWriter pw = new PrintWriter(System.out);
        boolean success = JynxDisassemble.a2jpw(pw,fname);
        System.err.println();
        if (!success) {
            System.out.println(M299.format(String.join(" ", args))); // "disassembly of %s failed"
            System.out.println();
            System.exit(1);
        }
    }
}

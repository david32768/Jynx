package roundtrip;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.EnumSet;
import java.util.Optional;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;

import static jynx.Global.ADD_OPTIONS;
import static jynx.Global.LOG;
import static jynx.Global.OPTIONS;
import static jynx.Message.M87;

import asm.JynxClassReader;
import asm2jynx.JynxDisassemble;
import jynx.ClassUtil;
import jynx.Global;
import jynx.GlobalOption;
import jynx.MainOption;
import jynx2asm.JynxClass;
import jynx2asm.JynxScanner;

public class RoundTrip {

    private static String textify(byte[] ba, int len) {
        ClassReader cr = JynxClassReader.getClassReader(ba);
        ClassNode cn = new ClassNode();
        cr.accept(cn,ClassReader.EXPAND_FRAMES);
        Printer printer = new Textifier();
        StringWriter sw = new StringWriter(len);
        try (PrintWriter pw = new PrintWriter(sw)) {
            TraceClassVisitor tcv = new TraceClassVisitor(null, printer, pw);
            cn.accept(tcv);
        }
        return sw.toString();
    }
    
    public static boolean roundTrip(Optional<String> optfname) {
        String classname = optfname.get();
        EnumSet<GlobalOption> options = OPTIONS(); 
        if (Global.OPTION(GlobalOption.SKIP_FRAMES) && Global.OPTION(GlobalOption.USE_STACK_MAP)) {
            LOG(M87,GlobalOption.SKIP_FRAMES,GlobalOption.USE_STACK_MAP); // "options %s and %s conflict"
            return false;
        }
        Global.newGlobal(MainOption.DISASSEMBLY);
        ADD_OPTIONS(options);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        boolean success = JynxDisassemble.a2jpw(pw, classname);
        if (!success) {
            System.out.format("disassembly of %s failed%n", classname);
            return false;
        }
        String result = sw.toString();
        System.err.println();
        Global.newGlobal(MainOption.ASSEMBLY);
        byte[] ba1 = JynxClass.getBytes(classname, null, JynxScanner.getInstance(result));
        if (ba1 == null) {
            System.out.format("assembly of %s failed%n", classname);
            return false;
        }
        Global.newGlobal(MainOption.DISASSEMBLY);
        ADD_OPTIONS(options);
        byte[] ba2;
        try {
            ba2 = ClassUtil.getClassBytes(classname);
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
        String s1 = textify(ba1,Short.MAX_VALUE);
        String s2 = textify(ba2,s1.length());
        success = s1.equals(s2);
        if (success) {
            System.err.format("string comparison with %s succeeded%n", classname);
        } else {
            System.err.format("string comparison with %s failed%n", classname);
        }
        return success;
    }
    
}

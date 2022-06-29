package jynx2asm.ops;

import java.io.PrintWriter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.function.UnaryOperator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import static jynx.Global.LOG;
import static jynx.Message.M176;
import static jynx.Message.M243;
import static jynx.Message.M267;

import jvm.Feature;
import jvm.JvmOp;
import jvm.JvmVersion;
import jvm.JvmVersionRange;
import jvm.Op;
import jynx.LogAssertionError;

public class JynxOps {

    private final Map<String, JynxOp> opmap;
    private final Map<String,MacroLib> macrolibs;
    private final JvmVersion jvmVersion;

    private JynxOps(JvmVersion jvmversion) {
        this.opmap = new HashMap<>(512);
        this.macrolibs = new HashMap<>();
        this.jvmVersion = jvmversion;
    }

    public static JynxOps getInstance(boolean extensions, JvmVersion jvmversion) {
        JynxOps ops =  new JynxOps(jvmversion);
        if (extensions) {
            SelectOps.streamExternal()
                    .map(op->(JynxOp)op)
                    .forEach(ops::addOp);
            ExtendedOps.streamExternal()
                    .map(op->(JynxOp)op)
                    .forEach(ops::addOp);
            JavaCallOps.streamExternal()
                    .map(op->(JynxOp)op)
                    .forEach(ops::addOp);
        }
        return ops;
    }

    private static final int MAX_SIMPLE = 16;
    
    private void addOp(JynxOp op) {
        String name = op.toString();
        JynxOp before = opmap.putIfAbsent(name, op);
        if (before != null) {
            LOG(M243, name, op.getClass(), before.getClass()); // "%s op defined in %s has already been defined in %s"
        }
        int opct = expandLevel(op).size();  // check max level (in call)
        if (opct > MAX_SIMPLE) {  // check max simple instructions
            // "%s has %d simple ops which exceeds maximum of %d"
            throw new LogAssertionError(M267, op, opct,MAX_SIMPLE);
        }
    }

    public JynxOp get(String jopstr) {
        JynxOp op =  Op.getOp(jopstr);
        if (op == null) {
            op = opmap.get(jopstr);
            if (op == null) {
                return null;
            }
        }
        jvmVersion.checkSupports(op);
        return op;
    }
    
    public MacroLib addMacroLib(String libname) {
        MacroLib result = macrolibs.get(libname);
        if (result != null) {
            return result;
        }
        ServiceLoader<MacroLib> libloader = ServiceLoader.load(MacroLib.class);
        for (MacroLib lib : libloader) {
            if (lib.name().equals(libname)) {
                lib.streamExternal()
                        .forEach(this::addOp);
                macrolibs.put(libname, lib);
                UnaryOperator<String> parmtrans = lib.parmTranslator();
                if (parmtrans != null) {
                    jynx.Global.setParmTrans(parmtrans);
                }
                result = lib;
                break;
            }
        }
        if (result == null) {
            LOG(M176,libname); // "%s not found as a macro library service"
        }
        return result;
    }

    public static Integer length(MacroOp macop) {
        List<Map.Entry<JynxOp, Integer>> oplist = expandLevel(macop);
        int sz = 0;
        for (Map.Entry<JynxOp, Integer>  me:oplist) {
            Integer oplen = me.getKey().length();
            if (oplen == null) {
                return null;
            }
            sz += oplen;
        }
        return sz;
    }

    public static JvmVersionRange range(MacroOp macop) {
        JvmVersionRange range = Feature.unlimited.range();
        for (JynxOp op : macop.getJynxOps()) {
            range = range.intersect(op.range());
        }
        return range;
    }

    private static List<Map.Entry<JynxOp, Integer>> expandLevel(JynxOp jop) {
        List<Map.Entry<JynxOp, Integer>> oplist = new ArrayList<>();
        JynxOps.expandLevel(oplist, jop, 0);
        return oplist;
    }

    private static void expandLevel(List<Map.Entry<JynxOp, Integer>> oplist, JynxOp jop, int level) {
        JvmVersionRange.checkLevel(level);
        if (jop instanceof MacroOp) {
            JynxOp[] ops = ((MacroOp) jop).getJynxOps();
            int opct = ops.length;
            if (opct > MAX_SIMPLE) {  // check max simple instructions
                // "%s has %d simple ops which exceeds maximum of %d"
                throw new LogAssertionError(M267, jop, opct,MAX_SIMPLE);
            }
            for (JynxOp op : ops) {
                JynxOps.expandLevel(oplist, op, level + 1);
            }
        } else {
            Map.Entry<JynxOp, Integer> pair = new AbstractMap.SimpleImmutableEntry<>(jop, level);
            oplist.add(pair);
        }
    }

    private static void print(PrintWriter pw, JynxOp jop) {
        print(pw, jop, 0);
    }

    private static void print(PrintWriter pw, JynxOp jop, int level) {
        JvmVersionRange.checkLevel(level);
        StringBuilder sb = new StringBuilder(2 * level);
        for (int i = 0; i < level; ++i) {
            sb.append("  ");
        }
        String spacer = sb.toString();
        String basic = jop instanceof JvmOp ? "" : " *";
        pw.format("%s%s%s%n", spacer, jop, basic);
        if (jop instanceof MacroOp) {
            for (JynxOp op : ((MacroOp) jop).getJynxOps()) {
                print(pw, op, level + 1);
            }
        }
    }

    public static void main(String[] args) {
        if (args.length == 0 || args.length == 1 && (args[0].equals("-h") || args[0].equals("--help"))) {
            System.err.println("Usage: JynxOps [macrolib]* [jynxop]");
            System.exit(1);
        }
        JynxOps ops  = getInstance(true, JvmVersion.DEFAULT_VERSION);
        int last = args.length - 1;
        for (int i = 0; i < last; ++i) {
            ops.addMacroLib(args[i]);
        }
        System.out.format("number of Jynx ops = %d%n", ops.opmap.size());
        String jopstr = args[last];
        JynxOp jop = ops.get(jopstr);
        if (jop == null) {
            System.err.format("%s is an unknown JynxOp", jopstr);
            System.exit(1);
        }
        try (PrintWriter pw = new PrintWriter(System.out)) {
            pw.format("JynxOp %s: %s, ", jop, jop.getClass());
            Integer length = jop.length();
            if (length == null) {
                pw.println(" length is variable or unknown");
            } else {
                pw.format(" length is %d%n", length);
            }
            pw.format("expansion of %s is :%n", jop);
            print(pw, jop);
        }
    }

}

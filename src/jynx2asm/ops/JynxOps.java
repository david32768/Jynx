package jynx2asm.ops;

import java.io.PrintWriter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import static jynx.Global.LOG;
import static jynx.Message.M176;
import static jynx.Message.M243;
import static jynx.Message.M267;

import com.github.david32768.jynx.MacroLib;
import jvm.Feature;
import jvm.JvmOp;
import jvm.JvmVersionRange;
import jvm.Op;
import jynx.LogAssertionError;

public class JynxOps {

    private JynxOps() {
    }

    private static final Map<String, JynxOp> OPMAP = new HashMap<>();

    private static void addOp(JynxOp op) {
        addOp(op, OPMAP);
    }

    private static final int MAX_SIMPLE = 16;
    
    private static void addOp(JynxOp op, Map<String, JynxOp> opmap) {
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

    static {
        Op.stream().forEach(JynxOps::addOp);

        AliasOps.streamExternal()
                .forEach(JynxOps::addOp);
        ExtendedOps.streamExternal()
                .forEach(JynxOps::addOp);
        JavaCallOps.streamExternal()
                .forEach(JynxOps::addOp);
    }

    public static void addMacroLib(Map<String, JynxOp> opmap, String libname) {
        ServiceLoader<MacroLib> libloader = ServiceLoader.load(MacroLib.class);
        boolean found = false;
        for (MacroLib lib : libloader) {
            if (lib.name().equals(libname)) {
                lib.streamExternal()
                        .forEach(op -> JynxOps.addOp(op, opmap));
                found = true;
                break;
            }
        }
        if (!found) {
            LOG(M176,libname); // "%s not found as a macro library service"
        }
    }

    public static Map<String, JynxOp> getOpMap() {
        return Collections.unmodifiableMap(OPMAP);
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
        System.out.format("number of Jynx ops = %d%n", OPMAP.size());
        if (args.length > 1) {
            System.err.println("Usage: JynxOps [jynxop]");
            System.exit(1);
        }
        if (args.length == 0) {
            return;
        }
        addMacroLib(OPMAP, "wasm32MVP");
        JynxOp jop = OPMAP.get(args[0]);
        if (jop == null) {
            System.err.format("%s is an unknown JynxOp", args[0]);
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

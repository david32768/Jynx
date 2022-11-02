package jynx2asm.ops;

import java.io.PrintWriter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import static jynx.Global.ADD_OPTION;
import static jynx.Global.LOG;
import static jynx.Message.M176;
import static jynx.Message.M243;
import static jynx.Message.M267;
import static jynx.Message.M314;
import static jynx.Message.M315;
import static jynx.Message.M316;
import static jynx.Message.M318;

import jvm.Feature;
import jvm.JvmVersion;
import jvm.JvmVersionRange;
import jynx.Global;
import jynx.LogAssertionError;
import jynx2asm.NameDesc;

public class JynxOps {

    private final Map<String, JynxOp> opmap;
    private final Map<String,MacroLib> macrolibs;
    private final JvmVersion jvmVersion;
    
    private Predicate<String> labelTester;
    private UnaryOperator<String> parmtrans;
    private BinaryOperator<String> ownertrans;
    

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
        Global.setOpMap(ops);
        return ops;
    }

    private static final int MAX_SIMPLE = 16;
    
    private void addOp(JynxOp op) {
        String name = op.toString();
        if (!NameDesc.OP_ID.isValid(name)) {
            // "op %s is not a valid op name"
            throw new LogAssertionError(M318, name);
        }
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
        JynxOp op =  JvmOp.getOp(jopstr);
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
                if (parmtrans == null) {
                    parmtrans = lib.parmTranslator();
                } else if (lib.parmTranslator() != null) {
                    // "only one parameter translater allowed"
                    LOG(M314);
                }
                if (ownertrans == null) {
                    ownertrans = lib.ownerTranslator();
                } else if (lib.ownerTranslator() != null) {
                    // "only one owner translater allowed"
                    LOG(M315);
                }
                if (labelTester == null) {
                    labelTester = lib.labelTester();
                } else if (lib.labelTester() != null) {
                    // "only one label tester allowed"
                    LOG(M316);
                }
                for (MacroOption opt:lib.getOptions()) {
                    ADD_OPTION(opt.option());
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

    public boolean isLabel(String labstr) {
        return labelTester != null && labelTester.test(labstr);
    }
    
    public String translateDesc(String str) {
        if (str == null || parmtrans == null || !str.startsWith("(")) {
            return str;
        } else {
            return parmtrans.apply(str);
        }
    }
    
    public String translateOwner(String classname, String str) {
        if (ownertrans == null) {
            return str;
        } else {
            return ownertrans.apply(classname,str);
        }
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

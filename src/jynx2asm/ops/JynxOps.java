package jynx2asm.ops;

import java.io.PrintWriter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.function.Predicate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import static jynx.Message.M324;
import static jynx.Message.M325;

import jvm.ConstType;
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
    private final Map<String,String> ownerTranslations;
    private final Map<String,String> parmTranslations;
    

    private JynxOps(JvmVersion jvmversion) {
        this.opmap = new HashMap<>(512);
        this.macrolibs = new HashMap<>();
        this.jvmVersion = jvmversion;
        this.ownerTranslations = new HashMap<>();
        this.parmTranslations = new HashMap<>();
    }

    public static JynxOps getInstance(JvmVersion jvmversion) {
        JynxOps ops =  new JynxOps(jvmversion);
        Global.setOpMap(ops);
        return ops;
    }

    private static final int MAX_SIMPLE = 16;
    
    private void addOp(String name, JynxOp op) {
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
                lib.getMacros().entrySet().stream()
                        .forEach(me->addOp(me.getKey(), me.getValue()));
                macrolibs.put(libname, lib);
                addTranslations(parmTranslations, lib.parmTranslations(), NameDesc.PARM_VALUE_NAME);
                addTranslations(ownerTranslations, lib.ownerTranslations(), NameDesc.OWNER_VALUE_NAME);
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

    private void addTranslations(Map<String, String> map, Map<String, String> add, NameDesc valuetype) {
        for (Map.Entry<String, String> entry : add.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (!NameDesc.KEY_NAME.isValid(key)) {
                // "translation %s -> %s is invalid; %s is not a valid key"
                LOG(M324, key, value, key);
            } else if (!valuetype.isValid(value)) {
                // "translation %s -> %s is invalid; %s is not a valid value"
                LOG(M325, key, value, value);
            } else {
                map.put(key, value);
            }
        }
    }
    
    public boolean isLabel(String labstr) {
        return labelTester != null && labelTester.test(labstr);
    }

    public String translateParm(String classname, String parm, boolean semi) {
        int last = parm.lastIndexOf('[');
        String type = parm.substring(last + 1); // OK if last == -1
        String prefix = parm.substring(0, last + 1); // OK if last == -1
        if (ConstType.isPrimitiveType(type)) {
            return parm;
        }
        int indexsemi = type.indexOf(';');
        boolean owner = type.contains("/") || indexsemi >= 0;
        if (owner) {
            if (indexsemi >= 0) {
                if (!type.startsWith("L") || indexsemi != type.length() - 1) {
                    return parm; // error
                }
                type = type.substring(1, indexsemi);
            }
            String trans = translateOwner(classname, type);
            return construct(prefix, trans, semi);
        } else {
            String trans = parmTranslations.get(type);
            if (trans == null) {
                return parm;
            }
            owner = trans.contains("/");
            if (owner) {
                type = construct(prefix, trans, semi);
            } else {
                type = prefix + trans;
            }
            //"parameter type %s changed to %s"
            LOG(M314, parm, type);
            return type;
        }
    }

    private String construct(String prefix, String type, boolean semi) {
        if (!prefix.isEmpty() || semi) { 
            return prefix + 'L' + type + ';';
        }
        return prefix + type;
    }
    
    public String translateDesc(String classname, String str) {
        int indexto = str.indexOf("->");
        if (indexto < 0) { 
            return str;
        }
        String parm = str.substring(0, indexto);
        String rtype = str.substring(indexto + 2);
        if (parm.startsWith("(") && parm.endsWith(")")) {
            parm = parm.substring(1, parm.length() - 1);
        } else {
            return str;
        }
        if (rtype.equals("()")) {
            rtype = "V";
        } else {
            rtype = translateParm(classname, rtype, true);
        }
        if (parm.isEmpty()) {
            return "()" + rtype;
        }
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        String[] parms = parm.split(",");
        for (String p:parms) {
            sb.append(translateParm(classname, p, true));
        }
        sb.append(')').append(rtype);
        return sb.toString();
    }
    
    public String translateOwner(String classname, String str) {
        if (str == null || str.equals("/")) {
            return classname;
        }
        String result = ownerTranslations.get(str);
        if (result == null) {
            return str;
        }
        // "owner %s translated to %s"
        LOG(M315, str, result);
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
        JynxOps ops  = getInstance(JvmVersion.DEFAULT_VERSION);
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

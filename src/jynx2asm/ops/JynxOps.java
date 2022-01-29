package jynx2asm.ops;

import java.io.PrintWriter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import static jynx.Global.LOG;
import static jynx.Message.M243;
import static jynx.Message.M62;

import jvm.Feature;
import jvm.JvmOp;
import jvm.JvmVersionRange;
import jvm.Op;
import jynx.LogAssertionError;

public class JynxOps  {

    private JynxOps() {}
    
    private static final Map<String,JynxOp> OPMAP = new HashMap<>();
    
    private static void addOp(JynxOp op) {
        String name = op.toString();
        JynxOp before = OPMAP.putIfAbsent(name, op);
        if (before != null) {
            LOG(M243,name,op.getClass(),before.getClass()); // "%s op defined in %s has already been defined in %s"
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
        StructuredOps.streamExternal()
                .forEach(JynxOps::addOp);

        ServiceLoader<MacroLib> libloader = ServiceLoader.load(MacroLib.class);
        for (MacroLib lib: libloader) {
            lib.streamExternal()
                    .forEach(JynxOps::addOp);
        }
    }

    public static JynxOp getInstance(String name) {
        return OPMAP.get(name);
    }

    private static final int MAXIMUM_LEVEL = 16;
    public static final String SPACES;
    
    static {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < MAXIMUM_LEVEL; ++i) {
            sb.append("  ");
        }
        SPACES = sb.toString();
    }
    
    public static Integer length(MacroOp macop) {
        return length(0,macop);
    }
    
    private static Integer length(int level, MacroOp macop) {
        checkLevel(level);
        int sz = 0;
        for (JynxOp op:macop.getJynxOps()) {
            Integer oplen;
            if (op instanceof MacroOp) {
                oplen = length(level + 1,(MacroOp)op);
            } else {
                oplen = op.length();
            }
            if (oplen == null) {
                return null;
            }
            sz += oplen;
        }
        return sz;
    }

    public static JvmVersionRange range(MacroOp macop) {
        JvmVersionRange range = Feature.unlimited.range();
        for (JynxOp op:macop.getJynxOps()) {
            range = range.intersect(op.range());
        }
        return range;
    }
    
   public static void checkLevel(int level) {
        if (level > MAXIMUM_LEVEL) {
           // "macro nest level exceeds %d"
            throw new LogAssertionError(M62,MAXIMUM_LEVEL);
        }
   }

   public static List<Map.Entry<JynxOp,Integer>> expandLevel(JynxOp jop) {
       List<Map.Entry<JynxOp,Integer>>  oplist = new ArrayList<>();
        JynxOps.expandLevel(oplist,jop,0);
       return oplist;
   }
   
   private static void  expandLevel(List<Map.Entry<JynxOp,Integer>>  oplist,JynxOp jop, int level) {
        checkLevel(level);
        if (jop instanceof MacroOp) {
            for (JynxOp op:((MacroOp)jop).getJynxOps()) {
                JynxOps.expandLevel(oplist,op,level + 1);
            }
        } else {
            Map.Entry<JynxOp,Integer> pair = new AbstractMap.SimpleImmutableEntry<>(jop,level);
            oplist.add(pair);
        }
    }
    
   private static void print(PrintWriter pw, JynxOp jop) {
       print(pw,jop,0);
   } 
   
   private static void print(PrintWriter pw,JynxOp jop, int level) {
        checkLevel(level);
        String spacer = SPACES.substring(0,2*level);
        String basic = jop instanceof JvmOp?"":" *";
        pw.format("%s%s%s%n", spacer, jop,basic);
        if (jop instanceof MacroOp) {
            for (JynxOp op:((MacroOp)jop).getJynxOps()) {
                print(pw,op,level + 1);
            }
        }
    }
    
    public static void main(String[] args) {
        System.out.format("number of Jynxx ops = %d%n",OPMAP.size());
        if (args.length == 1) {
            JynxOp jop = getInstance(args[0]);
            if (jop == null) {
                System.err.format("%s is an unknown JynxOp", args[0]);
                System.exit(1);
            }
            try (PrintWriter pw = new PrintWriter(System.out)) {
                print(pw,jop);
                pw.format("%s: %s", jop.getClass(),jop);
                Integer length = jop.length();
                if (length == null) {
                    pw.println(" length is variable or unknown");
                } else {
                    pw.format(" length is %d%n", length);
                }
                pw.println();
                for (Map.Entry<JynxOp,Integer> oplevel:expandLevel(jop)) {
                    JynxOp op = oplevel.getKey();
                    int level = oplevel.getValue();
                    String spacer = SPACES.substring(0,2*level);
                    String basic = op instanceof JvmOp?"":" *";
                    pw.format("%s%s%s%n", spacer, op,basic);
                }
            }
        }
    }

}

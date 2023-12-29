package jynx2asm.frame;

import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.tree.ParameterNode;

import static jynx.Global.LOG;
import static jynx.Message.M204;
import static jynx.Message.M211;
import static jynx.Message.M230;
import static jynx.Message.M337;

import asm.JynxVar;
import jynx.GlobalOption;
import jynx.LogIllegalArgumentException;
import jynx2asm.FrameElement;
import jynx2asm.NameDesc;
import jynx2asm.Token;

public class SymbolicVars extends LocalVars {

    private final static String THIS = "$this";
    
    private final Map<String,Integer> varmap;
    private final Map<String,FrameElement> typemap;
    private final boolean isVirtual;
    
    private int next;

    private SymbolicVars(StackMapLocals parmlocals, boolean isstatic, BitSet finalparms) {
        super(parmlocals, finalparms);
        this.varmap = new LinkedHashMap<>();
        this.typemap = new HashMap<>();
        this.isVirtual = !isstatic;
        this.next = 0;
    }
    
    public static SymbolicVars getInstance(boolean isstatic, StackMapLocals parmlocals,
            List<ParameterNode> parameters, BitSet finalparms) {
        SymbolicVars sv = new SymbolicVars(parmlocals, isstatic, finalparms);
        String[] parmnames;
        if (parameters == null) {
            parmnames = sv.defaultParmNames(parmlocals.size());
        } else {
            parmnames = parameters.stream()
                    .map(p -> p.name)
                    .toArray(String[]::new);
        }
        sv.setParms(parmlocals, parmnames);
        return sv;
    }

    private String[] defaultParmNames(int num) {
        String[] parmnames = new String[num];
        for (int i = 0; i < num; ++i) {
            String name = defaultParmName(i);
            parmnames[i] = name;
        }
        return parmnames;
    }
    
    private String defaultParmName(int num) {
        if (isVirtual && num == 0) {
            return THIS;
        } else {
            int parmnum = isVirtual? num - 1: num;
            return "$" + parmnum;
        }
    }
    
    private void setParms(StackMapLocals  smlocals, String[] parmnames) {
        assert smlocals.size() == parmnames.length;
        for (int i = 0; i < smlocals.size(); ++i) {
            FrameElement fe = smlocals.at(i);
            String name = parmnames[i];
            if (name == null) {
                name = defaultParmName(i);
            }
            newNumber(name, fe);
        }
    }

    @Override
    public int loadVarNumber(Token token) {
        String tokenstr = token.asString();
        Integer number = varmap.get(tokenstr);
        if (number == null) {
            //"unknown symbolic variable: %s"
            LOG(M211,token);
            return newNumber(tokenstr, FrameElement.ERROR);
        } else {
            return number;
        }
    }
    
    @Override
    protected int storeVarNumber(Token token, FrameElement fe) {
        String tokenstr = token.asString();
        Integer number = varmap.get(tokenstr);
        if (number == null) {
            number = newNumber(tokenstr, fe);
        } else if (typemap.get(tokenstr) != fe) {
            // "different types for %s; was %s but now %s"
            LOG(M204,token,typemap.get(tokenstr),fe);
        }
        if (isVirtual && number == 0) {
            // "attempting to overwrite %s using %s"
            LOG(M230, THIS, GlobalOption.SYMBOLIC_LOCAL);
        }
        return number;        
    }

    private int newNumber(String tokenstr, FrameElement fe) {
        if (tokenstr.equals(THIS) && fe != FrameElement.THIS) {
            // "%s is predefined"
            throw new LogIllegalArgumentException(M337, THIS);
        }
        NameDesc.SYMBOLIC_VAR.validate(tokenstr);
        int number = next;
        FrameElement shouldbenull = typemap.putIfAbsent(tokenstr,fe);
        assert shouldbenull == null;
        varmap.put(tokenstr, number);
        next += fe.slots();
        return number;        
    }

    @Override
    public void addSymbolicVars(List<JynxVar> jvars)  {
        assert jvars.isEmpty();
        for (Map.Entry<String, Integer> me : varmap.entrySet()) {
            String name = me.getKey();
            int num = me.getValue();
            FrameElement fe = typemap.get(name);
            JynxVar jvar = JynxVar.getIntance(num, name, fe.desc());
            jvars.add(jvar);
        }
    }
}

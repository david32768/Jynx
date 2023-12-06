package jynx2asm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.tree.ParameterNode;

import static jynx.Global.LOG;
import static jynx.Message.M204;
import static jynx.Message.M211;
import static jynx.Message.M230;

import jynx.GlobalOption;
import jynx2asm.frame.StackMapLocals;

public class SymbolicVars {

    private final static String THIS = "$this";
    
    private final Map<String,Integer> varmap;
    private final Map<Integer,FrameElement> typemap;
    private final boolean isVirtual;
    
    private int next;

    private SymbolicVars(boolean isstatic) {
        this.varmap = new HashMap<>();
        this.typemap = new HashMap<>();
        this.isVirtual = !isstatic;
        this.next = 0;
    }
    
    public static SymbolicVars getInstance(boolean isstatic, StackMapLocals parmosf, List<ParameterNode> parameters) {
        SymbolicVars sv = new SymbolicVars(isstatic);
        if (!isstatic) {
            sv.newNumber(THIS, FrameElement.THIS);
        }
        sv.setParms(parmosf);
        sv.setParmNames(parameters);
        return sv;
    }

    private void setParms(StackMapLocals  smlocals) {
        int parm0 = next;
        for (int i = parm0; i < smlocals.size(); ++i) {
            FrameElement fe = smlocals.at(i);
            String parmnumstr = "" + (i - parm0);
            newNumber(parmnumstr, fe);
        }
    }

    private void setParmNames(List<ParameterNode> parameters) {
        if (parameters != null) {
            int parmnum = 0;
            for (ParameterNode parameter:parameters) {
                setAlias(parmnum, parameter.name);
                ++parmnum;
            }
        }
    }
        
    private void setAlias(int num, String name) {
        Integer jvmnum = varmap.get("" + num);
        assert jvmnum != null && num >= 0;
        varmap.put(name, jvmnum);
        assert typemap.get(jvmnum) != null;
    }
    
    public int getLoadNumber(String token) {
        Integer number = varmap.get(token);
        if (number == null) {
            //"unknown symbolic variable: %s"
            LOG(M211,token);
            return newNumber(token, FrameElement.ERROR);
        } else {
            return number;
        }
    }
    
    public int getStoreNumber(String token, FrameElement fe) {
        Integer number = varmap.get(token);
        if (number == null) {
            number = newNumber(token, fe);
        } else if (typemap.get(number) != fe) {
            // "different types for %s; was %s but now %s"
            LOG(M204,token,typemap.get(number),fe);
        }
        if (isVirtual && number == 0) {
            // "attempting to overwrite %s using %s"
            LOG(M230, THIS, GlobalOption.SYMBOLIC_LOCAL);
        }
        return number;        
    }
    
    private int newNumber(String token, FrameElement fe) {
        int number = next;
        FrameElement shouldbenull = typemap.putIfAbsent(number,fe);
        assert shouldbenull == null;
        varmap.put(token, number);
        next += fe.slots();
        return number;        
    }
    
    public FrameElement getFrameElement(int num) {
        return typemap.get(num);
    }

}

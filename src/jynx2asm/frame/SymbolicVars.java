package jynx2asm.frame;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.objectweb.asm.tree.ParameterNode;

import static jynx.Global.LOG;
import static jynx.Message.M112;
import static jynx.Message.M204;
import static jynx.Message.M211;
import static jynx.Message.M230;
import static jynx.Message.M337;

import asm.JynxVar;
import jynx.GlobalOption;
import jynx.LogIllegalArgumentException;
import jynx2asm.FrameElement;
import jynx2asm.JynxLabel;
import jynx2asm.NameDesc;
import jynx2asm.Token;

public class SymbolicVars extends LocalVars {

    private final static String THIS = "$this";
    
    private final Map<String,Integer> varmap;
    private final Map<String,FrameElement> typemap;
    private final boolean isVirtual;
    
    private int next;

    private SymbolicVars(MethodParameters parameters) {
        super(parameters);
        this.varmap = new LinkedHashMap<>();
        this.typemap = new HashMap<>();
        this.isVirtual = !parameters.isStatic();
        this.next = 0;
    }
    
    public static SymbolicVars getInstance(MethodParameters parameters) {
        SymbolicVars sv = new SymbolicVars(parameters);
        ParameterNode[] parmnodes = parameters.getParameters();
        StackMapLocals smlocals = StackMapLocals.getInstance(parameters.getInitFrame());
        sv.setParms(smlocals, parmnodes);
        return sv;
    }

    private String defaultParmName(int num) {
        if (isVirtual && num == 0) {
            return THIS;
        } else {
            int parmnum = isVirtual? num - 1: num;
            return "$" + parmnum;
        }
    }
    
    private void setParms(StackMapLocals  smlocals, ParameterNode[] parmnodes) {
        assert smlocals.size() == parmnodes.length;
        for (int i = 0; i < smlocals.size(); ++i) {
            FrameElement fe = smlocals.at(i);
            ParameterNode pni = parmnodes[i];
            String name;
            if (pni == null) {
                name = defaultParmName(i);
            } else {
                name = pni.name;
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
    
    @Override
    public void visitFrame(List<Object> localarr, Optional<JynxLabel> lastLab) {
        // "stackmap locals have been ignored as %s specified"
        LOG(M112, GlobalOption.SYMBOLIC_LOCAL);
    }

}

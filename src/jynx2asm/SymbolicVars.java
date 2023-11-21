package jynx2asm;

import java.util.HashMap;
import java.util.Map;
import static jynx.Global.LOG;
import static jynx.Message.M204;
import static jynx.Message.M211;

public class SymbolicVars {
    
    private final Map<String,Integer> varmap;
    private final Map<Integer,FrameElement> typemap;

    public SymbolicVars(boolean isStatic) {
        this.varmap = new HashMap<>();
        this.typemap = new HashMap<>();
        if (!isStatic) {
            setNumber("$this", FrameElement.OBJECT, 0);
        }
    }

    public final void setNumber(String token, FrameElement fe, int num) {
        varmap.put(token, num);
        typemap.put(num,fe);
    }
    
    public void setAlias(int num, String name) {
        Integer jvmnum = varmap.get("" + num);
        assert jvmnum != null && num >= 0;
        varmap.put(name, jvmnum);
        assert typemap.get(jvmnum) != null;
    }
    
    public int getNumber(String token, FrameElement fe, int dflt) {
        Integer number = varmap.get(token);
        boolean load = fe == null;
        if (load) {
            if (number == null) {
                //"unknown variable: %s"
                LOG(M211,token);
                fe = FrameElement.ERROR;
            } else {
                return number;
            }
        }
        if (number == null) {
            number = dflt;
            varmap.put(token,dflt); // sz increased when store processed
            typemap.put(number,fe);
        } else if (typemap.get(number) != fe) {
            // "different types for %s; was %s but now %s"
            LOG(M204,token,typemap.get(number),fe);
        }
        return number;        
    }
    
    public FrameElement getFrameElement(int num) {
        return typemap.get(num);
    }

}

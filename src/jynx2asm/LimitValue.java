package jynx2asm;

import static jynx.Global.*;
import static jynx.Message.*;

public class LimitValue {
    
    private final Type type;
    private int setvalue;   // use default if line == null
    private int value;
    private Line line;

    public LimitValue(Type type) {
        this.type = type;
        this.setvalue = type.defvalue();
        this.value = 0;
    }
    
    public boolean isSet() {
        return line != null;
    }
    
    public void setLimit(int val,Line valline) {
        if (isSet()) {
            LOG(M33,type,this.line);    // "%s limit has already been set by line:%n  %s"
        } else if (this.value > val) {
            LOG(M35,this.value,this.type,val);  // "current value(%d) for %s excedes limit(%d)"
        } else {
            this.setvalue = val;
            this.line = valline;
        }
    }
    
    public void adjust(int val) {
        if (isSet() && val > this.setvalue) {
            LOG(M45,val,type,this.value,line);  // "value(%d) for %s exceeds that(%d) set by line:%n  %s"
        } else {
            this.value = Math.max(val, this.value);
        }
    }
    
    public int checkedValue() {
        if (isSet()) {
            if (value > setvalue) {
                // "value required (%d) for %s is more than limit value (%d); %d used"
                LOG(M22, value, type, setvalue, value); 
                return value;
            } else if (value < setvalue) {
                // "value required (%d) for %s is less than limit value (%d); %d used"
                LOG(M193, value, type, setvalue, setvalue);
            } 
            return setvalue;
        }
        if (value <= setvalue) {
            return setvalue;
        }
        return value;
    }
    
    public enum Type {
        locals(1),
        stack(1),
        ;
        
        private final int defvalue;

        private Type(int defvalue) {
            this.defvalue = defvalue;
        }

        public int defvalue() {
            return defvalue;
        }
        
    }
}

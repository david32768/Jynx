package jynx2asm.ops;

import java.util.Collections;
import java.util.EnumSet;
import java.util.function.Predicate;
import java.util.Map;

public abstract class MacroLib {
    
    public abstract Map<String,JynxOp> getMacros();
    public abstract String name();
    
    public EnumSet<MacroOption> getOptions() {return EnumSet.noneOf(MacroOption.class);}
    
    public Map<String,String> ownerTranslations() {
        return Collections.emptyMap();
    }
    
    public Map<String,String> parmTranslations() {
        return Collections.emptyMap();
    }
    
    public Predicate<String> labelTester() {
        return null;
    }

}

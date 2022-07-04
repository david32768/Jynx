package jynx2asm.ops;

import java.util.EnumSet;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import jynx.GlobalOption;

public abstract class MacroLib {
    
    public abstract Stream<MacroOp> streamExternal();
    public abstract String name();
    
    public EnumSet<GlobalOption> getOptions() {return EnumSet.noneOf(GlobalOption.class);}
    
    public UnaryOperator<String> parmTranslator() {
        return null;
    }
    
}

package jynx2asm.ops;

import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public abstract class MacroLib {
    
    public abstract Stream<MacroOp> streamExternal();
    public abstract String name();
    
    public UnaryOperator<String> parmTranslator() {
        return null;
    }
    
}

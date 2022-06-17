package com.github.david32768.jynx;

import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import jynx2asm.ops.MacroOp;

public abstract class MacroLib {
    
    public abstract Stream<MacroOp> streamExternal();
    public abstract String name();
    
    public UnaryOperator<String> parmTranslator() {
        return null;
    }
    
}

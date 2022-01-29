package jynx2asm.ops;

import java.util.stream.Stream;

public abstract class MacroLib {
    
    public abstract Stream<MacroOp> streamExternal();
    
}

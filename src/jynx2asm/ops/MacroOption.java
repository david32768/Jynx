package jynx2asm.ops;

import static jynx.GlobalOption.__STRUCTURED_LABELS;

import jynx.GlobalOption;

public enum MacroOption {
    
    STRUCTURED_LABELS(__STRUCTURED_LABELS),
    
    ;
    
    private final GlobalOption option;

    private MacroOption(GlobalOption option) {
        this.option = option;
    }

    public GlobalOption option() {
        return option;
    }
        
}

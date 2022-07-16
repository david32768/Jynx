package jynx2asm.ops;

import jynx.GlobalOption;

public enum MacroOption {
    
    STRUCTURED_LABELS(GlobalOption.__STRUCTURED_LABELS),
    UNSIGNED_LONG(GlobalOption.__UNSIGNED_LONG);
    ;
    
    private final GlobalOption option;

    private MacroOption(GlobalOption option) {
        this.option = option;
    }

    public GlobalOption option() {
        return option;
    }
        
}

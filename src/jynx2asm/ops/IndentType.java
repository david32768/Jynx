package jynx2asm.ops;

public enum IndentType {

    NONE(0,0),
    BEGIN(0,2),
    END(-2,0),
    ELSE(-2,2),
    ;
    
    private final int before;
    private final int after;

    private IndentType(int before, int after) {
        this.before = before;
        this.after = after;
    }

    public int after() {
        return after;
    }

    public int before() {
        return before;
    }
    
    
    
    
}

package jynx2asm;

public enum FrameClass {
    
    STACK(false),
    LOCALS(true),
    MAPLOCALS(false),
    ;
    
    private final boolean usesTwo;

    private FrameClass(boolean hasTwo) {
        this.usesTwo = hasTwo;
    }

    public int slots(FrameElement fe) {
        return fe.isTwo() && usesTwo? 2: 1;
    }
}

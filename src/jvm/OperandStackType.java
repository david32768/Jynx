package jvm;

enum OperandStackType {
    BINARY("(XX)X"),
    UNARY("(X)X"),
    CONSUME("(X)V"),
    GO("(X)V"),
    XRETURN("(X)V"),
    STORE("(X)V"),
    PRODUCE("()X"),
    COMPARE("(XX)I"),
    TRANSFORM2I("(X)I"),
    TRANSFORM2F("(X)F"),
    TRANSFORM2D("(X)D"),
    TRANSFORM2J("(X)J"),
    NOCHANGE("()V"),
    COMPARE_IF("(XX)V"),
    IF("(X)V"),
    ARRAY_STORE("(AIX)V"),
    ARRAY_LOAD("(AI)X"),
    SHIFT("(XI)X"),
    OPERAND,
    STACK,
    ARRAY_CREATE("(I)A"),
    A2X("(A)X"),
    ;

    private final String desc;

    private OperandStackType(String desc) {
        this.desc = desc;
    }

    private OperandStackType() {
        this.desc = null;
    }


    String getDesc(char x) {
        if (desc == null) {
            return null;
        }
        if ("AIJFD".indexOf(x) >= 0) {
            return desc.replace('X', x);
        } else {
            return desc.replace("X", "");
        }
    }

}

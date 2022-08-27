package jynx2asm;

import java.util.stream.Stream;

import static jynx.ReservedWord.res_expand;
import static jynx.ReservedWord.res_locals;
import static jynx.ReservedWord.res_stack;

import jynx.ReservedWord;

public enum PrintOption {

    EXPAND(res_expand),
    STACK(res_stack),
    LOCALS(res_locals),
    ;
    
    private final ReservedWord rw;

    private PrintOption(ReservedWord rw) {
        this.rw = rw;
    }
    
    public static PrintOption getInstance(Token token) {
        ReservedWord optrw = token.expectOneOf(res_expand, res_stack,res_locals);
        return Stream.of(values())
                .filter(po->po.rw == optrw)
                .findAny()
                .get();
    }
    
}

package jynx2asm;

import jynx.ReservedWord;

public interface TokenArray extends TokenDeque {

    boolean isMultiLine();
    
    public static TokenArray getInstance(JynxScanner js, Line line) {
        Token token = line.peekToken();
        boolean multiline = token.is(ReservedWord.dot_array);
        return multiline? new DotArray(js, line):new LineArray(line);
    }

}

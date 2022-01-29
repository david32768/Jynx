package jynx2asm;

import jynx.ReservedWord;

public interface TokenArray extends TokenDeque {

    boolean isMultiLine();
    
    public static TokenArray getInstance(JynxScanner js, Line line) {
        ReservedWord rw = line.peekToken().oneOf(ReservedWord.dot_array, ReservedWord.left_array);
        boolean multiline = rw == ReservedWord.dot_array;
        return multiline? new DotArray(js, line):new LineArray(line);
    }

}

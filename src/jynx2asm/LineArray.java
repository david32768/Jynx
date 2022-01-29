package jynx2asm;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

import jynx.ReservedWord;

class LineArray implements TokenArray {

    private Deque<Deque<Token>> lines;
    
    private Deque<Token> current;
    
    LineArray(Line line) {
        Objects.nonNull(line);
        line.nextToken().is(ReservedWord.left_array);
        this.lines = new ArrayDeque<>();
        readArray(line);
    }

    private void  readArray(Line line) {
        if (line.peekToken().is(ReservedWord.right_array)) {
            line.nextToken();
            return;
        }
        while (true) {
            Deque<Token> tokens = new ArrayDeque<>();
            Token token;
            while(true) {
                token = line.nextToken();
                if (token == Token.END_TOKEN || token.is(ReservedWord.left_array)  || token.is(ReservedWord.dot_array)) {
                    throw new AssertionError();
                }
                if (token.is(ReservedWord.comma) || token.is(ReservedWord.right_array)) {
                    break;
                }
                tokens.addLast(token);
            }
            if (tokens.isEmpty()) {
                    throw new AssertionError();
            }
            tokens.addLast(Token.END_TOKEN);
            lines.addLast(tokens);
            if (token.is(ReservedWord.right_array)) {
                break;
            }
        }
    }
    
    @Override
    public boolean isMultiLine() {
        return false;
    }
    
    @Override
    public Token firstToken() {
        if (current != null) {
            noMoreTokens();
        }
        if (lines.isEmpty()) {
            lines = null;
            return Token.getInstance(ReservedWord.right_array.toString());
        }
        current = lines.removeFirst();
        return current.removeFirst();
    }

    @Override
    public Deque<Token> getDeque() {
        return current;
    }
   
}

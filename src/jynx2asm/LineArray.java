package jynx2asm;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

import static jynx.Message.M276;

import jynx.LogIllegalArgumentException;
import jynx.ReservedWord;

class LineArray implements TokenArray {

    private Deque<Deque<Token>> lines;
    
    private Deque<Token> current;
    
    LineArray(Line line) {
        Objects.nonNull(line);
        this.lines = new ArrayDeque<>();
        readArray(line);
    }

    private void readArray(Line line) {
        Token token = line.nextToken();
        Token after = token.removeAtStart(ReservedWord.left_array);
        if (after == token) {
            token.mustBe(ReservedWord.left_array);
        } else {
            line.insert(after);
        }
        if (line.peekToken().is(ReservedWord.right_array)) {
            line.nextToken();
            return;
        }
        while (true) {
            Deque<Token> tokens = new ArrayDeque<>();
            while(true) {
                token = line.nextToken();
                if (token.mayBe(ReservedWord.comma,ReservedWord.right_array).isPresent()) {
                    break;
                }
                Token before = token.removeAtEnd(ReservedWord.comma);
                if (before != token) {
                    line.insert(ReservedWord.comma);
                } else {
                    before = token.removeAtEnd(ReservedWord.right_array);
                    if (before != token) {
                        line.insert(ReservedWord.right_array);
                    }
                }
                before.noneOf(ReservedWord.comma, ReservedWord.right_array, ReservedWord.left_array, ReservedWord.dot_array);
                tokens.addLast(before);
            }
            if (tokens.isEmpty()) {
                    // "empty element in  array"
                    throw new LogIllegalArgumentException(M276);
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

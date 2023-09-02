package jynx2asm;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

import static jynx.Message.M276;

import jynx.LogIllegalArgumentException;
import jynx.ReservedWord;

class LineArray implements TokenArray {

    private final Line line;
    
    private Deque<Deque<Token>> lines;
    private Deque<Token> current;
    
    LineArray(Line line) {
        Objects.nonNull(line);
        this.line = line;
        this.lines = new ArrayDeque<>();
        readArray(line);
    }

    @Override
    public Line line() {
        return line;
    }

    @Override
    public void close() {
        this.lines = null;
        this.current = null;
    }

    private void readArray(Line line) {
        Token token = line.nextTokenSplitIfStart(ReservedWord.left_array);
        token.mustBe(ReservedWord.left_array);

        while (true) {
            Deque<Token> tokens = new ArrayDeque<>();

            while(true) {
                token = line.nextTokenSplitIfEnd(ReservedWord.comma, ReservedWord.right_array);
                if (token.mayBe(ReservedWord.comma,ReservedWord.right_array).isPresent()) {
                    break;
                }
                token.noneOf(ReservedWord.left_array, ReservedWord.dot_array);
                tokens.addLast(token);
            }

            if (!tokens.isEmpty()) {
                tokens.addLast(Token.END_TOKEN);
                lines.addLast(tokens);
            } else if (token.is(ReservedWord.comma)) {
                // "empty element in  array"
                throw new LogIllegalArgumentException(M276);
            }

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
            return Token.getInstance(ReservedWord.right_array.externalName());
        }
        current = lines.removeFirst();
        return current.removeFirst();
    }

    @Override
    public Deque<Token> getDeque() {
        return current;
    }
   
}

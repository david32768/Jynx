package jynx2asm;

import java.util.Deque;
import java.util.Objects;

import static jynx.Global.LOGGER;

import jynx.Directive;
import jynx.ReservedWord;

class DotArray implements TokenArray {

    private Line line;
    private final JynxScanner js;
    
    DotArray(JynxScanner js, Line line) {
        Objects.nonNull(line);
        Objects.nonNull(js);
        line.nextToken().mustBe(ReservedWord.dot_array);
        line.noMoreTokens();
        this.js = js;
        this.line = line;
        LOGGER().pushContext();
    }

    @Override
    public Line line() {
        return line;
    }

    @Override
    public boolean isMultiLine() {
        return true;
    }
    
    @Override
    public Token firstToken() {
        Objects.nonNull(line);
        line = js.nextLineNotEnd(Directive.end_array);
        if (line == null) {
            return Token.getInstance(ReservedWord.right_array.toString());
        }
        return line.firstToken();
    }

    @Override
    public Deque<Token> getDeque() {
        Objects.nonNull(line);
        return line.getDeque();
    }

    @Override
    public void close() {
        if (line != null) {
            LOGGER().pushCurrent();
            while((line = js.nextLineNotEnd(Directive.end_array)) != null) {
                line.skipTokens();
            }
            LOGGER().popCurrent();
        }
        LOGGER().popContext();
    }
    
    
}

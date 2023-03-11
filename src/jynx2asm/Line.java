package jynx2asm;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Function;
import java.util.stream.Stream;

import static jynx.Global.LOG;
import static jynx.Message.M115;
import static jynx.Message.M198;
import static jynx.Message.M43;

import jynx.LogIllegalStateException;
import jynx.ReservedWord;
import jynx.StringUtil;

public class Line implements TokenDeque {

    enum LineType {
        DIRECTIVE,
        LABEL,
        CODE,
        ;
    }
    
    public static final Line EMPTY = new Line(null, 0, 0, new ArrayDeque<>(),LineType.CODE, null);
    private final String line;
    private final int linect;
    private final int indent;
    private final Deque<Token> tokens;
    private final LineType lineType;
    private final Function<Line,TokenArray> arrayfn;

    private final Token start;
    
    private Line(String line, int linect,  int indent, Deque<Token> tokens,
            LineType linetype, Function<Line,TokenArray> arrayfn) {
        this.line = line;
        this.linect = linect;
        this.indent = indent;
        this.tokens = tokens;
        this.lineType = linetype;
        this.start = tokens.peekFirst();
        this.arrayfn = arrayfn;
    }

    public final static char DIRECTIVE_INICATOR = '.';
    public final static char LABEL_INDICATOR = ':';
    
    public String getLine() {
        return line;
    }

    public int getLinect() {
        return linect;
    }

    public int getIndent() {
        return indent;
    }

    public boolean isDirective() {
        return lineType == LineType.DIRECTIVE;
    }

    public boolean isLabel() {
        return lineType == LineType.LABEL;
    }
    
    public TokenArray getTokenArray() {
        return arrayfn.apply(this);
    }
    
    @Override
    public Token firstToken() {
        if (tokens.isEmpty()) {
            LOG(M198); // "empty line - should not occur"
            throw new AssertionError();
        } else {
            Token token = tokens.removeFirst();
            if (token != start) {
                throw new LogIllegalStateException(M115,token); // "Not first token - token = %s"
            }
            return token;
        }
    }

    @Override
    public Deque<Token> getDeque() {
        return tokens;
    }

    private static LineType lineTypeOf(String str) {
        if (str.charAt(0) == DIRECTIVE_INICATOR) {
            return LineType.DIRECTIVE;
        }
        if (str.length() > 1 && str.indexOf(LABEL_INDICATOR) == str.length() - 1) {
            return LineType.LABEL;
        }
        return LineType.CODE;
    }
    
    public static Line tokenise(String line, int linect, Function<Line,TokenArray> arrayfn) {
        if (line.contains("\n") || line.contains("\r")) {
            LOG(line,M43); // "line contains newline or carriage return character"
            throw new AssertionError();
        }
        String str = line.trim();
        assert !str.isEmpty() && str.charAt(0) != ';';
        int indent = 0;
        while (Character.isWhitespace(line.charAt(indent))) ++indent;
        str = StringUtil.unescapeUnicode(str);
        String[] strings = StringUtil.tokenise(str);
        LineType linetype = lineTypeOf(strings[0]);
        Deque<Token> tokens = new ArrayDeque<>();
        Stream.of(strings)
                .map(Token::getInstance)
                .forEach(tokens::addLast);
        tokens.addLast(Token.END_TOKEN);
        return new Line(line,linect,indent,tokens,linetype,arrayfn);
    }

    @Override
    public String toString() {
        return String.format("%s ; %s = %d",line,ReservedWord.res_lineno,linect);
    }

}

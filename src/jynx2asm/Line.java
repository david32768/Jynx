package jynx2asm;

import java.util.ArrayDeque;
import java.util.Deque;
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
    
    public static final Line EMPTY = new Line(null, 0, 0, new ArrayDeque<>(),LineType.CODE);
    private final String line;
    private final int linect;
    private final int indent;
    private final Deque<Token> tokens;
    private final LineType lineType;

    private boolean start;
    private String comment;
    
    private Line(String line, int linect,  int indent, Deque<Token> tokens, LineType linetype) {
        this.line = line;
        this.linect = linect;
        this.indent = indent;
        this.tokens = tokens;
        this.lineType = linetype;
        this.comment = "";
        this.start = true;
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
    
    public void addComment(String comment) {
        this.comment += " ; " + comment;
    }

    @Override
    public Token firstToken() {
        if (!start) {
            throw new LogIllegalStateException(M115,tokens.getFirst()); // "Not first token - token = %s"
        }
        if (tokens.isEmpty()) {
            LOG(M198); // "empty line - should not occur"
            throw new AssertionError();
        } else {
            start = false;
            return tokens.removeFirst();
        }
    }

    @Override
    public Deque<Token> getDeque() {
        return tokens;
    }

    public static Line tokenise(String line, int linect) {
        if (line.contains("\n") || line.contains("\r")) {
            LOG(line,M43); // "line contains newline or carriage return character"
            throw new AssertionError();
        }
        String str = line.trim();
        assert !str.isEmpty() && str.charAt(0) != ';';
        int indent = 0;
        while (Character.isWhitespace(line.charAt(indent))) ++indent;
        LineType linetype = str.charAt(0) == DIRECTIVE_INICATOR?LineType.DIRECTIVE:LineType.CODE;
        str = StringUtil.unescapeUnicode(str);
        String[] strings = StringUtil.tokenise(str);
        if (linetype == LineType.CODE
                && strings[0].length() > 1
                && strings[0].indexOf(LABEL_INDICATOR) == strings[0].length() - 1) {
            linetype = LineType.LABEL;
        }
        Deque<Token> tokens = new ArrayDeque<>();
        Stream.of(strings)
                .map(Token::getInstance)
                .forEach(tokens::addLast);
        tokens.addLast(Token.END_TOKEN);
        return new Line(line,linect,indent,tokens,linetype);
    }

    @Override
    public String toString() {
        return String.format("%s ; %s = %d%s",line,ReservedWord.res_lineno,linect,comment);
    }

}

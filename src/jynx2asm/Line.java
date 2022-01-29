package jynx2asm;

import java.util.ArrayDeque;
import java.util.Deque;

import static jynx.Global.LOG;
import static jynx.Message.M115;
import static jynx.Message.M198;
import static jynx.Message.M43;
import static jynx.Message.M68;

import jynx.LogIllegalStateException;
import jynx.ReservedWord;
import jynx.StringState;

public class Line implements TokenDeque {

    enum LineType {
        DIRECTIVE,
        LABEL,
        CODE,
        ;
    }
    
    public static Line EMPTY = new Line(null, 0, 0, new ArrayDeque<>(),LineType.CODE);
    
    private final String line;
    private final int linect;
    private final int indent;
    private final Deque<Token> tokens;
    private final LineType lineType;

    private final int numTokens;
    private String comment;
    
    private Line(String line, int linect,  int indent, Deque<Token> tokens, LineType linetype) {
        this.line = line;
        this.linect = linect;
        this.indent = indent;
        this.tokens = tokens;
        this.lineType = linetype;
        this.comment = "";
        this.numTokens = tokens.size();
    }

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
        if (tokens.size() != numTokens) {
            throw new LogIllegalStateException(M115,tokens.getFirst()); // "Not first token - token = %s"
        }
        if (tokens.isEmpty()) {
            LOG(M198); // "empty line - should not occur"
            throw new AssertionError();
        } else {
            return tokens.removeFirst();
        }
    }

    @Override
    public Deque<Token> getDeque() {
        return tokens;
    }

    private static String prepare(String line) {
        // change blank in quoted strings to \n (which cannot occur in line)
        // remove comments which start with " ;"
        // reduce multiple blanks to one blank
        StringBuilder sb = new StringBuilder(line.length());
        StringState state = StringState.BLANK;
        char quote = '"';
        for (int i = 0; i < line.length(); ++i) {
            char c = line.charAt(i);
            if (c == '\t') c = ' ';
            switch (state) {
                case SLASH:
                    state = StringState.QUOTE;
                    break;
                case QUOTE:
                    switch (c) {
                        case '"': case '\'':
                            if (quote == c) {
                                state = StringState.ENDQUOTE;
                            }
                            break;
                        case ' ':
                            c = '\n';   // repcae blanks in quoted string by newline character
                            break;
                        case '\\':
                            state = StringState.SLASH;
                            break;
                    }
                    break;
                case ENDQUOTE:  // last character was closing quote
                    if (c != ' ') {
                        LOG(M68,c); // "Quoted string followed by '%c' instead of blank"
                        --i;    // reread character
                    }
                    state = StringState.BLANK;
                    break;
                case BLANK:
                    switch(c) {
                        case ' ':
                            continue;   // compress blanks to blank
                        case ';':
                            state = StringState.COMMENT;   // ignore characters
                            continue;
                        case '"': case '\'':
                            state = StringState.QUOTE;
                            quote = c;
                            break;
                        default:
                            state = StringState.UNQUOTED;
                            break;
                    }
                    break;
                case COMMENT:  // last token was blank semicolon i.e. comment start
                    continue;
                case UNQUOTED:  // not in quoted string
                    if (c == ' ') {
                        state = StringState.BLANK;
                    }
                    break;
                default:
                    throw new AssertionError();
            }
            sb.append(c);
        }
        return sb.toString();
    }
    
    public static Line tokenise(String line, int linect, boolean combine) {
        assert !line.trim().isEmpty() && " ;".indexOf(line.trim().charAt(0)) < 0;
        if (line.contains("\n") || line.contains("\r")) {
            LOG(M43); // "line contains newline or carriage return character"
            throw new AssertionError();
        }
        int indent = 0;
        while (line.charAt(indent) == ' ') ++indent;
        String str = prepare(line);
        // split line then change \n characters back to blanks
        String[] strings = str.split(" ");
        LineType linetype;
        if (strings[0].charAt(0) == '.') {
            linetype = LineType.DIRECTIVE;
        } else if (strings[0].endsWith(":")) {
            linetype = LineType.LABEL;
        } else {
            linetype = LineType.CODE;
        }
        Deque<Token> tokens = new ArrayDeque<>();
        for (int i = 0; i < strings.length; ++i) {
            String tokeni = strings[i].replace('\n', ' ');
            tokens.addLast(Token.getInstance(tokeni));
        }
        tokens.addLast(Token.END_TOKEN);
        return new Line(line,linect,indent,tokens,linetype);
    }

    @Override
    public String toString() {
        return String.format("%s ; %s = %d%s",line,ReservedWord.res_lineno,linect,comment);
    }

}

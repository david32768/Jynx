package jynx2asm.ops;

import static jynx.Message.M248;

import static jynx.Message.M407;
import static jynx.Message.M408;

import jynx.LogIllegalStateException;
import jynx2asm.LabelStack;
import jynx2asm.Line;
import jynx2asm.NameDesc;
import jynx2asm.Token;

public enum LineOps implements LineOp {

    mac_label,
    mac_label_else,
    mac_label_try,
    push_mac_label,
    lab_else,
    lab_peek_else,
    lab_peek,
    lab_peek_try,
    lab_push,
    lab_pop,
    lab_get,
    tok_skip,
    tok_swap,
    tok_print, // for debugging
    ;    
    
    private LineOps() {}


    @Override
    public void adjustLine(Line line, int macrolevel,LabelStack labelStack) {
        lineop(line, macrolevel, labelStack);
    }

    private final static String ELSE = "ELSE";
    private final static String END = "END";

    private static Token else2end(Token token) {
        String labstr = token.asString();
        if (labstr.endsWith(ELSE)) {
            labstr = labstr.substring(0, labstr.length() - ELSE.length()) + END;
        }
        return Token.getInstance(labstr);
    }
    
    private static Token removeElse(Token token) {
        String labstr = token.asString();
        if (labstr.endsWith(ELSE)) {
            labstr = labstr.substring(0, labstr.length() - ELSE.length());
        }
        return Token.getInstance(labstr);
    }
    
    public static String peekEndLabel(int index, LabelStack labelStack) {
        Token token = labelStack.peek(index);
        return else2end(token).toString();
    }
    
    private void lineop(Line line, int macrolevel,LabelStack labelStack) {
        String labstr = String.format("%cL%dML%d",NameDesc.GENERATED_LABEL_MARKER,line.getLinect(),macrolevel);
        switch(this) {
            case mac_label_else:
                Token maclabel = Token.getInstance(labstr + ELSE);
                labelStack.push(maclabel);
                line.insert(maclabel);
                break;
            case mac_label_try:
                // LIFO
                maclabel = Token.getInstance(labstr + ELSE);
                line.insert(maclabel);
                line.insert(maclabel);
                labelStack.push(maclabel);
                maclabel = Token.getInstance(labstr);
                line.insert(maclabel);
                break;
            case mac_label:
                maclabel = Token.getInstance(labstr);
                line.insert(maclabel);
                break;
            case push_mac_label:
                maclabel = Token.getInstance(labstr);
                labelStack.push(maclabel);
                break;
            case lab_else:
                Token token = labelStack.peek();
                if (!token.asString().endsWith(ELSE)) {
                    throw new LogIllegalStateException(M248); // "ELSE does not match asn IF op"
                }
                line.insert(else2end(token));
                break;
            case lab_peek_else:
                token = labelStack.peek();
                line.insert(else2end(token));
                break;
            case lab_peek_try:
                token = labelStack.peek();
                line.insert(removeElse(token));
                break;
            case lab_get:
                int index = line.nextToken().asInt();
                token = labelStack.peek(index);
                line.insert(else2end(token));
                break;
            case lab_pop:
                line.insert(labelStack.pop());
                break;
            case lab_peek:
                line.insert(labelStack.peek());
                break;
            case lab_push:
                labelStack.push(line.nextToken());
                break;
            case tok_skip:
                line.nextToken();
                break;
            case tok_print:
                System.err.format("token = %s%n", line.peekToken());
                break;
            case tok_swap:
                Token first = line.nextToken();
                Token second = line.nextToken();
                line.insert(first);
                line.insert(second);
                break;
            default:
                throw new EnumConstantNotPresentException(this.getClass(),this.name());
        }
    }

    private static enum Adjustment {
       INSERT,
       INSERT_AFTER,
       PREPEND,
       APPEND,
       REPLACE,
       CHECK,
       ;
    }
   
    public static LineOp insert(String str) {
        return new AdjustLine(Adjustment.INSERT, str);
    }
    
    public static LineOp insertAfter(String str) {
        return new AdjustLine(Adjustment.INSERT_AFTER, str);
    }
    
    public static LineOp insert(String klass, String method, String desc) {
        return new AdjustLine(Adjustment.INSERT, klass + '/' + method + desc);
    }
    
    public static LineOp prepend(String str) {
        return new AdjustLine(Adjustment.PREPEND,str);
    }
    
    public static LineOp append(String str) {
        return new AdjustLine(Adjustment.APPEND,str);
    }
    
    public static LineOp replace(String find, String replace) {
        return new AdjustLine(Adjustment.REPLACE,find,replace);
    }
    
    public static LineOp check(String str) {
        return new AdjustLine(Adjustment.CHECK,str);
    }
    
    private static class AdjustLine implements LineOp {

        private final Adjustment type;
        private final String adjust;
        private final String replace;

        public AdjustLine(Adjustment type, String adjust) {
            this.adjust = adjust;
            this.type = type;
            this.replace = null;
        }

        public AdjustLine(LineOps.Adjustment type, String adjust, String replace) {
            this.type = type;
            this.adjust = adjust;
            this.replace = replace;
            assert type == LineOps.Adjustment.REPLACE;
        }

        @Override
        public void adjustLine(Line line, int macrolevel, LabelStack labelStack){
            switch(type) {
                case INSERT:
                    Token token = Token.getInstance(adjust);
                    line.insert(token);
                    break;
                case INSERT_AFTER:
                    token = line.nextToken();
                    if (token == Token.END_TOKEN) {
                        throw new LogIllegalStateException(M407,type);  // "cannot %s end_token"
                    }
                    Token inserted = Token.getInstance(adjust);
                    line.insert(inserted);
                    line.insert(token);
                    break;
                case PREPEND:
                    token = line.nextToken().prepend(adjust);
                    line.insert(token);
                    break;
                case APPEND:
                    token = line.nextToken().append(adjust);
                    line.insert(token);
                    break;
                case REPLACE:
                    token = line.nextToken().replace(adjust, replace);
                    line.insert(token);
                    break;
                case CHECK:
                    token = line.nextToken();
                    if (!token.asString().equals(adjust)) {
                        // "expected %s but found %s"
                        throw new LogIllegalStateException(M408,adjust,token.asString());
                    }
                    break;
                default:
                    throw new EnumConstantNotPresentException(type.getClass(), type.name());
            }
        }

        @Override
        public String toString() {
            return String.format("*%s %s", type, adjust);
        }

    }

}

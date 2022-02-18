package jynx2asm.ops;

import static jynx.Message.M248;
import static jynx.Message.M900;

import jynx.LogAssertionError;
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
            case tok_swap:
                Token first = line.nextToken();
                Token second = line.nextToken();
                line.insert(first);
                line.insert(second);
                break;
            default:
                throw new LogAssertionError(M900,this); // "unknown enum constant %s in enum %s"
        }
    }

    public static LineOp insert(String str) {
        return new Insert(str);
    }
    
    public static LineOp insert(String klass, String method, String desc) {
        return new Insert(klass + '/' + method + desc);
    }
    
    private static class Insert implements LineOp {

        private final String insert;

        private Insert(String insert) {
            this.insert = insert;
        }
        
        @Override
        public void adjustLine(Line line, int macrolevel, LabelStack labelStack){
            Token token = Token.getInstance(insert);
            line.insert(token);
        }

        @Override
        public String toString() {
            return String.format("*Insert %s", insert);
        }

    }
}

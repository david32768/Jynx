package jynx2asm.ops;

import java.util.function.UnaryOperator;

import static jynx.Message.M408;

import jynx.LogIllegalStateException;
import jynx2asm.LabelStack;
import jynx2asm.Line;
import jynx2asm.NameDesc;
import jynx2asm.Token;

public enum LineOps implements LineOp {

    mac_label,

    lab_peek,
    lab_peek_if,
    lab_peek_else,
    lab_push,
    lab_push_if,
    lab_pop,
    lab_get,

    tok_skip,
    tok_swap,
    tok_dup,
    tok_print, // for debugging
    ;    
    
    private LineOps() {}

    @Override
    public void adjustLine(Line line, int macrolevel,LabelStack labelStack) {
        lineop(line, macrolevel, labelStack);
    }

    private final static String ELSE = "ELSE";

    public static String peekEndLabel(int index, LabelStack labelStack) {
        Token token = labelStack.peek(index);
        return token.asString();
    }
    
    private void lineop(Line line, int macrolevel,LabelStack labelStack) {
        Token token;
        switch(this) {
            case mac_label:
                String labstr = String.format("%cL%dML%d",NameDesc.GENERATED_LABEL_MARKER,line.getLinect(),macrolevel);
                Token maclabel  = Token.getInstance(labstr);
                line.insert(maclabel);
                break;

            case lab_get:
                int index = line.nextToken().asInt();
                token = labelStack.peek(index);
                line.insert(token);
                break;
            case lab_pop:
                line.insert(labelStack.pop());
                break;
            case lab_peek:
                line.insert(labelStack.peek());
                break;
            case lab_peek_else:
                line.insert(labelStack.peek().transform(s->s + ELSE));
                break;
            case lab_peek_if:
                line.insert(labelStack.peekIf());
                break;
            case lab_push:
                labelStack.push(line.nextToken());
                break;
            case lab_push_if:
                labelStack.pushIf(line.nextToken());
                break;

            case tok_skip:
                line.nextToken();
                break;
            case tok_print:
                System.err.format("token = %s%n", line.peekToken());
                break;
            case tok_dup:
                token = line.peekToken().checkNotEnd();
                line.insert(token);
                break;
            case tok_swap:
                Token first = line.nextToken().checkNotEnd();
                Token second = line.nextToken().checkNotEnd();
                line.insert(first);
                line.insert(second);
                break;
            default:
                throw new EnumConstantNotPresentException(this.getClass(),this.name());
        }
    }

    private static enum Adjustment {
       INSERT,
       TRANSFORM,
       CHECK,
       ;
    }
   
    public static LineOp insert(String str) {
        return new AdjustLine(Adjustment.INSERT, str);
    }
    
    public static LineOp insert(String klass, String method, String desc) {
        assert NameDesc.CLASS_NAME.validate(klass);
        assert NameDesc.METHOD_ID.validate(method);
        assert NameDesc.DESC.validate(desc);
        return new AdjustLine(Adjustment.INSERT, klass + '/' + method + desc);
    }
    
    public static LineOp prepend(String str) {
        return new AdjustLine(Adjustment.TRANSFORM,s->str + s);
    }
    
    public static LineOp append(String str) {
        return new AdjustLine(Adjustment.TRANSFORM,s->s + str);
    }
    
    public static LineOp replace(String find, String replace) {
        return new AdjustLine(Adjustment.TRANSFORM,str->str.replace(find,replace));
    }
    
    public static LineOp translateDesc() {
        return new AdjustLine(Adjustment.TRANSFORM,jynx.Global::TRANSLATE);
    }
    
    public static LineOp check(String str) {
        return new AdjustLine(Adjustment.CHECK,str);
    }
    
    private static class AdjustLine implements LineOp {

        private final Adjustment type;
        private final String adjust;
        private final UnaryOperator<String> op;

        public AdjustLine(Adjustment type, String adjust) {
            this.adjust = adjust;
            this.type = type;
            this.op = null;
            assert type != LineOps.Adjustment.TRANSFORM;
        }

        public AdjustLine(LineOps.Adjustment type, UnaryOperator<String> op) {
            this.type = type;
            this.adjust = null;
            this.op = op;
            assert type == LineOps.Adjustment.TRANSFORM;
        }

        @Override
        public void adjustLine(Line line, int macrolevel, LabelStack labelStack){
            Token token;
            switch(type) {
                case INSERT:
                    token = Token.getInstance(adjust);
                    line.insert(token);
                    break;
                case TRANSFORM:
                    token = line.nextToken().transform(op);
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

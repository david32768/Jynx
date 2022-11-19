package jynx2asm.ops;

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

    tok_skip,
    tok_skipall,
    tok_swap,
    tok_dup,
    tok_print, // for debugging
    
    ;    
    
    private LineOps() {}

    @Override
    public void adjustLine(Line line, int macrolevel, MacroOp macroop, LabelStack labelStack) {
        lineop(line, macrolevel, labelStack);
    }

    private final static String ELSE = "ELSE";

    private void lineop(Line line, int macrolevel,LabelStack labelStack) {
        Token token;
        switch(this) {
            case mac_label:
                String labstr = String.format("%cL%dML%d",NameDesc.GENERATED_LABEL_MARKER,line.getLinect(),macrolevel);
                Token maclabel  = Token.getInstance(labstr);
                line.insert(maclabel);
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
            case tok_skipall:
                line.skipTokens();
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

}

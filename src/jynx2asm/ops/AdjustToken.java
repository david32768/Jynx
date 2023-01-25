package jynx2asm.ops;

import java.util.function.UnaryOperator;
import jynx2asm.LabelStack;
import jynx2asm.Line;
import jynx2asm.NameDesc;
import jynx2asm.Token;

public class AdjustToken implements LineOp{
    
    private static enum Adjustment {
       INSERT,
       TRANSFORM,
       JOIN,
       ;
    }
   

    private final Adjustment type;
    private final String adjust;
    private final UnaryOperator<String> op;

    private AdjustToken(Adjustment type, String adjust) {
        this.adjust = adjust;
        this.type = type;
        this.op = null;
        assert type != Adjustment.TRANSFORM;
    }

    private AdjustToken(Adjustment type, UnaryOperator<String> op) {
        this.type = type;
        this.adjust = null;
        this.op = op;
        assert type == Adjustment.TRANSFORM;
    }

    @Override
    public void adjustLine(Line line, int macrolevel, MacroOp macroop, LabelStack labelStack){
        Token token;
        switch(type) {
            case INSERT:
                token = Token.getInstance(adjust);
                break;
            case JOIN:
                String first = line.nextToken().asString() + adjust;
                token = line.nextToken().transform(s->first + s);
                break;
            case TRANSFORM:
                token = line.nextToken().transform(op);
                break;
            default:
                throw new EnumConstantNotPresentException(type.getClass(), type.name());
        }
        line.insert(token);
    }

    @Override
    public String toString() {
        return String.format("*%s %s", type, adjust);
    }

    public static LineOp insert(String str) {
        return new AdjustToken(AdjustToken.Adjustment.INSERT, str);
    }
    
    public static LineOp join(String str) {
        return new AdjustToken(AdjustToken.Adjustment.JOIN, str);
    }
    
    public static LineOp insertMethod(String klass, String method, String desc) {
        assert NameDesc.CLASS_NAME.validate(klass);
        assert NameDesc.METHOD_ID.validate(method);
        assert NameDesc.DESC.validate(desc);
        return new AdjustToken(AdjustToken.Adjustment.INSERT, klass + '.' + method + desc);
    }
    
    public static LineOp prepend(String str) {
        return new AdjustToken(AdjustToken.Adjustment.TRANSFORM,s->str + s);
    }
    
    public static LineOp append(String str) {
        return new AdjustToken(AdjustToken.Adjustment.TRANSFORM,s->s + str);
    }

    public static LineOp surround(String pre, String post) {
        return new AdjustToken(AdjustToken.Adjustment.TRANSFORM,s->pre + s + post);
    }
    
    public static LineOp UC() {
        return new AdjustToken(AdjustToken.Adjustment.TRANSFORM,String::toUpperCase);
    }
    
    public static LineOp LC() {
        return new AdjustToken(AdjustToken.Adjustment.TRANSFORM,String::toLowerCase);
    }
    
    public static LineOp replace(String from, String to) {
        return new AdjustToken(AdjustToken.Adjustment.TRANSFORM,s->s.replace(from,to));
    }
    
    public static LineOp removePrefix(String prefix) {
        return new AdjustToken(AdjustToken.Adjustment.TRANSFORM,
                str->str.startsWith(prefix)?str.substring(prefix.length()):str);
    }
    
    public static LineOp translateDesc() {
        return new AdjustToken(AdjustToken.Adjustment.TRANSFORM,jynx.Global::TRANSLATE_DESC);
    }
    
}

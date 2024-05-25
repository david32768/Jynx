package jynx2asm.ops;

import static jynx.Global.LOG;
import static jynx.Message.M321;
import static jynx.Message.M408;
import static jynx.Message.M412;

import jynx.LogIllegalStateException;
import jynx.Message;
import jynx2asm.LabelStack;
import jynx2asm.Line;
import jynx2asm.Token;

public class TestToken implements LineOp {
    
    private static enum TestType {
       CHECK(M408), // "expected %s but found %s"
       CHECKNOT(M412), // "%s not supported"
       TEST_UINT_MAX(M321), // "%d does not in [0,%d]"
       ;
       
       private final Message msg;

        private TestType(Message msg) {
            this.msg = msg;
        }
       
    }
   
    private final TestType type;
    private final Object aux;

    private TestToken(TestType type, Object aux) {
        this.type = type;
        this.aux = aux;
    }

    @Override
    public void adjustLine(Line line, int macrolevel, MacroOp macroop, LabelStack labelStack){
        Token token;
        switch(type) {
            case CHECK:
                token = line.nextToken();
                if (!token.asString().equals(aux)) {
                    throw new LogIllegalStateException(type.msg,aux,token.asString());
                }
                break;
            case CHECKNOT:
                token = line.peekToken();
                if (!token.isEndToken() && token.asString().equals(aux)) {
                    throw new LogIllegalStateException(type.msg,aux);
                }
                break;
            case TEST_UINT_MAX:
                token = line.peekToken();
                long uint = token.asUnsignedInt();
                if (uint > (Long)aux) {
                    LOG(type.msg,uint,aux);
                }
                break;
            default:
                throw new EnumConstantNotPresentException(type.getClass(), type.name());
        }
    }

    @Override
    public String toString() {
        return String.format("*%s %s", type, aux);
    }

    public static LineOp check(String str) {
        return new TestToken(TestToken.TestType.CHECK,str);
    }

    public static LineOp checkNot(String str) {
        return new TestToken(TestToken.TestType.CHECKNOT,str);
    }

    public static LineOp testUIntMax(long max) {
        return new TestToken(TestType.TEST_UINT_MAX, max);
    }

}

package jynx2asm.ops;

import static jynx.Global.LOG;
import static jynx.Message.M320;
import static jynx.Message.M322;

import jynx.Message;
import jynx2asm.LabelStack;
import jynx2asm.Line;

public class MessageOp implements LineOp {
    
    private static enum MessageType {
       IGNORE(M320), // "occurences of %s have been ignored: %s"
       UNSUPPORTED(M322), // "use of %s is not supported: %s"
       ;
       
       private final Message msg;

        private MessageType(Message msg) {
            this.msg = msg;
        }
       
    }
   
    private final MessageType type;
    private final String aux;

    private MessageOp(MessageType type, String aux) {
        this.type = type;
        this.aux = aux;
    }

    @Override
    public void adjustLine(Line line, int macrolevel, MacroOp macroop, LabelStack labelStack){
        LOG(type.msg,macroop,aux);
    }

    @Override
    public String toString() {
        return String.format("*%s %s", type, aux);
    }

    public static LineOp ignoreMacro(String msg) {
        return new MessageOp(MessageOp.MessageType.IGNORE,msg);
    }
    
    public static LineOp unsupportedMacro(String msg) {
        return new MessageOp(MessageOp.MessageType.UNSUPPORTED,msg);
    }
    
}

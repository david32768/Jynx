package asm.instruction;

import org.objectweb.asm.MethodVisitor;

import static jynx.Message.M908;

import jynx.LogAssertionError;
import jynx2asm.handles.FieldHandle;
import jynx2asm.ops.JvmOp;
import jynx2asm.StackLocals;

public class FieldInstruction extends Instruction {

    private final FieldHandle fh;

    public FieldInstruction(JvmOp jop, FieldHandle fh) {
        super(jop);
        this.fh = fh;
    }

    @Override
    public void accept(MethodVisitor mv) {
        mv.visitFieldInsn(jvmop.asmOpcode(),fh.owner(), fh.name(), fh.desc());
    }

    @Override
    public void adjust(StackLocals stackLocals) {
        String desc = fh.desc();
        String stackdesc;
        switch (jvmop) {
            case asm_getfield:
                stackdesc = String.format("(L%s;)%s",fh.owner(),desc);
                break;
            case asm_getstatic:
                stackdesc = "()" + desc;
                break;
            case asm_putfield:
                stackdesc = String.format("(L%s;%s)V",fh.owner(),desc);
                break;
            case asm_putstatic:
                stackdesc = "(" + desc + ")V";
                break;
            default:
                // "unexpected Op %s in this instruction"),
                throw new LogAssertionError(M908,jvmop.name());
        }
        stackLocals.adjustStackOperand(stackdesc);
    }

    @Override
    public String toString() {
        return String.format("%s %s",jvmop,fh.ond());
    }

}

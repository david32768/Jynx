package asm.instruction;

import org.objectweb.asm.MethodVisitor;

import static jynx.Message.M908;

import jvm.JvmOp;
import jynx.LogAssertionError;
import jynx2asm.FieldDesc;
import jynx2asm.StackLocals;

public class FieldInstruction extends Instruction {

    private final FieldDesc fd;

    public FieldInstruction(JvmOp jop, FieldDesc fd) {
        super(jop);
        this.fd = fd;
    }

    @Override
    public void accept(MethodVisitor mv) {
        mv.visitFieldInsn(base.opcode(),fd.getOwner(), fd.getName(), fd.getDesc());
    }

    @Override
    public void adjust(StackLocals stackLocals) {
        String desc = fd.getDesc();
        String stackdesc;
        switch (base) {
            case asm_getfield:
                stackdesc = String.format("(L%s;)%s",fd.getOwner(),desc);
                break;
            case asm_getstatic:
                stackdesc = "()" + desc;
                break;
            case asm_putfield:
                stackdesc = String.format("(L%s;%s)V",fd.getOwner(),desc);
                break;
            case asm_putstatic:
                stackdesc = "(" + desc + ")V";
                break;
            default:
                // "unexpected Op %s in this instruction"),
                throw new LogAssertionError(M908,base.name());
        }
        stackLocals.adjustStackOperand(stackdesc);
    }

    @Override
    public String toString() {
        return String.format("%s %s",base,fd.toJynx());
    }

}

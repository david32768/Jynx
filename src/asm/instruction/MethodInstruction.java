package asm.instruction;

import org.objectweb.asm.MethodVisitor;

import static jynx.Message.M908;

import jynx.LogAssertionError;
import jynx2asm.MethodDesc;
import jynx2asm.ops.JvmOp;
import jynx2asm.StackLocals;

public class MethodInstruction extends Instruction {

    private final MethodDesc md;

    public MethodInstruction(JvmOp jop, MethodDesc md) {
        super(jop);
        this.md = md;
    }

    @Override
    public void accept(MethodVisitor mv) {
        mv.visitMethodInsn(jvmop.opcode(),md.getOwner(), md.getName(), md.getDesc(), md.isOwnerInterface());
    }

    @Override
    public void adjust(StackLocals stackLocals) {
        String owner = md.getOwner();
        owner = owner.charAt(0) == '['?owner:"L" + owner + ";";
        String desc = md.getDesc();
        String stackdesc;
        switch (jvmop) {
            case asm_invokestatic:
                stackdesc = desc;
                break;
            case asm_invokespecial:
                stackdesc = String.format("(%s%s",owner,desc.substring(1));
                break;
            case asm_invokeinterface:
            case asm_invokevirtual:
                stackdesc = String.format("(%s%s",owner,desc.substring(1));
                break;
            default:
                // "unexpected Op %s in this instruction"),
                throw new LogAssertionError(M908,jvmop.name());
        }
        stackLocals.adjustStackOperand(stackdesc);
    }

    @Override
    public String toString() {
        return String.format("%s %s",jvmop,md.toJynx());
    }

}

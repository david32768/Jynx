package asm.instruction;

import org.objectweb.asm.MethodVisitor;

import static jynx.Message.M908;

import jynx.LogAssertionError;
import jynx2asm.handles.MethodHandle;
import jynx2asm.ops.JvmOp;
import jynx2asm.StackLocals;

public class MethodInstruction extends Instruction {

    private final MethodHandle mh;

    public MethodInstruction(JvmOp jop, MethodHandle mh) {
        super(jop);
        this.mh = mh;
    }

    @Override
    public void accept(MethodVisitor mv) {
        mv.visitMethodInsn(jvmop.opcode(),mh.owner(), mh.name(), mh.desc(), mh.isInterface());
    }

    @Override
    public void adjust(StackLocals stackLocals) {
        String owner = mh.owner();
        owner = owner.charAt(0) == '['?owner:"L" + owner + ";";
        String desc = mh.desc();
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
        return String.format("%s %s",jvmop,mh.iond());
    }

    @Override
    public boolean needLineNumber() {
        return true;
    }

}

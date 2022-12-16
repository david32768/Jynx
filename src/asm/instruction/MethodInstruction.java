package asm.instruction;

import org.objectweb.asm.MethodVisitor;

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
        stackLocals.adjustStackOperand(jvmop,mh);
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

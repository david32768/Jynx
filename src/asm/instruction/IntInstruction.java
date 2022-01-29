package asm.instruction;

import org.objectweb.asm.MethodVisitor;

import jvm.JvmOp;

public class IntInstruction extends Instruction {

    private final int value;

    public IntInstruction(JvmOp jop, int value) {
        super(jop);
        this.value = value;
    }

    @Override
    public void accept(MethodVisitor mv) {
        mv.visitIntInsn(base.opcode(),value);
    }

    @Override
    public String toString() {
        return String.format("%s %d",base,value);
    }

}

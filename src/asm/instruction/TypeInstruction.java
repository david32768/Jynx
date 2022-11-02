package asm.instruction;

import org.objectweb.asm.MethodVisitor;

import jynx2asm.ops.JvmOp;

public class TypeInstruction extends Instruction {

    private final String type;

    public TypeInstruction(JvmOp jop, String type) {
        super(jop);
        this.type = type;
    }

    @Override
    public void accept(MethodVisitor mv) {
        mv.visitTypeInsn(jvmop.asmOpcode(), type);
    }

    @Override
    public String toString() {
        return String.format("%s %s",jvmop,type);
    }

    @Override
    public boolean needLineNumber() {
        return true;
    }

}

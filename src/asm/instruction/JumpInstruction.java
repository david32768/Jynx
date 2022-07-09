package asm.instruction;

import org.objectweb.asm.MethodVisitor;

import jynx2asm.JynxLabel;
import jynx2asm.ops.JvmOp;
import jynx2asm.StackLocals;

public class JumpInstruction extends Instruction {

    private final JynxLabel jlab;

    public JumpInstruction(JvmOp jop, JynxLabel jlab) {
        super(jop);
        this.jlab = jlab;
    }

    @Override
    public void accept(MethodVisitor mv) {
        mv.visitJumpInsn(jvmop.asmOpcode(), jlab.asmlabel());
    }

    @Override
    public void adjust(StackLocals stackLocals) {
        super.adjust(stackLocals);
        boolean jsr = jvmop == JvmOp.asm_jsr;
        stackLocals.adjustLabel(jlab, jsr);
    }

    @Override
    public String toString() {
        return String.format("%s %s",jvmop,jlab.name());
    }

}

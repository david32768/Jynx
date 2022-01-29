package asm.instruction;

import org.objectweb.asm.MethodVisitor;

import jvm.AsmOp;
import jvm.JvmOp;
import jynx2asm.JynxLabel;
import jynx2asm.StackLocals;

public class JumpInstruction extends Instruction {

    private final JynxLabel jlab;

    public JumpInstruction(JvmOp jop, JynxLabel jlab) {
        super(jop);
        this.jlab = jlab;
    }

    @Override
    public void accept(MethodVisitor mv) {
        mv.visitJumpInsn(base.opcode(), jlab.asmlabel());
    }

    @Override
    public void adjust(StackLocals stackLocals) {
        super.adjust(stackLocals);
        boolean jsr = base == AsmOp.asm_jsr;
        stackLocals.adjustLabel(jlab, jsr);
    }

    @Override
    public String toString() {
        return String.format("%s %s",base,jlab.name());
    }

}

package asm.instruction;

import org.objectweb.asm.MethodVisitor;

import jynx2asm.JynxLabel;
import jynx2asm.ops.JvmOp;
import jynx2asm.StackLocals;

public class LabelInstruction extends Instruction {

    private final JynxLabel jlab;

    public LabelInstruction(JvmOp jop, JynxLabel jlab) {
        super(jop);
        this.jlab = jlab;
    }

    @Override
    public void accept(MethodVisitor mv) {
        mv.visitLabel(jlab.asmlabel());
    }

    @Override
    public void adjust(StackLocals stackLocals) {
        stackLocals.visitLabel(jlab);
    }

    @Override
    public String toString() {
        return String.format("%s:",jlab.name());
    }

}

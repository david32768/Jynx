package asm.instruction;

import org.objectweb.asm.MethodVisitor;

import jvm.JvmOp;
import jynx2asm.StackLocals;

public class IncrInstruction extends Instruction {

    private final int var;
    private final int incr;

    public IncrInstruction(JvmOp jop, int var, int incr) {
        super(jop);
        this.var = var;
        this.incr = incr;
    }

    @Override
    public void accept(MethodVisitor mv) {
        mv.visitIincInsn(var, incr);
    }

    @Override
    public void adjust(StackLocals stackLocals) {
        stackLocals.checkIncr(var);
    }

    @Override
    public String toString() {
        return String.format("%s %d %d",base,var, incr);
    }

    
}

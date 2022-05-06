package asm.instruction;

import org.objectweb.asm.MethodVisitor;

import jvm.JvmOp;
import jynx2asm.StackLocals;

public class IncrInstruction extends Instruction {

    private final int varnum;
    private final int incr;

    public IncrInstruction(JvmOp jop, int varnum, int incr) {
        super(jop);
        this.varnum = varnum;
        this.incr = incr;
    }

    @Override
    public void accept(MethodVisitor mv) {
        mv.visitIincInsn(varnum, incr);
    }

    @Override
    public void adjust(StackLocals stackLocals) {
        stackLocals.checkIncr(varnum);
    }

    @Override
    public String toString() {
        return String.format("%s %d %d",base,varnum, incr);
    }

}

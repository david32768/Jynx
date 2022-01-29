package asm.instruction;

import org.objectweb.asm.MethodVisitor;

import jvm.JvmOp;
import jynx2asm.JynxCatch;
import jynx2asm.StackLocals;

public class TryInstruction extends Instruction {

    private final JynxCatch jcatch;

    public TryInstruction(JvmOp jop, JynxCatch jcatch) {
        super(jop);
        this.jcatch = jcatch;
    }

    @Override
    public void accept(MethodVisitor mv) {
        jcatch.accept(mv);;
    }

    @Override
    public void adjust(StackLocals stackLocals) {
        stackLocals.visitTryCatchBlock(jcatch);
    }

    @Override
    public String toString() {
        return String.format(".catch %s",jcatch);
    }

}

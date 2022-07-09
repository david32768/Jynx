package asm.instruction;

import org.objectweb.asm.MethodVisitor;

import jynx2asm.ops.JvmOp;
import jynx2asm.StackLocals;

public class VarInstruction extends Instruction {

    private final int varnum;

    public VarInstruction(JvmOp jop, int varnum) {
        super(jop);
        this.varnum = varnum;
    }

    @Override
    public void accept(MethodVisitor mv) {
        mv.visitVarInsn(jvmop.asmOpcode(),varnum);
    }
    
    @Override
    public void adjust(StackLocals stackLocals) {
        stackLocals.adjustLoadStore(jvmop, varnum);
    }

    @Override
    public String toString() {
        return String.format("%s %d",jvmop,varnum);
    }

}

package asm.instruction;

import org.objectweb.asm.MethodVisitor;

import jvm.AsmOp;
import jvm.JvmOp;
import jynx2asm.FrameElement;
import jynx2asm.StackLocals;

public class VarInstruction extends Instruction {

    private final int varnum;

    public VarInstruction(JvmOp jop, int varnum) {
        super(jop);
        this.varnum = varnum;
    }

    @Override
    public void accept(MethodVisitor mv) {
        mv.visitVarInsn(base.opcode(),varnum);
    }
    
    @Override
    public void adjust(StackLocals stackLocals) {
        stackLocals.adjustLoadStore(base, varnum);
    }

    @Override
    public String toString() {
        return String.format("%s %d",base,varnum);
    }

}

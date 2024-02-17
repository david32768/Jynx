package asm.instruction;

import org.objectweb.asm.MethodVisitor;

import jynx2asm.ops.JvmOp;
import jynx2asm.StackLocals;

public class StackInstruction extends Instruction {

    public StackInstruction(JvmOp jop) {
        super(jop);
    }

    @Override
    public void accept(MethodVisitor mv) {
        mv.visitInsn(jvmop.asmOpcode());
    }

    @Override
    public void adjust(StackLocals stackLocals) {
        stackLocals.adjustStackOp(jvmop);
    }

    @Override
    public String toString() {
        return jvmop.toString();
    }
    
}

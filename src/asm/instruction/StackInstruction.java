package asm.instruction;

import org.objectweb.asm.MethodVisitor;

import jvm.AsmOp;
import jvm.JvmOp;
import jynx2asm.ops.AliasOp;
import jynx2asm.StackLocals;

public class StackInstruction extends Instruction {

    public StackInstruction(JvmOp jop) {
        super(jop);
    }

    @Override
    public AsmOp resolve(StackLocals stackLocals) {
        if (base == null && jvmop instanceof AliasOp) {
            AliasOp aliasop = (AliasOp)jvmop;
            base = aliasop.resolve(stackLocals.stack().peekTOS(),null);
        }
        return base;
    }
    
    @Override
    public void accept(MethodVisitor mv) {
        mv.visitInsn(base.opcode());
    }

    @Override
    public void adjust(StackLocals stackLocals) {
        stackLocals.adjustStackOp(base);
    }

    @Override
    public String toString() {
        return jvmop.toString();
    }
    
    
}

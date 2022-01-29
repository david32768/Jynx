package asm.instruction;

import org.objectweb.asm.MethodVisitor;

import jvm.AsmOp;
import jvm.JvmOp;
import jynx2asm.FrameElement;
import jynx2asm.ops.AliasOp;
import jynx2asm.StackLocals;

public class VarInstruction extends Instruction {

    private int var;
    private final boolean relative;

    public VarInstruction(JvmOp jop, int var, boolean relative) {
        super(jop);
        this.var = var;
        this.relative = relative;
    }

    public VarInstruction(JvmOp jop, int var) {
        this(jop,var,false);
    }

    @Override
    public AsmOp resolve(StackLocals stackLocals) {
        if (base != null) {
            return base;
        }
        if (relative) {
            var = stackLocals.locals().absolute(var);
        }
        if (jvmop instanceof AliasOp) {
            AliasOp aliasop = (AliasOp)jvmop;
            FrameElement stackfe = stackLocals.stack().peekTOS();
            FrameElement localfe = stackLocals.locals().peek(var);
            base = aliasop.resolve(stackfe,localfe);
        }
        return base;
    }
    @Override
    public void accept(MethodVisitor mv) {
        mv.visitVarInsn(base.opcode(),var);
    }
    
    @Override
    public void adjust(StackLocals stackLocals) {
        stackLocals.adjustLoadStore(base, var);
    }

    @Override
    public String toString() {
        return String.format("%s %d",base,var);
    }

}

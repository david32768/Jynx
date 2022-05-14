package asm.instruction;

import org.objectweb.asm.MethodVisitor;

import jvm.AsmOp;
import jvm.JvmOp;
import jynx2asm.FrameElement;
import jynx2asm.ops.InternalOps;
import jynx2asm.StackLocals;

public class VarInstruction extends Instruction {

    private int varnum;
    private final boolean relative;

    public VarInstruction(JvmOp jop, int varnum, boolean relative) {
        super(jop);
        this.varnum = varnum;
        this.relative = relative;
    }

    public VarInstruction(JvmOp jop, int varnum) {
        this(jop,varnum,false);
    }

    @Override
    public AsmOp resolve(StackLocals stackLocals) {
        if (base != null) {
            return base;
        }
        if (relative) {
            varnum = stackLocals.locals().absolute(varnum);
        }
        if (jvmop instanceof InternalOps) {
            InternalOps aliasop = (InternalOps)jvmop;
            FrameElement stackfe = stackLocals.stack().peekTOS();
            FrameElement localfe = stackLocals.locals().peek(varnum);
            base = aliasop.resolve(stackfe,localfe);
        }
        return base;
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

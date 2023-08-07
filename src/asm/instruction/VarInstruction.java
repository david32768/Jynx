package asm.instruction;

import org.objectweb.asm.MethodVisitor;

import jynx2asm.ops.JvmOp;
import jynx2asm.StackLocals;
import jynx2asm.Token;

public class VarInstruction extends Instruction {

    private final Token varToken;
    private int varnum = -1;
    
    public VarInstruction(JvmOp jop, Token vartoken) {
        super(jop);
        this.varToken = vartoken;
    }

    @Override
    public void adjust(StackLocals stackLocals) {
        varnum = stackLocals.adjustLoadStore(jvmop, varToken);
        assert varnum >= 0;
        this.jvmop = JvmOp.exactVar(jvmop, varnum);
    }

    @Override
    public void accept(MethodVisitor mv) {
        mv.visitVarInsn(jvmop.asmOpcode(),varnum);
    }
    
    @Override
    public String toString() {
        if (varnum < 0) {
            return String.format("%s %s",jvmop,varToken);
        }
        return String.format("%s %d",jvmop,varnum);
    }

}

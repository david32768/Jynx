package asm.instruction;

import java.util.Arrays;

import org.objectweb.asm.MethodVisitor;

import jvm.JvmOp;
import jynx2asm.StackLocals;

public class MarrayInstruction extends Instruction {

    private final String type;
    private final int dims;

    public MarrayInstruction(JvmOp jop, String type, int dims) {
        super(jop);
        this.type = type;
        this.dims = dims;
    }

    @Override
    public void accept(MethodVisitor mv) {
        mv.visitMultiANewArrayInsn(type,dims);
    }

    @Override
    public void adjust(StackLocals stackLocals) {
        char[] parmarray = new char[dims];
        Arrays.fill(parmarray, 'I');
        String parms = String.valueOf(parmarray);
        parms = "(" + parms + ")" + type;
        stackLocals.adjustStackOperand(parms);
    }

    @Override
    public String toString() {
        return String.format("%s %s %d",base,type, dims);
    }

}

package asm.instruction;

import org.objectweb.asm.MethodVisitor;

import jvm.ConstType;
import jvm.JvmOp;
import jynx2asm.StackLocals;

public class LdcInstruction extends Instruction {

    private final Object cst;
    private final ConstType ct;

    public LdcInstruction(JvmOp jop,  Object cst, ConstType ct) {
        super(jop);
        this.cst = cst;
        this.ct = ct;
    }

    @Override
    public Integer minLength() {
        return 2;
    }

    @Override
    public Integer maxLength() {
        return 3;
    }

    @Override
    public void accept(MethodVisitor mv) {
        mv.visitLdcInsn(cst);
    }

    @Override
    public void adjust(StackLocals stackLocals) {
        stackLocals.adjustStackOperand("()" + ct.getDesc());
    }

    @Override
    public String toString() {
        return String.format("%s %s",base,cst);
    }

}

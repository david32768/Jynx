package asm.instruction;

import org.objectweb.asm.MethodVisitor;

import asm2jynx.Object2String;
import jvm.ConstType;
import jynx2asm.ops.JvmOp;
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
    public Integer maxLength() {
        if (jvmop == JvmOp.asm_ldc) {
            return JvmOp.opc_ldc_w.length();
        }
        return jvmop.length();
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
        Object2String o2s = new Object2String();
        return String.format("%s %s",jvmop,o2s.asm2String(cst));
    }

}

package asm.instruction;

import org.objectweb.asm.MethodVisitor;

import jynx2asm.ops.JvmOp;
import jynx2asm.StackLocals;

public class Instruction {

    protected final JvmOp jvmop;

    protected Instruction(JvmOp jvmop) {
        this.jvmop = jvmop;
    }

    public static Instruction getInstance(JvmOp jvmop) {
        assert !jvmop.isStack();
        return new Instruction(jvmop);
    }
    
    public boolean needLineNumber() {
        return jvmop == JvmOp.asm_idiv  || jvmop == JvmOp.asm_ldiv;
    }
    
    public JvmOp resolve(StackLocals stackLocals) {
        return jvmop;
    }

    public Integer minLength() {
        return jvmop.length();
    }
    
    public Integer maxLength() {
        return jvmop.length();
    }
    
    public void accept(MethodVisitor mv) {
        mv.visitInsn(jvmop.asmOpcode());
    }

    public void adjust(StackLocals stackLocals) {
        stackLocals.adjustStack(jvmop);
    }

    @Override
    public String toString() {
        return String.format("%s",jvmop);
    }

}

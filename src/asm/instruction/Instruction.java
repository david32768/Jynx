package asm.instruction;

import org.objectweb.asm.MethodVisitor;

import jvm.AsmOp;
import jvm.JvmOp;
import jynx2asm.StackLocals;

public class Instruction {

    protected final JvmOp jvmop;
    protected AsmOp base;

    protected Instruction(JvmOp jvmop) {
        this.jvmop = jvmop;
        this.base = jvmop == null?null:jvmop.getBase();
    }

    public static Instruction getInstance(JvmOp jvmop) {
        assert !jvmop.getBase().isStack();
        return new Instruction(jvmop);
    }
    
    
    public AsmOp resolve(StackLocals stackLocals) {
        return base;
    }

    public Integer minLength() {
        return jvmop.length();
    }
    
    public Integer maxLength() {
        return jvmop.length();
    }
    
    public void accept(MethodVisitor mv) {
        mv.visitInsn(base.opcode());
    }

    public void adjust(StackLocals stackLocals) {
        stackLocals.adjustStack(base);
    }

    @Override
    public String toString() {
        return String.format("%s",base);
    }

}

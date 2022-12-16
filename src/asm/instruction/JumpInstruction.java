package asm.instruction;

import org.objectweb.asm.MethodVisitor;

import jynx2asm.JynxLabel;
import jynx2asm.ops.JvmOp;
import jynx2asm.StackLocals;

public class JumpInstruction extends Instruction {

    private final JynxLabel jlab;
    
    private boolean isDefinitelyNotWide;
    private boolean isDefinitelyWide;

    public JumpInstruction(JvmOp jop, JynxLabel jlab) {
        super(jop);
        this.jlab = jlab;
        this.isDefinitelyNotWide = false;
        this.isDefinitelyWide = false;
    }

    @Override
    public JvmOp resolve(int minoffset, int maxoffset) {
        this.isDefinitelyNotWide = jlab.isDefinitelyNotWide(minoffset, maxoffset);
        this.isDefinitelyWide = jlab.isDefinitelyWide(minoffset, maxoffset);
        
        jlab.usedAt(minoffset, maxoffset,maxLength() - minLength());
        return jvmop;
    }

    @Override
    public void adjust(StackLocals stackLocals) {
        super.adjust(stackLocals);
        boolean jsr = jvmop == JvmOp.asm_jsr;
        stackLocals.adjustLabelJump(jlab, jsr);
    }

    private int wideLength() {
        switch(jvmop) {
            case asm_goto: case opc_goto_w:
                return JvmOp.opc_goto_w.length();
            case asm_jsr: case opc_jsr_w:
                return JvmOp.opc_jsr_w.length();
            default:
                return jvmop.length() + JvmOp.opc_goto_w.length();
        }
    }
    
    @Override
    public Integer minLength() {
        if (isDefinitelyWide) {
            return wideLength();
        } else {
            return jvmop.length();
        }
    }
    
    @Override
    public Integer maxLength() {
        if (isDefinitelyNotWide) {
            return jvmop.length();
        } else {
            return wideLength();
        }
    }
    
    @Override
    public void accept(MethodVisitor mv) {
        mv.visitJumpInsn(jvmop.asmOpcode(), jlab.asmlabel());
    }

    @Override
    public String toString() {
        return String.format("%s %s",jvmop,jlab.name());
    }

}

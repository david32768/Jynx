package asm.instruction;

import org.objectweb.asm.MethodVisitor;

import jynx2asm.JynxLabel;
import jynx2asm.ops.JvmOp;
import jynx2asm.StackLocals;

public class JumpInstruction extends Instruction {

    private final JynxLabel jlab;

    public JumpInstruction(JvmOp jop, JynxLabel jlab) {
        super(jop);
        this.jlab = jlab;
    }

    @Override
    public Integer maxLength() {
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
    public void accept(MethodVisitor mv) {
        mv.visitJumpInsn(jvmop.asmOpcode(), jlab.asmlabel());
    }

    @Override
    public void adjust(StackLocals stackLocals) {
        super.adjust(stackLocals);
        boolean jsr = jvmop == JvmOp.asm_jsr;
        stackLocals.adjustLabel(jlab, jsr);
    }

    @Override
    public String toString() {
        return String.format("%s %s",jvmop,jlab.name());
    }

}

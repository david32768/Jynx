package asm.instruction;

import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.MethodVisitor;

import jvm.JvmOp;
import jynx2asm.StackLocals;

public class DynamicInstruction extends Instruction {

    private final ConstantDynamic cd;
    private final Object[] bsmArgs;

    public DynamicInstruction(JvmOp jvmop,  ConstantDynamic  cstdyn) {
        super(jvmop);
        this.cd = cstdyn;
        this.bsmArgs = new Object[cd.getBootstrapMethodArgumentCount()];
        for (int i = 0; i < bsmArgs.length;++i) {
            bsmArgs[i] = cd.getBootstrapMethodArgument(i);
        }
    }

    @Override
    public void accept(MethodVisitor mv) {
        mv.visitInvokeDynamicInsn(cd.getName(), cd.getDescriptor(), cd.getBootstrapMethod(),bsmArgs);
    }

    @Override
    public void adjust(StackLocals stackLocals) {
        stackLocals.adjustStackOperand(cd.getDescriptor());
    }

    @Override
    public String toString() {
        return String.format("%s %s", base, cd);
    }

}

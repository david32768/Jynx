package asm.instruction;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import static jynx.Global.LOG;
import static jynx.Message.M34;

import jvm.NumType;
import jynx2asm.ops.JvmOp;
import jynx2asm.StackLocals;

public class LineInstruction extends Instruction {

    private static final int LINE_NUMBER_MOD = 50000; // 50000 for easy human calculation
    
    private final int lineNum;

    public LineInstruction(JvmOp jop, int lineNum) {
        super(jop);
        if (NumType.t_short.isInUnsignedRange(lineNum)) {
            this.lineNum = lineNum;
        } else {
            // "some generated line numbers have been reduced mod %d as exceed unsigned short max"
            LOG(M34,LINE_NUMBER_MOD);
            this.lineNum = lineNum%(LINE_NUMBER_MOD);
        }
    }

    @Override
    public void accept(MethodVisitor mv) {
        Label label = new Label();  //  to get multiple line numbers. eg in jdk3/ArtificialStructures
        mv.visitLabel(label);
        mv.visitLineNumber(lineNum, label);
    }

    @Override
    public void adjust(StackLocals stackLocals) {
    }

    @Override
    public String toString() {
        return String.format("line %d",lineNum);
    }

}

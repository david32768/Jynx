package asm.instruction;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import static jynx.Global.LOG;
import static jynx.Message.M800;

import jvm.NumType;
import jynx2asm.Line;
import jynx2asm.ops.JvmOp;
import jynx2asm.StackLocals;

public class LineInstruction extends Instruction {

    private final int lineNum;
    private final Line line;

    public LineInstruction(int linenum, Line line) {
        super((JvmOp)null);
        assert NumType.t_short.isInUnsignedRange(linenum);
        if (linenum == 0) {
            // "line number 0 changed to 1; ASM Issue #317989"
            LOG(M800);
            linenum = 1;
        }
        this.lineNum = linenum;
        this.line = line;
    }

    @Override
    public void adjust(StackLocals stackLocals) {}

    @Override
    public void accept(MethodVisitor mv) {
        Label label = new Label();  //  to get multiple line numbers. eg in jdk3/ArtificialStructures
        mv.visitLabel(label);
        mv.visitLineNumber(lineNum, label);
    }

    @Override
    public String toString() {
        return String.format("line %d",lineNum);
    }

}

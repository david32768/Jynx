package asm.instruction;

import java.util.List;
import java.util.stream.Collectors;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import jynx.ReservedWord;
import jynx2asm.JynxLabel;
import jynx2asm.ops.JvmOp;
import jynx2asm.StackLocals;

public class TableInstruction extends Instruction {

    private final int min;
    private final int max;
    private final JynxLabel dflt;
    private final List<JynxLabel> labels;

    public TableInstruction(JvmOp jop, int min, int max, JynxLabel dflt, List<JynxLabel> labels) {
        super(jop);
        this.min = min;
        this.max = max;
        this.dflt = dflt;
        this.labels = labels;
    }

    @Override
    public Integer minLength() {
        return 1 + 4 + 4 + 4 + 4*labels.size();
    }

    @Override
    public Integer maxLength() {
        return minLength() + 3; // maximum padding
    }

    @Override
    public void accept(MethodVisitor mv) {
        Label[] asmlabels = labels.stream()
            .map(JynxLabel::asmlabel)
            .toArray(Label[]::new);
        mv.visitTableSwitchInsn(min, max, dflt.asmlabel(), asmlabels);
    }

    @Override
    public void adjust(StackLocals stackLocals) {
        super.adjust(stackLocals);
        stackLocals.adjustLabels(dflt,labels);
    }

    @Override
    public String toString() {
        String brlabels = labels.stream()
                .map(JynxLabel::name)
                .collect(Collectors.joining(" , "));
        return String.format("%s %d default %s %s %s %s",
                jvmop,min, dflt,ReservedWord.left_array,brlabels,ReservedWord.right_array);
    }

}

package asm.instruction;

import java.util.Map;
import java.util.stream.Collectors;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import jvm.JvmOp;
import jynx.ReservedWord;
import jynx2asm.JynxLabel;
import jynx2asm.StackLocals;

public class LookupInstruction extends Instruction {

    private final JynxLabel dflt;
    private final Map<Integer,JynxLabel> intlabels;

    public LookupInstruction(JvmOp jop, JynxLabel dflt, Map<Integer,JynxLabel> intlabels) {
        super(jop);
        this.dflt = dflt;
        this.intlabels = intlabels;
    }

    @Override
    public Integer minLength() {
        return 1 + 4 + 4 + 8*intlabels.size();
    }

    @Override
    public Integer maxLength() {
        return minLength() + 3; // maximum padding
    }

    @Override
    public void accept(MethodVisitor mv) {
        Label[] asmlabels = intlabels.values().stream()
            .map(JynxLabel::asmlabel)
            .toArray(Label[]::new);
        int[] asmkeys = intlabels.keySet().stream()
                .mapToInt(i->(int)i)
                .toArray();
        mv.visitLookupSwitchInsn(dflt.asmlabel(), asmkeys, asmlabels);
    }

    @Override
    public void adjust(StackLocals stackLocals) {
        super.adjust(stackLocals);
        stackLocals.adjustLabels(dflt,intlabels.values());
    }

    @Override
    public String toString() {
        String brlabels = intlabels.entrySet().stream()
                .map(me-> me.getKey().toString() + " : " + me.getValue().name())
                .collect(Collectors.joining(" "));
        return String.format("%s default %s %s %s %s",
                base,dflt,ReservedWord.left_array,brlabels,ReservedWord.right_array);
    }

}

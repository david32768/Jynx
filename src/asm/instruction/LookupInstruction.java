package asm.instruction;

import java.util.Map;
import java.util.stream.Collectors;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import jynx.ReservedWord;
import jynx2asm.JynxLabel;
import jynx2asm.ops.JvmOp;
import jynx2asm.StackLocals;

public class LookupInstruction extends SwitchInstruction {

    private final JynxLabel dflt;
    private final Map<Integer,JynxLabel> intlabels;

    public LookupInstruction(JvmOp jop, JynxLabel dflt, Map<Integer,JynxLabel> intlabels) {
        super(jop, minsize(intlabels.size()));
        this.dflt = dflt;
        this.intlabels = intlabels;
    }

    private static final int OVERHEAD = 1 + 4 + 4; // opcode, dflt lbel, label count

    public static final long minsize(long labelct) {
        return OVERHEAD + 8*labelct;
    }
    
    @Override
    public void adjust(StackLocals stackLocals) {
        super.adjust(stackLocals);
        stackLocals.adjustLabelSwitch(dflt,intlabels.values());
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
    public String toString() {
        String brlabels = intlabels.entrySet().stream()
                .map(me-> me.getKey().toString() + " -> " + me.getValue().name())
                .collect(Collectors.joining(" , "));
        return String.format("%s default %s %s %s %s",
                jvmop,dflt,ReservedWord.left_array,brlabels,ReservedWord.right_array);
    }

}

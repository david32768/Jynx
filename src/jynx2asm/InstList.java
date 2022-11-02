package jynx2asm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;

import org.objectweb.asm.tree.MethodNode;

import static jynx.Global.LOG;
import static jynx.Global.OPTION;
import static jynx.Message.M290;
import static jynx.Message.M291;
import static jynx.Message.M292;
import static jynx.Message.M990;
import static jynx.ReservedWord.*;

import asm.instruction.Instruction;
import asm.instruction.LineInstruction;
import jynx.GlobalOption;
import jynx.ReservedWord;
import jynx2asm.ops.JvmOp;

public class InstList {

    private final List<Instruction> instructions;
    private final StackLocals stackLocals;
    private final Line line;
    private final String spacer;

    private final boolean expand;
    private final boolean stack;
    private final boolean locals;

    private boolean addLineNumber;
    
    private String stackb;
    private String localsb;
    
    public InstList(StackLocals stacklocals, Line line, EnumMap<ReservedWord, Integer> options) {
        this.instructions = new ArrayList<>();
        this.stackLocals = stacklocals;
        this.line = line;
        int indent = line.getIndent();
        char[] chars = new char[indent];
        Arrays.fill(chars, ' ');
        this.spacer = String.valueOf(chars);
        this.expand = options.containsKey(res_expand);
        this.stack = options.containsKey(res_stack);
        this.locals = options.containsKey(res_locals);
        this.stackb = this.stack? stackLocals.stringStack(): "";
        this.localsb = this.locals? stackLocals.stringLocals(): "";
        if (!options.isEmpty()) {
            LOG(M990,line); // "%s"
        }
        this.addLineNumber = OPTION(GlobalOption.GENERATE_LINE_NUMBERS);
    }

    public Line getLine() {
        return line;
    }

    private void printStack() {
        String stacka = stackLocals.stringStack();
        LOG(M290, spacer,stackb,stacka); // ";%s  %s -> %s"
        stackb = stacka;
    }
    
    private void printLocals() {
        String localsa = stackLocals.stringLocals();
        if (!localsa.equals(localsb)) {
            LOG(M291,spacer,res_locals,localsa); // ";%s  %s = %s"
        }
        localsb = localsa;
    }
    
    private void printStackLocals() {
        if (stack) {
            printStack();
        }
        if (locals) {
            printLocals();
        }
    }

    private void addInsn(Instruction insn) {
        if (expand) {
            LOG(M292,spacer,insn); // "%s  +%s"
        }
        boolean ok = stackLocals.visitInsn(insn, line);
        if (ok) {
            instructions.add(insn);
            if (expand) {
                printStackLocals();
            }
        }
    }

    public void add(Instruction insn) {
        if (addLineNumber && insn.needLineNumber()) {
            addInsn(new LineInstruction(line.getLinect(),line));    
            addLineNumber = false;
        }
        addInsn(insn);
    }

    public void accept(MethodNode mnode) {
        for (Instruction in:instructions) {
            in.accept(mnode);
        }
        if (!expand) {
            printStackLocals();
        }
    }
    
    public FrameElement peekTOS() {
        return stackLocals.stack().peekTOS();
    }
    
    public FrameElement peekVar(int varnum) {
        return stackLocals.locals().peek(varnum);
    }
    
    public int absolute(int varnum) {
        return stackLocals.locals().absolute(varnum);
    }

    public JvmOp getReturnOp() {
        return stackLocals.getReturnOp();
    }
}

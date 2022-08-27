package jynx2asm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;

import org.objectweb.asm.tree.MethodNode;

import static jynx.ReservedWord.res_locals;

import asm.instruction.Instruction;
import jynx2asm.ops.JvmOp;

public class InstList {

    private final List<Instruction> instructions;
    private final StackLocals stackLocals;
    private final Line line;
    private final String spacer;

    private final boolean expand;
    private final boolean stack;
    private final boolean locals;
    
    private String stackb;
    private String localsb;
    
    public InstList(StackLocals stacklocals, Line line, EnumMap<PrintOption, Integer> options) {
        this.instructions = new ArrayList<>();
        this.stackLocals = stacklocals;
        this.line = line;
        int indent = line.getIndent();
        char[] chars = new char[indent];
        Arrays.fill(chars, ' ');
        this.spacer = String.valueOf(chars);
        this.expand = options.containsKey(PrintOption.EXPAND);
        this.stack = options.containsKey(PrintOption.STACK);
        this.locals = options.containsKey(PrintOption.LOCALS);
        this.stackb = this.stack? stackLocals.stringStack(): "";
        this.localsb = this.locals? stackLocals.stringStack(): "";
        if (!options.isEmpty()) {
            System.out.println(line);
        }
    }

    private void printStack() {
        String stacka = stackLocals.stringStack();
        System.out.format(";%s  %s -> %s%n", spacer,stackb,stacka);
        stackb = stacka;
    }
    
    private void printLocals() {
        String localsa = stackLocals.stringLocals();
        if (!localsa.equals(localsb)) {
            System.out.format(";%s %s = %s%n",spacer,res_locals,localsa);
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
    
    public void add(Instruction insn) {
        if (expand) {
            System.out.format("%s  +%s%n",spacer,insn);
        }
        boolean ok = stackLocals.visitInsn(insn, line);
        if (ok) {
            instructions.add(insn);
            if (expand) {
                printStackLocals();
            }
        }
    }

    public void addFront(Instruction insn) {
        instructions.add(0,insn);
    }

    public List<Instruction> getInstructions() {
        return instructions;
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

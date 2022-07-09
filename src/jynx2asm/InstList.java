package jynx2asm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.objectweb.asm.tree.MethodNode;

import static jynx.ReservedWord.res_locals;

import asm.instruction.Instruction;
import jynx2asm.ops.JvmOp;

public class InstList {

    private final List<Instruction> instructions;
    private final StackLocals stackLocals;
    private final Line line;
    private final boolean print;
    private final boolean expand;
    private final String spacer;
    
    private String stackb;
    private String localsb;
    
    public InstList(StackLocals stacklocals, Line line,boolean print, boolean expand) {
        this.instructions = new ArrayList<>();
        this.stackLocals = stacklocals;
        this.line = line;
        this.print = print;
        this.expand = expand;
        int indent = line.getIndent();
        char[] chars = new char[indent];
        Arrays.fill(chars, ' ');
        this.spacer = String.valueOf(chars);
        if (print) {
            this.stackb = stackLocals.stringStack();
            this.localsb = stackLocals.stringLocals();
            System.out.println(line);
        } else {
            this.stackb = "";
            this.localsb = "";
        }
    }

    private void printStackLocals() {
        String stacka = stackLocals.stringStack();
        String localsa = stackLocals.stringLocals();
        System.out.format(";%s  %s -> %s", spacer,stackb,stacka);
        if (!localsa.equals(localsb)) {
            System.out.format(";%s %s = %s",spacer,res_locals,localsa);
        }
        System.out.println();
        stackb = stacka;
        localsb = localsa;
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
        if (print && !expand) {
            printStackLocals();
        }
        for (Instruction in:instructions) {
            in.accept(mnode);
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

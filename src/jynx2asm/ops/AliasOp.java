package jynx2asm.ops;

import java.util.Optional;

import asm.instruction.Instruction;
import jvm.AsmOp;
import jynx2asm.Line;

public interface AliasOp extends JynxOp {

    public Optional<Instruction> getInst(Line line, AsmOp returnop);

}

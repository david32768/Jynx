package jynx2asm.ops;

import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static jvm.AsmOp.*;

import asm.instruction.Instruction;
import asm.instruction.IntInstruction;
import asm.instruction.LdcInstruction;
import jvm.AsmOp;
import jvm.ConstType;
import jvm.Feature;
import jvm.JvmOp;
import jvm.NumType;
import jvm.Op;
import jynx2asm.Line;

public enum AliasOps implements AliasOp, JvmOp {

    //convenience aliases
    opc_ildc(AliasOps::ildc,1,3),
    opc_lldc(AliasOps::lldc,1,3),
    opc_fldc(AliasOps::fldc,1,3),
    opc_dldc(AliasOps::dldc,1,3),
    ;    

    private final BiFunction<AliasOps,Line,Instruction> fn;
    private final Integer minlength;
    private final Integer maxlength;
    private final Feature requires;

    private AliasOps(BiFunction<AliasOps,Line,Instruction> fn,
            Integer minlength, Integer maxlength) {
        this(fn,minlength,maxlength,Feature.unlimited);
    }

    private AliasOps(BiFunction<AliasOps,Line, Instruction> fn,
            Integer minlength, Integer maxlength, Feature requires) {
        this.fn = fn;
        this.minlength = minlength;
        this.maxlength = maxlength;
        this.requires = requires;
    }
    
    public static Stream<AliasOps> streamExternal() {
        return Arrays.stream(values())
            .filter(m->m.name().startsWith("opc_"));
    }
    
    @Override
    public Feature feature() {
        return requires;
    }

    @Override
    public Integer length() {
        return Objects.equals(minlength, maxlength)?minlength:null;
    }

    @Override
    public Integer minLength() {
        return minlength;
    }

    @Override
    public Integer maxLength() {
        return maxlength;
    }

    @Override
    public AsmOp getBase() {
        return null;
    }

    @Override
    public Optional<Instruction> getInst(Line line, AsmOp returnop) {
        Objects.nonNull(fn);
        return Optional.of(fn.apply(this,line));
    }
    
    private Instruction ildc(Line line) {
        int ival = line.nextToken().asInt();
        switch(ival) {
            case -1:
                return Instruction.getInstance(asm_iconst_m1);
            case 0:
                return Instruction.getInstance(asm_iconst_0);
            case 1:
                return Instruction.getInstance(asm_iconst_1);
            case 2:
                return Instruction.getInstance(asm_iconst_2);
            case 3:
                return Instruction.getInstance(asm_iconst_3);
            case 4:
                return Instruction.getInstance(asm_iconst_4);
            case 5:
                return Instruction.getInstance(asm_iconst_5);
        }
        if (NumType.t_byte.isInRange(ival)) {
            return new IntInstruction(asm_bipush,ival);
        } else if (NumType.t_short.isInRange(ival)) {
            return new IntInstruction(asm_sipush,ival);
        } else {
            return new LdcInstruction(asm_ldc,ival,ConstType.ct_int);
        }
    }

    private Instruction lldc(Line line) {
        long lval = line.nextToken().asLong();
        if (lval == 0L) {
            return Instruction.getInstance(asm_lconst_0);
        } else if (lval == 1L) {
            return Instruction.getInstance(asm_lconst_1);
        } else {
            return new LdcInstruction(Op.opc_ldc2_w, lval, ConstType.ct_long);
        }
    }

    private static final int MINUS_ZERO_FLOAT = Float.floatToRawIntBits(-0.0f);
    
    private Instruction fldc(Line line) {
        float fval = line.nextToken().asFloat();
        if (fval == 0.0f && Float.floatToRawIntBits(fval) != MINUS_ZERO_FLOAT) {
            return Instruction.getInstance(asm_fconst_0);
        } else if (fval == 1.0f) {
            return Instruction.getInstance(asm_fconst_1);
        } else if (fval == 2.0f) {
            return Instruction.getInstance(asm_fconst_2);
        } else {
            return new LdcInstruction(asm_ldc,fval,ConstType.ct_float);
        }
    }

    private static final long MINUS_ZERO_DOUBLE = Double.doubleToRawLongBits(-0.0);
    
    private Instruction dldc(Line line) {
        double dval = line.nextToken().asDouble();
        if (dval == 0.0 && Double.doubleToRawLongBits(dval) != MINUS_ZERO_DOUBLE) {
            return Instruction.getInstance(asm_dconst_0);
        } else if (dval == 1.0) {
            return Instruction.getInstance(asm_dconst_1);
        } else {
            return new LdcInstruction(Op.opc_ldc2_w,dval,ConstType.ct_double);
        }
    }
    
    @Override
    public String toString() {
        assert name().startsWith("opc_");
        return name().substring(4);
    }

}

package jynx2asm.ops;

import java.util.Objects;
import java.util.stream.Stream;

import static jvm.AsmOp.*;

import asm.instruction.Instruction;
import asm.instruction.IntInstruction;
import asm.instruction.LdcInstruction;
import asm.instruction.StackInstruction;
import asm.instruction.VarInstruction;
import jvm.AsmOp;
import jvm.ConstType;
import jvm.Feature;
import jvm.JvmOp;
import jvm.NumType;
import jvm.Op;
import jynx2asm.FrameElement;
import jynx2asm.InstList;
import jynx2asm.Line;

public enum AliasOps implements JynxOp, JvmOp {

    opc_ildc(1,3),
    opc_lldc(1,3),
    opc_fldc(1,3),
    opc_dldc(1,3),

    xxx_xload(2,4),
    xxx_xstore(2,4),
    xxx_xload_rel(2,4),
    xxx_xstore_rel(2,4),

    xxx_dupn(1,1),
    xxx_popn(1,1),
    xxx_dupn_xn(1,1),

    xxx_xreturn(1,1),
    ;    

    private final Integer minlength;
    private final Integer maxlength;
    private final Feature requires;

    private AliasOps(Integer minlength, Integer maxlength) {
        this(minlength,maxlength,Feature.unlimited);
    }

    private AliasOps(Integer minlength, Integer maxlength, Feature requires) {
        this.minlength = minlength;
        this.maxlength = maxlength;
        this.requires = requires;
    }
    
    @Override
    public boolean isExternal() {
        return name().startsWith("opc_");
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

    public Instruction getInst(Line line, InstList instlist) {
        switch (this) {
            case opc_ildc:
                return ildc(line);
            case opc_lldc:
                return lldc(line);
            case opc_fldc:
                return fldc(line);
            case opc_dldc:
                return dldc(line);
            case xxx_xreturn:
                AsmOp asmop = instlist.getReturnOp();
                return Instruction.getInstance(asmop);
        }
        FrameElement stackfe = instlist.peekTOS();
        boolean isTwo = stackfe.isTwo();
        AsmOp asmop;
        int num;
        switch (this) {
            case xxx_dupn:
                asmop = isTwo?asm_dup2:asm_dup;
                return new StackInstruction(asmop);
            case xxx_dupn_xn:
                asmop = isTwo?asm_dup2_x2:asm_dup_x1;
                return new StackInstruction(asmop);
            case xxx_popn:
                asmop = isTwo?asm_pop2:asm_pop;
                return new StackInstruction(asmop);
            case xxx_xload:
                num = line.nextToken().asUnsignedShort();
                asmop = resolveLoad(instlist.peekVar(num));
                return new VarInstruction(asmop, num);
            case xxx_xload_rel:
                num = line.nextToken().asUnsignedShort();
                num = instlist.absolute(num);
                asmop = resolveLoad(instlist.peekVar(num));
                return new VarInstruction(asmop, num);
            case xxx_xstore:
                num = line.nextToken().asUnsignedShort();
                asmop = resolveStore(stackfe);
                return new VarInstruction(asmop, num);
            case xxx_xstore_rel:
                num = line.nextToken().asUnsignedShort();
                num = instlist.absolute(num);
                asmop = resolveStore(stackfe);
                return new VarInstruction(asmop, num);
            default:
                throw new EnumConstantNotPresentException(this.getClass(), name());
        }
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
    
    private static AsmOp resolveLoad(FrameElement localfe) {
        AsmOp base;
        switch(localfe) {
            case INTEGER:
                base = asm_iload;
                break;
            case LONG:
                base = asm_lload;
                break;
            case FLOAT:
                base = asm_fload;
                break;
            case DOUBLE:
                base = asm_dload;
                break;
            default:
                base = asm_aload;
                break;
        }
        return base;
    }
    
    private static  AsmOp resolveStore(FrameElement stackfe) {
        AsmOp base;
        switch(stackfe) {
            case INTEGER:
                base = asm_istore;
                break;
            case LONG:
                base = asm_lstore;
                break;
            case FLOAT:
                base = asm_fstore;
                break;
            case DOUBLE:
                base = asm_dstore;
                break;
            default:
                base = asm_astore;
                break;
        }
        return base;
    }
    
    @Override
    public String toString() {
        return name().startsWith("opc_")?name().substring(4):name();
    }

    public static Stream<AliasOps> streamExternal() {
        return Stream.of(values())
            .filter(AliasOps::isExternal);
    }
    
}

package jynx2asm.ops;

import java.util.Objects;
import java.util.stream.Stream;

import static jvm.AsmOp.*;
import static jvm.OpArg.arg_constant;
import static jvm.OpArg.arg_none;
import static jvm.OpArg.arg_var;

import asm.instruction.Instruction;
import asm.instruction.IntInstruction;
import asm.instruction.LdcInstruction;
import asm.instruction.StackInstruction;
import asm.instruction.VarInstruction;
import jvm.AsmOp;
import jvm.ConstType;
import jvm.Feature;
import jvm.JvmOp;
import jvm.JvmVersionRange;
import jvm.NumType;
import jvm.Op;
import jvm.OpArg;
import jynx2asm.FrameElement;
import jynx2asm.InstList;
import jynx2asm.Line;

public enum AliasOps implements JynxOp {

    opc_ildc(arg_constant,1,3),
    opc_lldc(arg_constant,1,3),
    opc_fldc(arg_constant,1,3),
    opc_dldc(arg_constant,1,3),

    xxx_xload(arg_var,1,4),
    xxx_xstore(arg_var,1,4),
    xxx_xload_rel(arg_var,1,4),
    xxx_xstore_rel(arg_var,1,4),

    xxx_dupn(arg_none,1,1),
    xxx_popn(arg_none,1,1),
    xxx_dupn_xn(arg_none,1,1),

    xxx_xreturn(arg_none,1,1),
    ;    

    private final OpArg oparg;
    private final Integer minlength;
    private final Integer maxlength;
    private final Feature requires;

    private AliasOps(OpArg oparg, Integer minlength, Integer maxlength) {
        this(oparg,minlength,maxlength,Feature.unlimited);
    }

    private AliasOps(OpArg oparg, Integer minlength, Integer maxlength, Feature requires) {
        this.oparg = oparg;
        this.minlength = minlength;
        this.maxlength = maxlength;
        this.requires = requires;
    }
    
    @Override
    public JvmVersionRange range() {
        return requires.range();
    }

    @Override
    public Integer length() {
        return Objects.equals(minlength, maxlength)?minlength:null;
    }

    @Override
    public boolean isExternal() {
        return name().startsWith("opc_");
    }

    public Instruction getInst(Line line, InstList instlist) {
        switch (oparg) {
            case arg_none:
                return getInstNone(instlist);
            case arg_var:
                return getInstVar(line,instlist);
            case arg_constant:
                return getInstConstant(line);
            default:
                throw new EnumConstantNotPresentException(oparg.getClass(), oparg.name());
        }
    }

    private Instruction getInstVar(Line line, InstList instlist) {
        int num  = line.nextToken().asUnsignedShort();
        JvmOp jvmop;
        switch (this) {
            case xxx_xload_rel:
                num = instlist.absolute(num);
                // FALL THROUGH
            case xxx_xload:
                FrameElement localfe = instlist.peekVar(num);
                jvmop = localfe.addTypeChar("load");
                break;
            case xxx_xstore_rel:
                num = instlist.absolute(num);
                // FALL THROUGH
            case xxx_xstore:
                FrameElement stackfe = instlist.peekTOS();
                jvmop = stackfe.addTypeChar("store");
                break;
            default:
                throw new EnumConstantNotPresentException(this.getClass(), name());
        }
        return new VarInstruction(jvmop, num);
    }

    private Instruction getInstNone(InstList instlist) {
        if (this == xxx_xreturn) {
            AsmOp asmop = instlist.getReturnOp();
            return Instruction.getInstance(asmop);
        }
        FrameElement stackfe = instlist.peekTOS();
        boolean isTwo = stackfe.isTwo();
        JvmOp jvmop;
        switch (this) {
            case xxx_dupn:
                jvmop = isTwo?asm_dup2:asm_dup;
                break;
            case xxx_dupn_xn:
                jvmop = isTwo?asm_dup2_x2:asm_dup_x1;
                break;
            case xxx_popn:
                jvmop = isTwo?asm_pop2:asm_pop;
                break;
            default:
                throw new EnumConstantNotPresentException(this.getClass(), name());
        }
        return new StackInstruction(jvmop);
    }

    private Instruction getInstConstant(Line line) {
        switch (this) {
            case opc_ildc:
                return ildc(line);
            case opc_lldc:
                return lldc(line);
            case opc_fldc:
                return fldc(line);
            case opc_dldc:
                return dldc(line);
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
    
    @Override
    public String toString() {
        return name().substring(4);
    }

    public static Stream<AliasOps> streamExternal() {
        return Stream.of(values())
            .filter(AliasOps::isExternal);
    }
    
}

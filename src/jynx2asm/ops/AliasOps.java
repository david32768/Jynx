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
import asm.instruction.StackInstruction;
import asm.instruction.VarInstruction;
import jvm.AsmOp;
import jvm.ConstType;
import jvm.Feature;
import jvm.JvmOp;
import jvm.NumType;
import jvm.Op;
import jynx2asm.FrameElement;
import jynx2asm.Line;

public enum AliasOps implements AliasOp, JvmOp {

    //convenience aliases
    opc_ildc(AliasOps::ildc,null,1,3),
    opc_lldc(AliasOps::lldc,null,1,3),
    opc_fldc(AliasOps::fldc,null,1,3),
    opc_dldc(AliasOps::dldc,null,1,3),
    opc_xload(AliasOps::xload,AliasOps::resolveLoad,2,2),
    opc_xstore(AliasOps::xstore,AliasOps::resolveStore,2,2),
    opc_xload_rel(AliasOps::xload_rel,AliasOps::resolveLoad,2,4),
    opc_xstore_rel(AliasOps::xstore_rel,null,2,4),
    opc_dupn(AliasOps::stack,AliasOps::resolveDup,1,1),
    opc_popn(AliasOps::stack,AliasOps::resolvePop,1,1),
    opc_dupn_xn(AliasOps::stack,AliasOps::resolveDupX,1,1),
    opc_xreturn(null,null,1,1),
    // intermediate codes for correct length
    aux_xload_w(null,AliasOps::resolveLoad,4,4),
    aux_xstore_w(null,AliasOps::resolveStore,4,4),
    aux_xload_n(null,AliasOps::resolveLoad,1,1),
    aux_xstore_n(null,AliasOps::resolveStore,1,1),
    ;    

    private final BiFunction<AliasOps,Line,Instruction> fn;
    private final BiFunction<FrameElement,FrameElement,AsmOp> resolvefn;
    private final Integer minlength;
    private final Integer maxlength;
    private final Feature requires;

    private AliasOps(BiFunction<AliasOps,Line,Instruction> fn, BiFunction<FrameElement,FrameElement,AsmOp> resolvefn,
            Integer minlength, Integer maxlength) {
        this(fn,resolvefn,minlength,maxlength,Feature.unlimited);
    }

    private AliasOps(BiFunction<AliasOps,Line, Instruction> fn, BiFunction<FrameElement,FrameElement, AsmOp> resolvefn,
            Integer minlength, Integer maxlength, Feature requires) {
        this.fn = fn;
        this.resolvefn = resolvefn;
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
        if (this == opc_xreturn) {
            return Optional.of(Instruction.getInstance(returnop));
        }
        Objects.nonNull(fn);
        return Optional.of(fn.apply(this,line));
    }
    
    @Override
    public AsmOp resolve(FrameElement stackfe, FrameElement localfe) {
        Objects.nonNull(resolvefn);
        return resolvefn.apply(stackfe,localfe);
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
    
    private static Instruction xloadHelper(int num, boolean relative) {
        if (num <= 3) {
            return new VarInstruction(aux_xload_n, num,relative);
        }
        if (NumType.t_byte.isInUnsignedRange(num)) {
            return new VarInstruction(opc_xload, num,relative);
        }
        return new VarInstruction(aux_xload_w, num,relative);
    } 
    
    private static Instruction xstoreHelper(int num, boolean relative) {
        if (num <= 3) {
            return new VarInstruction(aux_xstore_n, num,relative);
        }
        if (NumType.t_byte.isInUnsignedRange(num)) {
            return new VarInstruction(opc_xstore, num,relative);
        }
        return new VarInstruction(aux_xstore_w, num,relative);
    } 
    
    private Instruction xload(Line line) {
        int num = line.nextToken().asUnsignedShort();
        return xloadHelper(num,false);
    }

    private Instruction xstore(Line line) {
        int num = line.nextToken().asUnsignedShort();
        return xstoreHelper(num,false);
    }

    private Instruction xload_rel(Line line) {
        int num = line.nextToken().asUnsignedShort();
        return xloadHelper(num,true);
    }

    private Instruction xstore_rel(Line line) {
        int num = line.nextToken().asUnsignedShort();
        return xstoreHelper(num,true);
    }

    private Instruction stack(Line line) {
        return new StackInstruction(this);
    }

    private static AsmOp resolveLoad(FrameElement stackfe,FrameElement localfe) {
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
    
    private static  AsmOp resolveStore(FrameElement stackfe,FrameElement localfe) {
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
    
    private static  AsmOp resolveDup(FrameElement stackfe,FrameElement localfe) {
        assert localfe == null;
        return stackfe.isTwo()?asm_dup2:asm_dup;
    }
    
    private  static AsmOp resolvePop(FrameElement stackfe,FrameElement localfe) {
        assert localfe == null;
        return stackfe.isTwo()?asm_pop2:asm_pop;
    }
    
    private  static AsmOp resolveDupX(FrameElement stackfe,FrameElement localfe) {
        assert localfe == null;
        return stackfe.isTwo()?asm_dup2_x2:asm_dup_x1;
    }
    
    @Override
    public String toString() {
        return name().startsWith("opc_")?name().substring(4):name();
    }

}

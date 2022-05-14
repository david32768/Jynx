package jynx2asm.ops;

import java.util.function.BiFunction;
import java.util.Objects;
import java.util.Optional;

import static jvm.AsmOp.*;

import asm.instruction.Instruction;
import asm.instruction.StackInstruction;
import asm.instruction.VarInstruction;
import jvm.AsmOp;
import jvm.Feature;
import jvm.JvmOp;
import jvm.NumType;
import jynx2asm.FrameElement;
import jynx2asm.Line;

public enum InternalOps implements AliasOp, JvmOp {

    opc_xload(InternalOps::xload,InternalOps::resolveLoad,2,2),
    opc_xstore(InternalOps::xstore,InternalOps::resolveStore,2,2),
    opc_xload_rel(InternalOps::xload_rel,InternalOps::resolveLoad,2,4),
    opc_xstore_rel(InternalOps::xstore_rel,null,2,4),
    opc_dupn(InternalOps::stack,InternalOps::resolveDup,1,1),
    opc_popn(InternalOps::stack,InternalOps::resolvePop,1,1),
    opc_dupn_xn(InternalOps::stack,InternalOps::resolveDupX,1,1),
    opc_xreturn(null,null,1,1),
    // intermediate codes for correct length
    aux_xload_w(null,InternalOps::resolveLoad,4,4),
    aux_xstore_w(null,InternalOps::resolveStore,4,4),
    aux_xload_n(null,InternalOps::resolveLoad,1,1),
    aux_xstore_n(null,InternalOps::resolveStore,1,1),
    ;    

    private final BiFunction<InternalOps,Line,Instruction> fn;
    private final BiFunction<FrameElement,FrameElement,AsmOp> resolvefn;
    private final Integer minlength;
    private final Integer maxlength;
    private final Feature requires;

    private InternalOps(BiFunction<InternalOps,Line,Instruction> fn, BiFunction<FrameElement,FrameElement,AsmOp> resolvefn,
            Integer minlength, Integer maxlength) {
        this(fn,resolvefn,minlength,maxlength,Feature.unlimited);
    }

    private InternalOps(BiFunction<InternalOps,Line, Instruction> fn, BiFunction<FrameElement,FrameElement, AsmOp> resolvefn,
            Integer minlength, Integer maxlength, Feature requires) {
        this.fn = fn;
        this.resolvefn = resolvefn;
        this.minlength = minlength;
        this.maxlength = maxlength;
        this.requires = requires;
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
    
    public AsmOp resolve(FrameElement stackfe, FrameElement localfe) {
        Objects.nonNull(resolvefn);
        return resolvefn.apply(stackfe,localfe);
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

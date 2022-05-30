package jvm;

import java.util.Objects;
import java.util.stream.Stream;

import static org.objectweb.asm.Opcodes.*;

import static jvm.OpArg.*;
import static jvm.OperandStackType.*;
import static jynx.Global.LOG;
import static jynx.Message.*;

import asm.CheckOpcodes;
import jynx.LogIllegalArgumentException;


public enum AsmOp implements JvmOp {
    asm_aaload(ARRAY_LOAD, 'A', 50, AALOAD, arg_none),
    asm_aastore(ARRAY_STORE, 'A', 83, AASTORE, arg_none),
    asm_aconst_null(PRODUCE, 'A', 1, ACONST_NULL, arg_none),
    asm_aload(PRODUCE, 'A', 25, ALOAD, arg_var),
    asm_anewarray(ARRAY_CREATE, 'A', 189, ANEWARRAY, arg_class),
    asm_areturn(XRETURN, 'A', 176, ARETURN, arg_none),
    asm_arraylength(A2X, 'I', 190, ARRAYLENGTH, arg_none),
    asm_astore(STORE, 'A', 58, ASTORE, arg_var),
    asm_athrow(GO, 'A', 191, ATHROW, arg_none),
    asm_baload(ARRAY_LOAD, 'I',  51, BALOAD, arg_none),
    asm_bastore(ARRAY_STORE, 'I', 84, BASTORE, arg_none),
    asm_bipush(PRODUCE, 'I', 16, BIPUSH, arg_byte),
    asm_caload(ARRAY_LOAD, 'I', 52, CALOAD, arg_none),
    asm_castore(ARRAY_STORE, 'I', 85, CASTORE, arg_none),
    xxx_catch(NOCHANGE, ' ', -4, -4, arg_dir),
    asm_checkcast(A2X, 'A', 192, CHECKCAST, arg_class),
    asm_d2f(TRANSFORM2F, 'D', 144, D2F, arg_none),
    asm_d2i(TRANSFORM2I, 'D', 142, D2I, arg_none),
    asm_d2l(TRANSFORM2J, 'D', 143, D2L, arg_none),
    asm_dadd(BINARY, 'D', 99, DADD, arg_none),
    asm_daload(ARRAY_LOAD, 'D', 49, DALOAD, arg_none),
    asm_dastore(ARRAY_STORE, 'D', 82, DASTORE, arg_none),
    asm_dcmpg(COMPARE, 'D', 152, DCMPG, arg_none),
    asm_dcmpl(COMPARE, 'D', 151, DCMPL, arg_none),
    asm_dconst_0(PRODUCE, 'D', 14, DCONST_0, arg_none),
    asm_dconst_1(PRODUCE, 'D', 15, DCONST_1, arg_none),
    asm_ddiv(BINARY, 'D', 111, DDIV, arg_none),
    asm_dload(PRODUCE, 'D', 24, DLOAD, arg_var),
    asm_dmul(BINARY, 'D', 107, DMUL, arg_none),
    asm_dneg(UNARY, 'D', 119, DNEG, arg_none),
    asm_drem(BINARY, 'D', 115, DREM, arg_none),
    asm_dreturn(XRETURN, 'D', 175, DRETURN, arg_none),
    asm_dstore(STORE, 'D', 57, DSTORE, arg_var),
    asm_dsub(BINARY, 'D', 103, DSUB, arg_none),
    asm_dup(STACK, 'S', 89, DUP, arg_none),
    asm_dup2(STACK, 'S', 92, DUP2, arg_none),
    asm_dup2_x1(STACK, 'S', 93, DUP2_X1, arg_none),
    asm_dup2_x2(STACK, 'S', 94, DUP2_X2, arg_none),
    asm_dup_x1(STACK, 'S', 90, DUP_X1, arg_none),
    asm_dup_x2(STACK, 'S', 91, DUP_X2, arg_none),
    asm_f2d(TRANSFORM2D, 'F', 141, F2D, arg_none),
    asm_f2i(TRANSFORM2I, 'F', 139, F2I, arg_none),
    asm_f2l(TRANSFORM2J, 'F', 140, F2L, arg_none),
    asm_fadd(BINARY, 'F', 98, FADD, arg_none),
    asm_faload(ARRAY_LOAD, 'F', 48, FALOAD, arg_none),
    asm_fastore(ARRAY_STORE, 'F', 81, FASTORE, arg_none),
    asm_fcmpg(COMPARE, 'F', 150, FCMPG, arg_none),
    asm_fcmpl(COMPARE, 'F', 149, FCMPL, arg_none),
    asm_fconst_0(PRODUCE, 'F', 11, FCONST_0, arg_none),
    asm_fconst_1(PRODUCE, 'F', 12, FCONST_1, arg_none),
    asm_fconst_2(PRODUCE, 'F', 13, FCONST_2, arg_none),
    asm_fdiv(BINARY, 'F', 110, FDIV, arg_none),
    asm_fload(PRODUCE, 'F', 23, FLOAD, arg_var),
    asm_fmul(BINARY, 'F', 106, FMUL, arg_none),
    asm_fneg(UNARY, 'F', 118, FNEG, arg_none),
    asm_frem(BINARY, 'F', 114, FREM, arg_none),
    asm_freturn(XRETURN, 'F', 174, FRETURN, arg_none),
    asm_fstore(STORE, 'F', 56, FSTORE, arg_var),
    asm_fsub(BINARY, 'F', 102, FSUB, arg_none),
    asm_getfield(OPERAND, 'O', 180, GETFIELD, arg_field),
    asm_getstatic(OPERAND, 'O', 178, GETSTATIC, arg_field),
    asm_goto(GO, ' ', 167, GOTO, arg_label),
    asm_i2b(TRANSFORM2I, 'I', 145, I2B, arg_none),
    asm_i2c(TRANSFORM2I, 'I', 146, I2C, arg_none),
    asm_i2d(TRANSFORM2D, 'I', 135, I2D, arg_none),
    asm_i2f(TRANSFORM2F, 'I', 134, I2F, arg_none),
    asm_i2l(TRANSFORM2J, 'I', 133, I2L, arg_none),
    asm_i2s(TRANSFORM2I, 'I', 147, I2S, arg_none),
    asm_iadd(BINARY, 'I', 96, IADD, arg_none),
    asm_iaload(ARRAY_LOAD, 'I', 46, IALOAD, arg_none),
    asm_iand(BINARY, 'I', 126, IAND, arg_none),
    asm_iastore(ARRAY_STORE, 'I', 79, IASTORE, arg_none),
    asm_iconst_0(PRODUCE, 'I', 3, ICONST_0, arg_none),
    asm_iconst_1(PRODUCE, 'I', 4, ICONST_1, arg_none),
    asm_iconst_2(PRODUCE, 'I', 5, ICONST_2, arg_none),
    asm_iconst_3(PRODUCE, 'I', 6, ICONST_3, arg_none),
    asm_iconst_4(PRODUCE, 'I', 7, ICONST_4, arg_none),
    asm_iconst_5(PRODUCE, 'I', 8, ICONST_5, arg_none),
    asm_iconst_m1(PRODUCE, 'I', 2, ICONST_M1, arg_none),
    asm_idiv(BINARY, 'I', 108, IDIV, arg_none),
    asm_if_acmpeq(COMPARE_IF, 'A', 165, IF_ACMPEQ, arg_label),
    asm_if_acmpne(COMPARE_IF, 'A', 166, IF_ACMPNE, arg_label),
    asm_if_icmpeq(COMPARE_IF, 'I', 159, IF_ICMPEQ, arg_label),
    asm_if_icmpge(COMPARE_IF, 'I', 162, IF_ICMPGE, arg_label),
    asm_if_icmpgt(COMPARE_IF, 'I', 163, IF_ICMPGT, arg_label),
    asm_if_icmple(COMPARE_IF, 'I', 164, IF_ICMPLE, arg_label),
    asm_if_icmplt(COMPARE_IF, 'I', 161, IF_ICMPLT, arg_label),
    asm_if_icmpne(COMPARE_IF, 'I', 160, IF_ICMPNE, arg_label),
    asm_ifeq(IF, 'I', 153, IFEQ, arg_label),
    asm_ifge(IF, 'I', 156, IFGE, arg_label),
    asm_ifgt(IF, 'I', 157, IFGT, arg_label),
    asm_ifle(IF, 'I', 158, IFLE, arg_label),
    asm_iflt(IF, 'I', 155, IFLT, arg_label),
    asm_ifne(IF, 'I', 154, IFNE, arg_label),
    asm_ifnonnull(IF, 'A', 199, IFNONNULL, arg_label),
    asm_ifnull(IF, 'A', 198, IFNULL, arg_label),
    asm_iinc(NOCHANGE, 'I', 132, IINC, arg_incr),
    asm_iload(PRODUCE, 'I', 21, ILOAD, arg_var),
    asm_imul(BINARY, 'I', 104, IMUL, arg_none),
    asm_ineg(UNARY, 'I', 116, INEG, arg_none),
    asm_instanceof(A2X, 'I', 193, INSTANCEOF, arg_class),
    asm_invokedynamic(OPERAND, 'O', 186, INVOKEDYNAMIC, arg_callsite,Feature.invokeDynamic),
    asm_invokeinterface(OPERAND, 'O', 185, INVOKEINTERFACE, arg_interface),
    asm_invokespecial(OPERAND, 'O', 183, INVOKESPECIAL, arg_method,Feature.invokespecial),
    asm_invokestatic(OPERAND, 'O', 184, INVOKESTATIC, arg_method),
    asm_invokevirtual(OPERAND, 'O', 182, INVOKEVIRTUAL, arg_method),
    asm_ior(BINARY, 'I', 128, IOR, arg_none),
    asm_irem(BINARY, 'I',112, IREM, arg_none),
    asm_ireturn(XRETURN, 'I',172, IRETURN, arg_none),
    asm_ishl(SHIFT, 'I', 120, ISHL, arg_none),
    asm_ishr(SHIFT, 'I', 122, ISHR, arg_none),
    asm_istore(STORE, 'I', 54, ISTORE, arg_var),
    asm_isub(BINARY, 'I', 100, ISUB, arg_none),
    asm_iushr(SHIFT, 'I', 124, IUSHR, arg_none),
    asm_ixor(BINARY, 'I', 130, IXOR, arg_none),
    asm_jsr(NOCHANGE, ' ', 168,JSR,arg_label,Feature.subroutines), // jvms 4.9.1
    asm_l2d(TRANSFORM2D, 'J', 138, L2D, arg_none),
    asm_l2f(TRANSFORM2F, 'J', 137, L2F, arg_none),
    asm_l2i(TRANSFORM2I, 'J', 136, L2I, arg_none),
    xxx_label(NOCHANGE,' ',-1,-1,arg_dir),
    asm_ladd(BINARY, 'J', 97, LADD, arg_none),
    asm_laload(ARRAY_LOAD, 'J', 47, LALOAD, arg_none),
    asm_land(BINARY, 'J', 127, LAND, arg_none),
    asm_lastore(ARRAY_STORE, 'J', 80, LASTORE, arg_none),
    asm_lcmp(COMPARE, 'J', 148, LCMP, arg_none),
    asm_lconst_0(PRODUCE, 'J', 9, LCONST_0, arg_none),
    asm_lconst_1(PRODUCE, 'J', 10, LCONST_1, arg_none),
    asm_ldc(OPERAND, 'O', 18, LDC, arg_constant),
    asm_ldiv(BINARY, 'J', 109, LDIV, arg_none),
    asm_lload(PRODUCE, 'J', 22, LLOAD, arg_var),
    asm_lmul(BINARY, 'J', 105, LMUL, arg_none),
    asm_lneg(UNARY, 'J', 117, LNEG, arg_none),
    asm_lookupswitch(GO, 'I', 171, LOOKUPSWITCH, arg_lookupswitch),
    asm_lor(BINARY, 'J', 129, LOR, arg_none),
    asm_lrem(BINARY, 'J', 113, LREM, arg_none),
    asm_lreturn(XRETURN, 'J', 173, LRETURN, arg_none),
    asm_lshl(SHIFT, 'J', 121, LSHL, arg_none),
    asm_lshr(SHIFT, 'J', 123, LSHR, arg_none),
    asm_lstore(STORE, 'J', 55, LSTORE, arg_var),
    asm_lsub(BINARY, 'J', 101, LSUB, arg_none),
    asm_lushr(SHIFT, 'J', 125, LUSHR, arg_none),
    asm_lxor(BINARY, 'J', 131, LXOR, arg_none),
    asm_monitorenter(CONSUME, 'A', 194, MONITORENTER, arg_none),
    asm_monitorexit(CONSUME, 'A', 195, MONITOREXIT, arg_none),
    asm_multianewarray(OPERAND, 'O', 197, MULTIANEWARRAY, arg_marray),
    asm_new(PRODUCE, 'A', 187, NEW, arg_class),
    asm_newarray(ARRAY_CREATE, 'A', 188, NEWARRAY, arg_atype),
    asm_nop(NOCHANGE, ' ', 0, NOP, arg_none),
    asm_pop(STACK, 'S', 87, POP, arg_none),
    asm_pop2(STACK, 'S', 88, POP2, arg_none),
    asm_putfield(OPERAND, 'O', 181, PUTFIELD, arg_field),
    asm_putstatic(OPERAND, 'O', 179, PUTSTATIC, arg_field),
    asm_ret(GO, 'R', 169, RET, arg_var,Feature.subroutines), // implied by jvms 4.9.1
    asm_return(XRETURN, ' ', 177, RETURN, arg_none),
    asm_saload(ARRAY_LOAD, 'I', 53, SALOAD, arg_none),
    asm_sastore(ARRAY_STORE, 'I', 86, SASTORE, arg_none),
    asm_sipush(PRODUCE, 'I', 17, SIPUSH, arg_short),
    asm_swap(STACK, 'S', 95, SWAP, arg_none),
    asm_tableswitch(GO, 'I', 170, TABLESWITCH, arg_tableswitch),
    
    ;

    private final OperandStackType type;
    private final char ctype;
    private final int opcode;
    private final int asmOpcode;
    private final OpArg args;
    private final Feature requires;
    private final Integer length;
    private final String desc;

    private AsmOp(OperandStackType type, char ctype, int opcode, int asmopcode, OpArg args) {
        this(type,ctype, opcode, asmopcode, args, Feature.unlimited);
    }
    
    private AsmOp(OperandStackType type, char ctype, int opcode,int asmopcode, OpArg args, Feature requires) {
        this.type = type;
        this.ctype = ctype;
        this.opcode = opcode;
        this.asmOpcode = asmopcode;
        this.args = args;
        this.requires = requires;
        this.length = args.length();
        this.desc = type.getDesc(ctype);
        assert opcode == asmopcode;
    }

    private static final AsmOp[] codemap = new AsmOp[256];
    
    static {
        boolean ok = true;
        String last = "";
        for (AsmOp op:values()) {
            assert (op.type == STACK) == (op.ctype == 'S');
            assert (op.type == OPERAND) == (op.ctype == 'O');
            assert op.type != STORE || op.args == arg_var;
            assert op.name().compareTo(last) > 0:String.format("%s %s",op,last);
            int opcode = op.opcode;
            if (opcode < 0) {
                continue;
            }
            assert opcode == CheckOpcodes.getStaticFieldValue(op.toString().toUpperCase());
            AsmOp mapop = codemap[opcode];
            if (mapop == null) {
                codemap[opcode] = op;
            } else {
                LOG(M274,op,opcode,mapop); // "duplicate: %s has the same opcode(%d) as %s"
                ok = false;
            }
            last = op.name();
        }
        if (!ok) {
            LOG(M280); // "program terminated because of severe error(s)"
            System.exit(1);
        }
    }

    public static AsmOp getInstance(int opcode, JvmVersion jvmversion) {
        AsmOp result =  codemap[opcode];
        Objects.nonNull(result);
        jvmversion.checkSupports(result);
        return result;
    }
    
    OperandStackType type() {
        return type;
    }

    public char ctype() {
        return ctype;
    }
    
    
    public int opcode() {
        return asmOpcode;
    }

    public OpArg args() {
        return args;
    }

    @Override
    public boolean isImmediate() {
        return length != null && length == 1;
    }

    @Override
    public Integer length() {
        return length;
    }
    
    @Override
    public Feature feature() {
        return requires;
    }

    @Override
    public AsmOp getBase() {
        return this;
    }

    @Override
    public boolean isExternal() {
        return name().startsWith("asm_");
    }

    public boolean isStack() {
        return type == STACK;
    }
    
    public boolean isUnconditional() {
        return type == GO || type == XRETURN;
    }
    
    public boolean isReturn() {
        return type == XRETURN;
    }
    
    public void checkArg(OpArg expected) {
        if (args != expected) {
            // "expected arg %s but was %s"
            throw new LogIllegalArgumentException(M362,expected,args);
        }
    }
    
    public char vartype() {
        checkArg(arg_var);
        return ctype;
    }
    
    public boolean isStoreVar() {
        checkArg(arg_var);
        return type == STORE;
    }
    
    public String desc() {
        return desc;
    }

    @Override
    public String toString() {
        return name().substring(4);
    }

    public static void main(String[] args) {
        String arg = args[0];
        if (arg.startsWith("arg_")) {
            Stream.of(values())
                    .filter(op->op.args.name().equalsIgnoreCase(arg))
                    .forEach(op->System.out.format("%s %s %s%n",op,op.type,op.args));
        } else {
            Stream.of(values())
                    .filter(op->op.type.name().equalsIgnoreCase(arg))
                    .forEach(op->System.out.format("%s %s %s%n",op,op.type,op.args));
        }
    }
}

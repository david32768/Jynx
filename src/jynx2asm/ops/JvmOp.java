package jynx2asm.ops;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static org.objectweb.asm.Opcodes.*;

import static jvm.OpArg.*;
import static jynx.Global.LOG;
import static jynx.Message.M274;
import static jynx.Message.M280;
import static jynx.Message.M302;
import static jynx.Message.M362;
import static jynx2asm.ops.OperandStackType.*;

import asm.CheckOpcodes;
import jvm.Feature;
import jvm.JvmVersion;
import jvm.JvmVersionRange;
import jvm.NumType;
import jvm.OpArg;
import jynx.LogIllegalArgumentException;

public enum JvmOp implements JynxOp {

    asm_aaload(50,1,ARRAY_LOAD,'A',AALOAD,arg_none),
    asm_aastore(83,1,ARRAY_STORE,'A',AASTORE,arg_none),
    asm_aconst_null(1,1,PRODUCE,'A',ACONST_NULL,arg_none),
    asm_aload(25,2,PRODUCE,'A',ALOAD,arg_var),
    asm_anewarray(189,3,ARRAY_CREATE,'A',ANEWARRAY,arg_class),
    asm_areturn(176,1,XRETURN,'A',ARETURN,arg_none),
    asm_arraylength(190,1,A2X,'I',ARRAYLENGTH,arg_none),
    asm_astore(58,2,STORE,'A',ASTORE,arg_var),
    asm_athrow(191,1,GO,'A',ATHROW,arg_none),
    asm_baload(51,1,ARRAY_LOAD,'I',BALOAD,arg_none),
    asm_bastore(84,1,ARRAY_STORE,'I',BASTORE,arg_none),
    asm_bipush(16,2,PRODUCE,'I',BIPUSH,arg_byte),
    asm_caload(52,1,ARRAY_LOAD,'I',CALOAD,arg_none),
    asm_castore(85,1,ARRAY_STORE,'I',CASTORE,arg_none),
    asm_checkcast(192,3,A2X,'A',CHECKCAST,arg_class),
    asm_d2f(144,1,TRANSFORM2F,'D',D2F,arg_none),
    asm_d2i(142,1,TRANSFORM2I,'D',D2I,arg_none),
    asm_d2l(143,1,TRANSFORM2J,'D',D2L,arg_none),
    asm_dadd(99,1,BINARY,'D',DADD,arg_none),
    asm_daload(49,1,ARRAY_LOAD,'D',DALOAD,arg_none),
    asm_dastore(82,1,ARRAY_STORE,'D',DASTORE,arg_none),
    asm_dcmpg(152,1,COMPARE,'D',DCMPG,arg_none),
    asm_dcmpl(151,1,COMPARE,'D',DCMPL,arg_none),
    asm_dconst_0(14,1,PRODUCE,'D',DCONST_0,arg_none),
    asm_dconst_1(15,1,PRODUCE,'D',DCONST_1,arg_none),
    asm_ddiv(111,1,BINARY,'D',DDIV,arg_none),
    asm_dload(24,2,PRODUCE,'D',DLOAD,arg_var),
    asm_dmul(107,1,BINARY,'D',DMUL,arg_none),
    asm_dneg(119,1,UNARY,'D',DNEG,arg_none),
    asm_drem(115,1,BINARY,'D',DREM,arg_none),
    asm_dreturn(175,1,XRETURN,'D',DRETURN,arg_none),
    asm_dstore(57,2,STORE,'D',DSTORE,arg_var),
    asm_dsub(103,1,BINARY,'D',DSUB,arg_none),
    asm_dup(89,1,STACK,'S',DUP,arg_none),
    asm_dup2(92,1,STACK,'S',DUP2,arg_none),
    asm_dup2_x1(93,1,STACK,'S',DUP2_X1,arg_none),
    asm_dup2_x2(94,1,STACK,'S',DUP2_X2,arg_none),
    asm_dup_x1(90,1,STACK,'S',DUP_X1,arg_none),
    asm_dup_x2(91,1,STACK,'S',DUP_X2,arg_none),
    asm_f2d(141,1,TRANSFORM2D,'F',F2D,arg_none),
    asm_f2i(139,1,TRANSFORM2I,'F',F2I,arg_none),
    asm_f2l(140,1,TRANSFORM2J,'F',F2L,arg_none),
    asm_fadd(98,1,BINARY,'F',FADD,arg_none),
    asm_faload(48,1,ARRAY_LOAD,'F',FALOAD,arg_none),
    asm_fastore(81,1,ARRAY_STORE,'F',FASTORE,arg_none),
    asm_fcmpg(150,1,COMPARE,'F',FCMPG,arg_none),
    asm_fcmpl(149,1,COMPARE,'F',FCMPL,arg_none),
    asm_fconst_0(11,1,PRODUCE,'F',FCONST_0,arg_none),
    asm_fconst_1(12,1,PRODUCE,'F',FCONST_1,arg_none),
    asm_fconst_2(13,1,PRODUCE,'F',FCONST_2,arg_none),
    asm_fdiv(110,1,BINARY,'F',FDIV,arg_none),
    asm_fload(23,2,PRODUCE,'F',FLOAD,arg_var),
    asm_fmul(106,1,BINARY,'F',FMUL,arg_none),
    asm_fneg(118,1,UNARY,'F',FNEG,arg_none),
    asm_frem(114,1,BINARY,'F',FREM,arg_none),
    asm_freturn(174,1,XRETURN,'F',FRETURN,arg_none),
    asm_fstore(56,2,STORE,'F',FSTORE,arg_var),
    asm_fsub(102,1,BINARY,'F',FSUB,arg_none),
    asm_getfield(180,3,OPERAND,'O',GETFIELD,arg_field),
    asm_getstatic(178,3,OPERAND,'O',GETSTATIC,arg_field),
    asm_goto(167,3,GO,' ',GOTO,arg_label),
    asm_i2b(145,1,TRANSFORM2I,'I',I2B,arg_none),
    asm_i2c(146,1,TRANSFORM2I,'I',I2C,arg_none),
    asm_i2d(135,1,TRANSFORM2D,'I',I2D,arg_none),
    asm_i2f(134,1,TRANSFORM2F,'I',I2F,arg_none),
    asm_i2l(133,1,TRANSFORM2J,'I',I2L,arg_none),
    asm_i2s(147,1,TRANSFORM2I,'I',I2S,arg_none),
    asm_iadd(96,1,BINARY,'I',IADD,arg_none),
    asm_iaload(46,1,ARRAY_LOAD,'I',IALOAD,arg_none),
    asm_iand(126,1,BINARY,'I',IAND,arg_none),
    asm_iastore(79,1,ARRAY_STORE,'I',IASTORE,arg_none),
    asm_iconst_0(3,1,PRODUCE,'I',ICONST_0,arg_none),
    asm_iconst_1(4,1,PRODUCE,'I',ICONST_1,arg_none),
    asm_iconst_2(5,1,PRODUCE,'I',ICONST_2,arg_none),
    asm_iconst_3(6,1,PRODUCE,'I',ICONST_3,arg_none),
    asm_iconst_4(7,1,PRODUCE,'I',ICONST_4,arg_none),
    asm_iconst_5(8,1,PRODUCE,'I',ICONST_5,arg_none),
    asm_iconst_m1(2,1,PRODUCE,'I',ICONST_M1,arg_none),
    asm_idiv(108,1,BINARY,'I',IDIV,arg_none),
    asm_if_acmpeq(165,3,COMPARE_IF,'A',IF_ACMPEQ,arg_label),
    asm_if_acmpne(166,3,COMPARE_IF,'A',IF_ACMPNE,arg_label),
    asm_if_icmpeq(159,3,COMPARE_IF,'I',IF_ICMPEQ,arg_label),
    asm_if_icmpge(162,3,COMPARE_IF,'I',IF_ICMPGE,arg_label),
    asm_if_icmpgt(163,3,COMPARE_IF,'I',IF_ICMPGT,arg_label),
    asm_if_icmple(164,3,COMPARE_IF,'I',IF_ICMPLE,arg_label),
    asm_if_icmplt(161,3,COMPARE_IF,'I',IF_ICMPLT,arg_label),
    asm_if_icmpne(160,3,COMPARE_IF,'I',IF_ICMPNE,arg_label),
    asm_ifeq(153,3,IF,'I',IFEQ,arg_label),
    asm_ifge(156,3,IF,'I',IFGE,arg_label),
    asm_ifgt(157,3,IF,'I',IFGT,arg_label),
    asm_ifle(158,3,IF,'I',IFLE,arg_label),
    asm_iflt(155,3,IF,'I',IFLT,arg_label),
    asm_ifne(154,3,IF,'I',IFNE,arg_label),
    asm_ifnonnull(199,3,IF,'A',IFNONNULL,arg_label),
    asm_ifnull(198,3,IF,'A',IFNULL,arg_label),
    asm_iinc(132,3,NOCHANGE,'I',IINC,arg_incr),
    asm_iload(21,2,PRODUCE,'I',ILOAD,arg_var),
    asm_imul(104,1,BINARY,'I',IMUL,arg_none),
    asm_ineg(116,1,UNARY,'I',INEG,arg_none),
    asm_instanceof(193,3,A2X,'I',INSTANCEOF,arg_class),
    asm_invokedynamic(186,5,OPERAND,'O',INVOKEDYNAMIC,arg_callsite,Feature.invokeDynamic),
    asm_invokeinterface(185,5,OPERAND,'O',INVOKEINTERFACE,arg_interface),
    asm_invokespecial(183,3,OPERAND,'O',INVOKESPECIAL,arg_method,Feature.invokespecial),
    asm_invokestatic(184,3,OPERAND,'O',INVOKESTATIC,arg_method),
    asm_invokevirtual(182,3,OPERAND,'O',INVOKEVIRTUAL,arg_method),
    asm_ior(128,1,BINARY,'I',IOR,arg_none),
    asm_irem(112,1,BINARY,'I',IREM,arg_none),
    asm_ireturn(172,1,XRETURN,'I',IRETURN,arg_none),
    asm_ishl(120,1,SHIFT,'I',ISHL,arg_none),
    asm_ishr(122,1,SHIFT,'I',ISHR,arg_none),
    asm_istore(54,2,STORE,'I',ISTORE,arg_var),
    asm_isub(100,1,BINARY,'I',ISUB,arg_none),
    asm_iushr(124,1,SHIFT,'I',IUSHR,arg_none),
    asm_ixor(130,1,BINARY,'I',IXOR,arg_none),
    asm_jsr(168,3,NOCHANGE,' ',JSR,arg_label,Feature.subroutines),
    asm_l2d(138,1,TRANSFORM2D,'J',L2D,arg_none),
    asm_l2f(137,1,TRANSFORM2F,'J',L2F,arg_none),
    asm_l2i(136,1,TRANSFORM2I,'J',L2I,arg_none),
    asm_ladd(97,1,BINARY,'J',LADD,arg_none),
    asm_laload(47,1,ARRAY_LOAD,'J',LALOAD,arg_none),
    asm_land(127,1,BINARY,'J',LAND,arg_none),
    asm_lastore(80,1,ARRAY_STORE,'J',LASTORE,arg_none),
    asm_lcmp(148,1,COMPARE,'J',LCMP,arg_none),
    asm_lconst_0(9,1,PRODUCE,'J',LCONST_0,arg_none),
    asm_lconst_1(10,1,PRODUCE,'J',LCONST_1,arg_none),
    asm_ldc(18,2,OPERAND,'O',LDC,arg_constant),
    asm_ldiv(109,1,BINARY,'J',LDIV,arg_none),
    asm_lload(22,2,PRODUCE,'J',LLOAD,arg_var),
    asm_lmul(105,1,BINARY,'J',LMUL,arg_none),
    asm_lneg(117,1,UNARY,'J',LNEG,arg_none),
    asm_lookupswitch(171,null,GO,'I',LOOKUPSWITCH,arg_lookupswitch),
    asm_lor(129,1,BINARY,'J',LOR,arg_none),
    asm_lrem(113,1,BINARY,'J',LREM,arg_none),
    asm_lreturn(173,1,XRETURN,'J',LRETURN,arg_none),
    asm_lshl(121,1,SHIFT,'J',LSHL,arg_none),
    asm_lshr(123,1,SHIFT,'J',LSHR,arg_none),
    asm_lstore(55,2,STORE,'J',LSTORE,arg_var),
    asm_lsub(101,1,BINARY,'J',LSUB,arg_none),
    asm_lushr(125,1,SHIFT,'J',LUSHR,arg_none),
    asm_lxor(131,1,BINARY,'J',LXOR,arg_none),
    asm_monitorenter(194,1,CONSUME,'A',MONITORENTER,arg_none),
    asm_monitorexit(195,1,CONSUME,'A',MONITOREXIT,arg_none),
    asm_multianewarray(197,4,OPERAND,'O',MULTIANEWARRAY,arg_marray),
    asm_new(187,3,PRODUCE,'A',NEW,arg_class),
    asm_newarray(188,2,ARRAY_CREATE,'A',NEWARRAY,arg_atype),
    asm_nop(0,1,NOCHANGE,' ',NOP,arg_none),
    asm_pop(87,1,STACK,'S',POP,arg_none),
    asm_pop2(88,1,STACK,'S',POP2,arg_none),
    asm_putfield(181,3,OPERAND,'O',PUTFIELD,arg_field),
    asm_putstatic(179,3,OPERAND,'O',PUTSTATIC,arg_field),
    asm_ret(169,2,GO,'R',RET,arg_var,Feature.subroutines),
    asm_return(177,1,XRETURN,' ',RETURN,arg_none),
    asm_saload(53,1,ARRAY_LOAD,'I',SALOAD,arg_none),
    asm_sastore(86,1,ARRAY_STORE,'I',SASTORE,arg_none),
    asm_sipush(17,3,PRODUCE,'I',SIPUSH,arg_short),
    asm_swap(95,1,STACK,'S',SWAP,arg_none),
    asm_tableswitch(170,null,GO,'I',TABLESWITCH,arg_tableswitch),

    opc_aload_0(42,1,PRODUCE,'A',ALOAD,arg_var),
    opc_aload_1(43,1,PRODUCE,'A',ALOAD,arg_var),
    opc_aload_2(44,1,PRODUCE,'A',ALOAD,arg_var),
    opc_aload_3(45,1,PRODUCE,'A',ALOAD,arg_var),
    opc_aload_w(25,4,PRODUCE,'A',ALOAD,arg_var),
    opc_astore_0(75,1,STORE,'A',ASTORE,arg_var),
    opc_astore_1(76,1,STORE,'A',ASTORE,arg_var),
    opc_astore_2(77,1,STORE,'A',ASTORE,arg_var),
    opc_astore_3(78,1,STORE,'A',ASTORE,arg_var),
    opc_astore_w(58,4,STORE,'A',ASTORE,arg_var),
    opc_dload_0(38,1,PRODUCE,'D',DLOAD,arg_var),
    opc_dload_1(39,1,PRODUCE,'D',DLOAD,arg_var),
    opc_dload_2(40,1,PRODUCE,'D',DLOAD,arg_var),
    opc_dload_3(41,1,PRODUCE,'D',DLOAD,arg_var),
    opc_dload_w(24,4,PRODUCE,'D',DLOAD,arg_var),
    opc_dstore_0(71,1,STORE,'D',DSTORE,arg_var),
    opc_dstore_1(72,1,STORE,'D',DSTORE,arg_var),
    opc_dstore_2(73,1,STORE,'D',DSTORE,arg_var),
    opc_dstore_3(74,1,STORE,'D',DSTORE,arg_var),
    opc_dstore_w(57,4,STORE,'D',DSTORE,arg_var),
    opc_fload_0(34,1,PRODUCE,'F',FLOAD,arg_var),
    opc_fload_1(35,1,PRODUCE,'F',FLOAD,arg_var),
    opc_fload_2(36,1,PRODUCE,'F',FLOAD,arg_var),
    opc_fload_3(37,1,PRODUCE,'F',FLOAD,arg_var),
    opc_fload_w(23,4,PRODUCE,'F',FLOAD,arg_var),
    opc_fstore_0(67,1,STORE,'F',FSTORE,arg_var),
    opc_fstore_1(68,1,STORE,'F',FSTORE,arg_var),
    opc_fstore_2(69,1,STORE,'F',FSTORE,arg_var),
    opc_fstore_3(70,1,STORE,'F',FSTORE,arg_var),
    opc_fstore_w(56,4,STORE,'F',FSTORE,arg_var),
    opc_goto_w(200,5,GO,' ',GOTO,arg_label),
    opc_iinc_w(132,6,NOCHANGE,'I',IINC,arg_incr),
    opc_iload_0(26,1,PRODUCE,'I',ILOAD,arg_var),
    opc_iload_1(27,1,PRODUCE,'I',ILOAD,arg_var),
    opc_iload_2(28,1,PRODUCE,'I',ILOAD,arg_var),
    opc_iload_3(29,1,PRODUCE,'I',ILOAD,arg_var),
    opc_iload_w(21,4,PRODUCE,'I',ILOAD,arg_var),
    opc_invokenonvirtual(183,3,OPERAND,'O',INVOKESPECIAL,arg_method,Feature.invokenonvirtual),
    opc_istore_0(59,1,STORE,'I',ISTORE,arg_var),
    opc_istore_1(60,1,STORE,'I',ISTORE,arg_var),
    opc_istore_2(61,1,STORE,'I',ISTORE,arg_var),
    opc_istore_3(62,1,STORE,'I',ISTORE,arg_var),
    opc_istore_w(54,4,STORE,'I',ISTORE,arg_var),
    opc_jsr_w(201,5,NOCHANGE,' ',JSR,arg_label,Feature.subroutines),
    opc_ldc2_w(20,3,OPERAND,'O',LDC,arg_constant),
    opc_ldc_w(19,3,OPERAND,'O',LDC,arg_constant),
    opc_lload_0(30,1,PRODUCE,'J',LLOAD,arg_var),
    opc_lload_1(31,1,PRODUCE,'J',LLOAD,arg_var),
    opc_lload_2(32,1,PRODUCE,'J',LLOAD,arg_var),
    opc_lload_3(33,1,PRODUCE,'J',LLOAD,arg_var),
    opc_lload_w(22,4,PRODUCE,'J',LLOAD,arg_var),
    opc_lstore_0(63,1,STORE,'J',LSTORE,arg_var),
    opc_lstore_1(64,1,STORE,'J',LSTORE,arg_var),
    opc_lstore_2(65,1,STORE,'J',LSTORE,arg_var),
    opc_lstore_3(66,1,STORE,'J',LSTORE,arg_var),
    opc_lstore_w(55,4,STORE,'J',LSTORE,arg_var),
    opc_ret_w(169,4,GO,'R',RET,arg_var,Feature.subroutines),

    opc_wide(196,null,NOCHANGE,' ',NOP,arg_none),

    xxx_catch(-4,0,NOCHANGE,' ',-4,arg_dir),
    xxx_label(-1,0,NOCHANGE,' ',-1,arg_dir),
    xxx_labelweak(-1,0,NOCHANGE,' ',-1,arg_dir),
    ;
        
    private final int opcode;
    private final Integer length;
    private final OperandStackType type;
    private final char ctype;
    private final int asmOpcode;
    private final OpArg args;
    private final Feature requires;
    private final String desc;

    private JvmOp(int opcode, Integer length, OperandStackType type, char ctype, int asmOpcode, OpArg args) {
        this(opcode, length, type, ctype, asmOpcode, args, Feature.unlimited);
    }

    private JvmOp(int opcode, Integer length, OperandStackType type, char ctype, int asmOpcode, OpArg args, Feature requires) {
        this.opcode = opcode;
        this.length = length;
        this.type = type;
        this.ctype = ctype;
        this.asmOpcode = asmOpcode;
        this.args = args;
        this.requires = requires;
        this.desc = type.getDesc(ctype);
    }

    private static final JvmOp[] CODEMAP = new JvmOp[256];
    private static final Map<String,JvmOp> OPMAP = new HashMap<>();

    static {
        
        boolean ok = true;
        String last = "";
        for (JvmOp op:values()) {
            assert (op.type == STACK) == (op.ctype == 'S');
            assert (op.type == OPERAND) == (op.ctype == 'O');
            assert op.type != STORE || op.args == arg_var;
            assert op.name().compareTo(last) > 0:String.format("%s %s",op,last);
            int opcode = op.opcode;
            JvmOp mapop;
            String prefix = op.name().substring(0, 4);
            switch (prefix) {
                case "asm_":
                    assert opcode >= 0 && opcode < 256;
                    assert opcode == CheckOpcodes.getStaticFieldValue(op.toString().toUpperCase());
                    mapop = CODEMAP[opcode];
                    if (mapop == null) {
                        CODEMAP[opcode] = op;
                    } else {
                        LOG(M274,op,opcode,mapop); // "duplicate: %s has the same opcode(%d) as %s"
                        ok = false;
                    }
                    break;
                case "opc_":
                    assert opcode >= 0 && opcode < 256;
                    mapop = CODEMAP[opcode];
                    if (mapop == null) {
                        CODEMAP[opcode] = op;
                    } else {
                        boolean validsame = op.isWideFormOf(mapop)
                                && (op.args() == arg_var || op.args() == arg_incr);
                        if (validsame && !Objects.equals(op.feature(), mapop.feature())) {
                            // "%s is null or has different feature requirement than %s"
                            throw new LogIllegalArgumentException(M302,op,mapop);
                        }
                        if (!validsame && mapop != asm_invokespecial && op != opc_invokenonvirtual){
                            LOG(M274,op,opcode,mapop); // "duplicate: %s has the same opcode(%d) as %s"
                            ok = false;
                        }
                    }
                    break;
                case "xxx_":
                    assert opcode < 0;
                    break;
                default:
                    throw new AssertionError();
            }
            last = op.name();
            if (ok) {
                JvmOp before = OPMAP.putIfAbsent(op.toString(), op);
                ok = before == null;
            }
        }
        if (!ok) {
            LOG(M280); // "program terminated because of severe error(s)"
            System.exit(1);
        }
    }
    
    public static JvmOp getInstance(int opcode, JvmVersion jvmversion) {
        JvmOp result =  CODEMAP[opcode];
        Objects.nonNull(result);
        jvmversion.checkSupports(result);
        return result;
    }
    
    public int opcode() {
        return opcode;
    }

    public Integer length() {
        return length;
    }
    
    public char ctype() {
        return ctype;
    }
    
    
    public int asmOpcode() {
        return asmOpcode;
    }

    public OpArg args() {
        return args;
    }

    public Feature feature() {
        return requires;
    }

    @Override
    public JvmVersionRange range() {
        return requires.range();
    }

    public boolean isImmediate() {
        return length != null && length == 1;
    }

    public boolean isExternal() {
        return name().startsWith("asm_") || name().startsWith("opc_");
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

    private boolean isWideFormOf(JvmOp base) {
        return toString().equals(base.toString() + "_w");
    }

    public static JvmOp getOp(int code) {
        JvmOp result = code >= 0 && code < CODEMAP.length?CODEMAP[code]:null;
        Objects.nonNull(result);
        return result;
    }

    public static JvmOp getOp(String opstr) {
        return OPMAP.get(opstr);
    }
    
    public static JvmOp getWideOp(int code) {
        JvmOp jop = getOp(code);
        JvmOp result = OPMAP.get(jop.toString() + "_w");
        Objects.nonNull(result);
        return result;
    }
    
    private static JvmOp getOp(JvmOp jop, Object suffix, JvmVersion jvmversion) {
        JvmOp result = OPMAP.get(jop.toString() + "_" + suffix.toString());
        Objects.nonNull(result);
        jvmversion.checkSupports(result);
        return result;
    }
    
    public  static JvmOp exactVar(JvmOp jop, int v, JvmVersion jvmversion) {
        jop.checkArg(arg_var);
        if (v >= 0 && v <= 3 && jop != JvmOp.asm_ret) {
            return getOp(jop,v,jvmversion);
        } else if (!NumType.t_byte.isInUnsignedRange(v)) {
            return getOp(jop,'w',jvmversion);
        }
        return jop;
    }
    
    public  static JvmOp exactIncr(JvmOp jop, int v, int incr, JvmVersion jvmversion) {
        jop.checkArg(arg_incr);
        if (NumType.t_byte.isInUnsignedRange(v) && NumType.t_byte.isInRange(incr)) {
            return jop;
        }
        return getOp(jop,'w', jvmversion);
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

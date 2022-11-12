package jynx2asm.ops;

import java.util.EnumSet;
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

import asm.CheckOpcodes;
import jvm.Feature;
import jvm.JvmVersion;
import jvm.JvmVersionRange;
import jvm.NumType;
import jvm.OpArg;
import jynx.LogIllegalArgumentException;
import jynx2asm.NameDesc;

public enum JvmOp implements JynxOp {

    asm_aaload(50, 1, "(AI)A", arg_none, AALOAD),
    asm_aastore(83, 1, "(AIA)V", arg_none, AASTORE),
    asm_aconst_null(1, 1, "()A", arg_none, ACONST_NULL),
    asm_aload(25, 2, "()A", arg_var, ALOAD),
    asm_anewarray(189, 3, "(I)A", arg_class, ANEWARRAY),
    asm_areturn(176, 1, "(A)V", arg_none, ARETURN),
    asm_arraylength(190, 1, "(A)I", arg_none, ARRAYLENGTH),
    asm_astore(58, 2, "(A)V", arg_var, ASTORE),
    asm_athrow(191, 1, "(A)V", arg_none, ATHROW),
    asm_baload(51, 1, "(AI)I", arg_none, BALOAD),
    asm_bastore(84, 1, "(AII)V", arg_none, BASTORE),
    asm_bipush(16, 2, "()I", arg_byte, BIPUSH),
    asm_caload(52, 1, "(AI)I", arg_none, CALOAD),
    asm_castore(85, 1, "(AII)V", arg_none, CASTORE),
    asm_checkcast(192, 3, "(A)A", arg_class, CHECKCAST),
    asm_d2f(144, 1, "(D)F", arg_none, D2F),
    asm_d2i(142, 1, "(D)I", arg_none, D2I),
    asm_d2l(143, 1, "(D)J", arg_none, D2L),
    asm_dadd(99, 1, "(DD)D", arg_none, DADD),
    asm_daload(49, 1, "(AI)D", arg_none, DALOAD),
    asm_dastore(82, 1, "(AID)V", arg_none, DASTORE),
    asm_dcmpg(152, 1, "(DD)I", arg_none, DCMPG),
    asm_dcmpl(151, 1, "(DD)I", arg_none, DCMPL),
    asm_dconst_0(14, 1, "()D", arg_none, DCONST_0),
    asm_dconst_1(15, 1, "()D", arg_none, DCONST_1),
    asm_ddiv(111, 1, "(DD)D", arg_none, DDIV),
    asm_dload(24, 2, "()D", arg_var, DLOAD),
    asm_dmul(107, 1, "(DD)D", arg_none, DMUL),
    asm_dneg(119, 1, "(D)D", arg_none, DNEG),
    asm_drem(115, 1, "(DD)D", arg_none, DREM),
    asm_dreturn(175, 1, "(D)V", arg_none, DRETURN),
    asm_dstore(57, 2, "(D)V", arg_var, DSTORE),
    asm_dsub(103, 1, "(DD)D", arg_none, DSUB),
    asm_dup(89, 1, "t->tt", arg_stack, DUP),
    asm_dup2(92, 1, "T->TT", arg_stack, DUP2),
    asm_dup2_x1(93, 1, "nT->TnT", arg_stack, DUP2_X1),
    asm_dup2_x2(94, 1, "NT->TNT", arg_stack, DUP2_X2),
    asm_dup_x1(90, 1, "nt->tnt", arg_stack, DUP_X1),
    asm_dup_x2(91, 1, "Nt->tNt", arg_stack, DUP_X2),
    asm_f2d(141, 1, "(F)D", arg_none, F2D),
    asm_f2i(139, 1, "(F)I", arg_none, F2I),
    asm_f2l(140, 1, "(F)J", arg_none, F2L),
    asm_fadd(98, 1, "(FF)F", arg_none, FADD),
    asm_faload(48, 1, "(AI)F", arg_none, FALOAD),
    asm_fastore(81, 1, "(AIF)V", arg_none, FASTORE),
    asm_fcmpg(150, 1, "(FF)I", arg_none, FCMPG),
    asm_fcmpl(149, 1, "(FF)I", arg_none, FCMPL),
    asm_fconst_0(11, 1, "()F", arg_none, FCONST_0),
    asm_fconst_1(12, 1, "()F", arg_none, FCONST_1),
    asm_fconst_2(13, 1, "()F", arg_none, FCONST_2),
    asm_fdiv(110, 1, "(FF)F", arg_none, FDIV),
    asm_fload(23, 2, "()F", arg_var, FLOAD),
    asm_fmul(106, 1, "(FF)F", arg_none, FMUL),
    asm_fneg(118, 1, "(F)F", arg_none, FNEG),
    asm_frem(114, 1, "(FF)F", arg_none, FREM),
    asm_freturn(174, 1, "(F)V", arg_none, FRETURN),
    asm_fstore(56, 2, "(F)V", arg_var, FSTORE),
    asm_fsub(102, 1, "(FF)F", arg_none, FSUB),
    asm_getfield(180, 3, null, arg_field, GETFIELD),
    asm_getstatic(178, 3, null, arg_field, GETSTATIC),
    asm_goto(167, 3, "()V", arg_label, GOTO),
    asm_i2b(145, 1, "(I)I", arg_none, I2B),
    asm_i2c(146, 1, "(I)I", arg_none, I2C),
    asm_i2d(135, 1, "(I)D", arg_none, I2D),
    asm_i2f(134, 1, "(I)F", arg_none, I2F),
    asm_i2l(133, 1, "(I)J", arg_none, I2L),
    asm_i2s(147, 1, "(I)I", arg_none, I2S),
    asm_iadd(96, 1, "(II)I", arg_none, IADD),
    asm_iaload(46, 1, "(AI)I", arg_none, IALOAD),
    asm_iand(126, 1, "(II)I", arg_none, IAND),
    asm_iastore(79, 1, "(AII)V", arg_none, IASTORE),
    asm_iconst_0(3, 1, "()I", arg_none, ICONST_0),
    asm_iconst_1(4, 1, "()I", arg_none, ICONST_1),
    asm_iconst_2(5, 1, "()I", arg_none, ICONST_2),
    asm_iconst_3(6, 1, "()I", arg_none, ICONST_3),
    asm_iconst_4(7, 1, "()I", arg_none, ICONST_4),
    asm_iconst_5(8, 1, "()I", arg_none, ICONST_5),
    asm_iconst_m1(2, 1, "()I", arg_none, ICONST_M1),
    asm_idiv(108, 1, "(II)I", arg_none, IDIV),
    asm_if_acmpeq(165, 3, "(AA)V", arg_label, IF_ACMPEQ),
    asm_if_acmpne(166, 3, "(AA)V", arg_label, IF_ACMPNE),
    asm_if_icmpeq(159, 3, "(II)V", arg_label, IF_ICMPEQ),
    asm_if_icmpge(162, 3, "(II)V", arg_label, IF_ICMPGE),
    asm_if_icmpgt(163, 3, "(II)V", arg_label, IF_ICMPGT),
    asm_if_icmple(164, 3, "(II)V", arg_label, IF_ICMPLE),
    asm_if_icmplt(161, 3, "(II)V", arg_label, IF_ICMPLT),
    asm_if_icmpne(160, 3, "(II)V", arg_label, IF_ICMPNE),
    asm_ifeq(153, 3, "(I)V", arg_label, IFEQ),
    asm_ifge(156, 3, "(I)V", arg_label, IFGE),
    asm_ifgt(157, 3, "(I)V", arg_label, IFGT),
    asm_ifle(158, 3, "(I)V", arg_label, IFLE),
    asm_iflt(155, 3, "(I)V", arg_label, IFLT),
    asm_ifne(154, 3, "(I)V", arg_label, IFNE),
    asm_ifnonnull(199, 3, "(A)V", arg_label, IFNONNULL),
    asm_ifnull(198, 3, "(A)V", arg_label, IFNULL),
    asm_iinc(132, 3, "()V", arg_incr, IINC),
    asm_iload(21, 2, "()I", arg_var, ILOAD),
    asm_imul(104, 1, "(II)I", arg_none, IMUL),
    asm_ineg(116, 1, "(I)I", arg_none, INEG),
    asm_instanceof(193, 3, "(A)I", arg_class, INSTANCEOF),
    asm_invokedynamic(186, 5, null, arg_callsite, INVOKEDYNAMIC, Feature.invokeDynamic),
    asm_invokeinterface(185, 5, null, arg_interface, INVOKEINTERFACE),
    asm_invokespecial(183, 3, null, arg_method, INVOKESPECIAL, Feature.invokespecial),
    asm_invokestatic(184, 3, null, arg_method, INVOKESTATIC),
    asm_invokevirtual(182, 3, null, arg_method, INVOKEVIRTUAL),
    asm_ior(128, 1, "(II)I", arg_none, IOR),
    asm_irem(112, 1, "(II)I", arg_none, IREM),
    asm_ireturn(172, 1, "(I)V", arg_none, IRETURN),
    asm_ishl(120, 1, "(II)I", arg_none, ISHL),
    asm_ishr(122, 1, "(II)I", arg_none, ISHR),
    asm_istore(54, 2, "(I)V", arg_var, ISTORE),
    asm_isub(100, 1, "(II)I", arg_none, ISUB),
    asm_iushr(124, 1, "(II)I", arg_none, IUSHR),
    asm_ixor(130, 1, "(II)I", arg_none, IXOR),
    asm_jsr(168, 3, "()V", arg_label, JSR, Feature.subroutines),
    asm_l2d(138, 1, "(J)D", arg_none, L2D),
    asm_l2f(137, 1, "(J)F", arg_none, L2F),
    asm_l2i(136, 1, "(J)I", arg_none, L2I),
    asm_ladd(97, 1, "(JJ)J", arg_none, LADD),
    asm_laload(47, 1, "(AI)J", arg_none, LALOAD),
    asm_land(127, 1, "(JJ)J", arg_none, LAND),
    asm_lastore(80, 1, "(AIJ)V", arg_none, LASTORE),
    asm_lcmp(148, 1, "(JJ)I", arg_none, LCMP),
    asm_lconst_0(9, 1, "()J", arg_none, LCONST_0),
    asm_lconst_1(10, 1, "()J", arg_none, LCONST_1),
    asm_ldc(18, 2, null, arg_constant, LDC),
    asm_ldiv(109, 1, "(JJ)J", arg_none, LDIV),
    asm_lload(22, 2, "()J", arg_var, LLOAD),
    asm_lmul(105, 1, "(JJ)J", arg_none, LMUL),
    asm_lneg(117, 1, "(J)J", arg_none, LNEG),
    asm_lookupswitch(171, null, "(I)V", arg_lookupswitch, LOOKUPSWITCH),
    asm_lor(129, 1, "(JJ)J", arg_none, LOR),
    asm_lrem(113, 1, "(JJ)J", arg_none, LREM),
    asm_lreturn(173, 1, "(J)V", arg_none, LRETURN),
    asm_lshl(121, 1, "(JI)J", arg_none, LSHL),
    asm_lshr(123, 1, "(JI)J", arg_none, LSHR),
    asm_lstore(55, 2, "(J)V", arg_var, LSTORE),
    asm_lsub(101, 1, "(JJ)J", arg_none, LSUB),
    asm_lushr(125, 1, "(JI)J", arg_none, LUSHR),
    asm_lxor(131, 1, "(JJ)J", arg_none, LXOR),
    asm_monitorenter(194, 1, "(A)V", arg_none, MONITORENTER),
    asm_monitorexit(195, 1, "(A)V", arg_none, MONITOREXIT),
    asm_multianewarray(197, 4, null, arg_marray, MULTIANEWARRAY),
    asm_new(187, 3, "()A", arg_class, NEW),
    asm_newarray(188, 2, "(I)A", arg_atype, NEWARRAY),
    asm_nop(0, 1, "()V", arg_none, NOP),
    asm_pop(87, 1, "t->", arg_stack, POP),
    asm_pop2(88, 1, "T->", arg_stack, POP2),
    asm_putfield(181, 3, null, arg_field, PUTFIELD),
    asm_putstatic(179, 3, null, arg_field, PUTSTATIC),
    asm_ret(169, 2, "()V", arg_var, RET, Feature.subroutines),
    asm_return(177, 1, "()V", arg_none, RETURN),
    asm_saload(53, 1, "(AI)I", arg_none, SALOAD),
    asm_sastore(86, 1, "(AII)V", arg_none, SASTORE),
    asm_sipush(17, 3, "()I", arg_short, SIPUSH),
    asm_swap(95, 1, "nt->tn", arg_stack, SWAP),
    asm_tableswitch(170, null, "(I)V", arg_tableswitch, TABLESWITCH),

    opc_aload_0(42, 1, "()A", arg_var, ALOAD),
    opc_aload_1(43, 1, "()A", arg_var, ALOAD),
    opc_aload_2(44, 1, "()A", arg_var, ALOAD),
    opc_aload_3(45, 1, "()A", arg_var, ALOAD),
    opc_aload_w(25, 4, "()A", arg_var, ALOAD),
    opc_astore_0(75, 1, "(A)V", arg_var, ASTORE),
    opc_astore_1(76, 1, "(A)V", arg_var, ASTORE),
    opc_astore_2(77, 1, "(A)V", arg_var, ASTORE),
    opc_astore_3(78, 1, "(A)V", arg_var, ASTORE),
    opc_astore_w(58, 4, "(A)V", arg_var, ASTORE),
    opc_dload_0(38, 1, "()D", arg_var, DLOAD),
    opc_dload_1(39, 1, "()D", arg_var, DLOAD),
    opc_dload_2(40, 1, "()D", arg_var, DLOAD),
    opc_dload_3(41, 1, "()D", arg_var, DLOAD),
    opc_dload_w(24, 4, "()D", arg_var, DLOAD),
    opc_dstore_0(71, 1, "(D)V", arg_var, DSTORE),
    opc_dstore_1(72, 1, "(D)V", arg_var, DSTORE),
    opc_dstore_2(73, 1, "(D)V", arg_var, DSTORE),
    opc_dstore_3(74, 1, "(D)V", arg_var, DSTORE),
    opc_dstore_w(57, 4, "(D)V", arg_var, DSTORE),
    opc_fload_0(34, 1, "()F", arg_var, FLOAD),
    opc_fload_1(35, 1, "()F", arg_var, FLOAD),
    opc_fload_2(36, 1, "()F", arg_var, FLOAD),
    opc_fload_3(37, 1, "()F", arg_var, FLOAD),
    opc_fload_w(23, 4, "()F", arg_var, FLOAD),
    opc_fstore_0(67, 1, "(F)V", arg_var, FSTORE),
    opc_fstore_1(68, 1, "(F)V", arg_var, FSTORE),
    opc_fstore_2(69, 1, "(F)V", arg_var, FSTORE),
    opc_fstore_3(70, 1, "(F)V", arg_var, FSTORE),
    opc_fstore_w(56, 4, "(F)V", arg_var, FSTORE),
    opc_goto_w(200, 5, "()V", arg_label, GOTO),
    opc_iinc_w(132, 6, "()V", arg_incr, IINC),
    opc_iload_0(26, 1, "()I", arg_var, ILOAD),
    opc_iload_1(27, 1, "()I", arg_var, ILOAD),
    opc_iload_2(28, 1, "()I", arg_var, ILOAD),
    opc_iload_3(29, 1, "()I", arg_var, ILOAD),
    opc_iload_w(21, 4, "()I", arg_var, ILOAD),
    opc_invokenonvirtual(183, 3, null, arg_method, INVOKESPECIAL, Feature.invokenonvirtual),
    opc_istore_0(59, 1, "(I)V", arg_var, ISTORE),
    opc_istore_1(60, 1, "(I)V", arg_var, ISTORE),
    opc_istore_2(61, 1, "(I)V", arg_var, ISTORE),
    opc_istore_3(62, 1, "(I)V", arg_var, ISTORE),
    opc_istore_w(54, 4, "(I)V", arg_var, ISTORE),
    opc_jsr_w(201, 5, "()V", arg_label, JSR, Feature.subroutines),
    opc_ldc2_w(20, 3, null, arg_constant, LDC),
    opc_ldc_w(19, 3, null, arg_constant, LDC),
    opc_lload_0(30, 1, "()J", arg_var, LLOAD),
    opc_lload_1(31, 1, "()J", arg_var, LLOAD),
    opc_lload_2(32, 1, "()J", arg_var, LLOAD),
    opc_lload_3(33, 1, "()J", arg_var, LLOAD),
    opc_lload_w(22, 4, "()J", arg_var, LLOAD),
    opc_lstore_0(63, 1, "(J)V", arg_var, LSTORE),
    opc_lstore_1(64, 1, "(J)V", arg_var, LSTORE),
    opc_lstore_2(65, 1, "(J)V", arg_var, LSTORE),
    opc_lstore_3(66, 1, "(J)V", arg_var, LSTORE),
    opc_lstore_w(55, 4, "(J)V", arg_var, LSTORE),
    opc_ret_w(169, 4, "()V", arg_var, RET, Feature.subroutines),

    opc_switch(171, null, "(I)V", arg_lookupswitch, LOOKUPSWITCH),

    opc_wide(196, null, "()V", arg_none, NOP),

    xxx_label(-1, 0, "()V", arg_dir, -1),
    xxx_labelweak(-1, 0, "()V", arg_dir, -1),
    xxx_line(-2, 0, "()V", arg_dir, -2),
    ;
        
    private final int opcode;
    private final Integer length;
    private final String desc;
    private final OpArg args;
    private final int asmOpcode;
    private final Feature requires;

    private JvmOp(int opcode, Integer length, String desc, OpArg args, int asmOpcode) {
        this(opcode, length, desc, args, asmOpcode, Feature.unlimited);
    }

    private JvmOp(int opcode, Integer length, String desc, OpArg args, int asmOpcode, Feature requires) {
        this.opcode = opcode;
        this.length = length;
        this.asmOpcode = asmOpcode;
        this.args = args;
        this.requires = requires;
        assert desc == null // operand based stack change
                || args == arg_stack  && NameDesc.STACKOP_DESC.isValid(desc)
                || NameDesc.OP_DESC.isValid(desc):String.format("%s is invalid as op stack descriptor",desc);
        this.desc = desc;
    }

    private static final JvmOp[] CODEMAP = new JvmOp[256];
    private static final Map<String,JvmOp> OPMAP = new HashMap<>();

    static {
        
        boolean ok = true;
        String last = "";
        for (JvmOp op:values()) {
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
                        boolean sameargs = mapop.args == op.args;
                        boolean samevar = op.isWideFormOf(mapop)
                                && (op.args() == arg_var || op.args() == arg_incr);
                        boolean samefeature = Objects.equals(op.requires, mapop.requires);
                        boolean sameswitch = mapop == asm_lookupswitch && op == opc_switch;
                        
                        boolean validsame = sameargs && (samevar || sameswitch);
                        boolean nonvirtual = sameargs && mapop == asm_invokespecial && op == opc_invokenonvirtual;
                        
                        if (validsame && !samefeature) {
                            // "%s is null or has different feature requirement than %s"
                            throw new LogIllegalArgumentException(M302,op,mapop);
                        } else if (!validsame && !nonvirtual){
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

    @Override
    public Integer length() {
        return length;
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

    @Override
    public boolean isExternal() {
        return true;
    }

    public boolean isReturn() {
        return toString().contains("return");
    }
    
    public boolean isStack() {
        return args == arg_stack;
    }
    
    private static EnumSet<JvmOp> GO = EnumSet.of(asm_athrow, asm_goto,asm_lookupswitch,asm_ret,asm_tableswitch);
    
    public boolean isUnconditional() {
        return GO.contains(getOp(this.asmOpcode)) || isReturn();
    }
    
    public char vartype() {
        checkArg(arg_var);
        char ctype = Character.toUpperCase(toString().charAt(0));
        return ctype == 'L'?'J':ctype;
    }
    
    public boolean isStoreVar() {
        checkArg(arg_var);
        return toString().substring(1).startsWith("store");
    }
    
    public void checkArg(OpArg expected) {
        if (args != expected) {
            // "expected arg %s but was %s"
            throw new LogIllegalArgumentException(M362,expected,args);
        }
    }
    
    public String desc() {
        if (desc == null) {
            return null;
        }
        assert !desc.contains("->");
        return desc;
    }

    public String stackManipulate() {
        assert desc != null && desc.contains("->");
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
        System.out.println("XRETURN");
        Stream.of(values())
                .filter(JvmOp::isReturn)
                .forEach(System.out::println);
        System.out.println();
        System.out.println();
        System.out.println("GO or xRETRUN");
        Stream.of(values())
                .filter(JvmOp::isUnconditional)
                .forEach(System.out::println);
        System.out.println();
        System.out.println();
        System.out.println("STACK");
        Stream.of(values())
                .filter(JvmOp::isStack)
                .forEach(System.out::println);
        System.out.println();
        System.out.println();
        System.out.println("VARTYPE");
        Stream.of(values())
                .filter(op->op.args == arg_var)
                .forEach(op->System.out.format("%s '%c'%n", op.name(),op.vartype()));
    }

}

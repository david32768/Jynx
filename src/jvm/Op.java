package jvm;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static jvm.AsmOp.*;
import static jvm.OpArg.*;
import static jynx.Global.LOG;
import static jynx.Message.*;

import jynx.LogIllegalArgumentException;

public enum Op implements JvmOp {
    opc_aload_w(asm_aload, 25, 4),
    opc_aload_0(asm_aload, 42, 1),
    opc_aload_1(asm_aload, 43, 1),
    opc_aload_2(asm_aload, 44, 1),
    opc_aload_3(asm_aload, 45, 1),
    opc_astore_w(asm_astore, 58, 4),
    opc_astore_0(asm_astore, 75, 1),
    opc_astore_1(asm_astore, 76, 1),
    opc_astore_2(asm_astore, 77, 1),
    opc_astore_3(asm_astore, 78, 1),
    opc_dload_w(asm_dload, 24, 4),
    opc_dload_0(asm_dload, 38, 1),
    opc_dload_1(asm_dload, 39, 1),
    opc_dload_2(asm_dload, 40, 1),
    opc_dload_3(asm_dload, 41, 1),
    opc_dstore_w(asm_dstore, 57, 4),
    opc_dstore_0(asm_dstore, 71, 1),
    opc_dstore_1(asm_dstore, 72, 1),
    opc_dstore_2(asm_dstore, 73, 1),
    opc_dstore_3(asm_dstore, 74, 1),
    opc_fload_w(asm_fload, 23, 4),
    opc_fload_0(asm_fload, 34, 1),
    opc_fload_1(asm_fload, 35, 1),
    opc_fload_2(asm_fload, 36, 1),
    opc_fload_3(asm_fload, 37, 1),
    opc_fstore_w(asm_fstore, 56, 4),
    opc_fstore_0(asm_fstore, 67, 1),
    opc_fstore_1(asm_fstore, 68, 1),
    opc_fstore_2(asm_fstore, 69, 1),
    opc_fstore_3(asm_fstore, 70, 1),
    opc_goto_w(asm_goto, 200, 5),
    opc_iinc_w(asm_iinc, 132, 6),
    opc_iload_w(asm_iload, 21, 4),
    opc_iload_0(asm_iload, 26, 1),
    opc_iload_1(asm_iload, 27, 1),
    opc_iload_2(asm_iload, 28, 1),
    opc_iload_3(asm_iload, 29, 1),
    opc_invokenonvirtual(asm_invokespecial, 183, 3,Feature.invokenonvirtual),
    opc_istore_w(asm_istore, 54, 4),
    opc_istore_0(asm_istore, 59, 1),
    opc_istore_1(asm_istore, 60, 1),
    opc_istore_2(asm_istore, 61, 1),
    opc_istore_3(asm_istore, 62, 1),
    opc_jsr_w(asm_jsr, 201, 5),
    opc_ldc_w(asm_ldc, 19, 3),
    opc_ldc2_w(asm_ldc, 20, 3),
    opc_lload_w(asm_lload, 22, 4),
    opc_lload_0(asm_lload, 30, 1),
    opc_lload_1(asm_lload, 31, 1),
    opc_lload_2(asm_lload, 32, 1),
    opc_lload_3(asm_lload, 33, 1),
    opc_lstore_w(asm_lstore, 55, 4),
    opc_lstore_0(asm_lstore, 63, 1),
    opc_lstore_1(asm_lstore, 64, 1),
    opc_lstore_2(asm_lstore, 65, 1),
    opc_lstore_3(asm_lstore, 66, 1),
    opc_ret_w(asm_ret, 169, 4),

    opc_labelweak(xxx_label,-1,0),
    // not required - use _w suffix
    opc_wide(asm_nop, 196, null,Feature.unlimited), // feature specified to avoid assert in constructor

    ;

    private final int opcode;
    private final Feature requires;
    private final Integer length;
    private final AsmOp base;

    private Op(AsmOp base,int opcode,Integer length) {
        this.base = base;
        this.opcode = opcode;
        this.requires = base.feature();
        this.length = length;
        assert toString().startsWith(base.toString());
    }
    
    private Op(AsmOp base,int opcode,Integer length, Feature feature) {
        this.base = base;
        this.opcode = opcode;
        this.requires = feature;
        this.length = length;
    }
    
    private static final Map<String,JvmOp> OPMAP = new HashMap<>();

    static {
        JvmOp[] codemap = new JvmOp[256];
        for (AsmOp op : AsmOp.values()) {
            if (op.opcode() < 0) {
                continue;
            }
            codemap[op.opcode()] = op;
            OPMAP.putIfAbsent(op.toString(), op);
        }
        boolean ok = true;
        for (Op op:values()) {
            boolean opok = true;
            int opcode = op.opcode;
            if (opcode < 0) {
                continue;
            }
            JvmOp mapop = codemap[opcode];
            if (mapop == null) {
                codemap[opcode] = op;
                if (!Objects.equals(op.feature(), op.base.feature())) {
                    // "%s is null or has different feature requirement than %s"
                    throw new LogIllegalArgumentException(M302,op,op.base);
                }
            } else {
                boolean validsame = op.isWideFormOf(mapop)
                        && (op.getBase().args() == arg_var || op.getBase().args() == arg_incr);
                if (validsame && !Objects.equals(op.feature(), op.base.feature())) {
                    // "%s is null or has different feature requirement than %s"
                    throw new LogIllegalArgumentException(M302,op,op.base);
                }
                if (!validsame && mapop != asm_invokespecial && op != opc_invokenonvirtual){
                    LOG(M274,op,opcode,mapop); // "duplicate: %s has the same opcode(%d) as %s"
                    opok = false;
                }
            }
            if (opok) {
                JvmOp before = OPMAP.putIfAbsent(op.toString(), op);
                opok = before == null;
            }
            ok &= opok;
        }
        if (!ok) {
            LOG(M280); // "program terminated because of severe error(s)"
            System.exit(1);
        }
    }
    
    @Override
    public Feature feature() {
        return requires;
    }

    @Override
    public AsmOp getBase() {
        return base;
    }
    
    public static Stream<JvmOp> stream() {
        return OPMAP.values().stream();
    }
    
    private boolean isWideFormOf(JvmOp base) {
        return toString().equals(base.toString() + "_w");
    }

    @Override
    public Integer length() {
        return length;
    }

    @Override
    public String toString() {
        return name().substring(4);
    }
    
    private static JvmOp getOp(AsmOp jop, Object suffix, JvmVersion jvmversion) {
        JvmOp result = OPMAP.get(jop.toString() + "_" + suffix.toString());
        Objects.nonNull(result);
        jvmversion.checkSupports(result);
        return result;
    }
    
    public  static JvmOp exactVar(AsmOp jop, int v, JvmVersion jvmversion) {
        jop.checkArg(arg_var);
        if (v >= 0 && v <= 3 && jop != AsmOp.asm_ret) {
            return getOp(jop,v,jvmversion);
        } else if (!NumType.t_byte.isInUnsignedRange(v)) {
            return getOp(jop,'w',jvmversion);
        }
        return jop;
    }
    
    public  static JvmOp exactIncr(AsmOp jop, int v, int incr, JvmVersion jvmversion) {
        jop.checkArg(arg_incr);
        if (NumType.t_byte.isInUnsignedRange(v) && NumType.t_byte.isInRange(incr)) {
            return jop;
        }
        return getOp(jop,'w', jvmversion);
    }

}

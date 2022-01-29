package jynx2asm.ops;

import java.util.Arrays;
import java.util.stream.Stream;

import static jvm.AsmOp.*;


public enum ExtendedOps implements MacroOp {

    // callop not used

    ext_isignum(asm_i2l,asm_lconst_0,asm_lcmp),
    // extended stack ops; stack-opcode stack-opcode
    ext_swap2(asm_dup2_x2,asm_pop2),
    ext_swap21(asm_dup_x2,asm_pop),
    ext_swap12(asm_dup2_x1,asm_pop2),
    //
    ext_icmp(asm_i2l,ext_swap12,asm_i2l,asm_lcmp,asm_ineg),
    ext_irevcmp(asm_i2l,ext_swap12,asm_i2l,asm_lcmp),
    //
    ext_ireturn_0(asm_iconst_0,asm_ireturn),
    ext_ireturn_1(asm_iconst_1,asm_ireturn),
    ext_ireturn_m1(asm_iconst_m1,asm_ireturn),
    ext_zreturn_false(ext_ireturn_0),
    ext_zreturn_true(ext_ireturn_1),
    // extended if; (cmp opcode, if opcode)
    ext_if_lcmpeq(asm_lcmp, asm_ifeq),
    ext_if_lcmpge(asm_lcmp, asm_ifge),
    ext_if_lcmpgt(asm_lcmp, asm_ifgt),
    ext_if_lcmple(asm_lcmp, asm_ifle),
    ext_if_lcmplt(asm_lcmp, asm_iflt),
    ext_if_lcmpne(asm_lcmp, asm_ifne),

    ext_if_fcmpeq(asm_fcmpl, asm_ifeq),
    ext_if_fcmpge(asm_fcmpl, asm_ifge),
    ext_if_fcmpgt(asm_fcmpl, asm_ifgt),
    ext_if_fcmple(asm_fcmpg, asm_ifle),
    ext_if_fcmplt(asm_fcmpg, asm_iflt),
    ext_if_fcmpne(asm_fcmpl, asm_ifne),

    ext_if_dcmpeq(asm_dcmpl, asm_ifeq),
    ext_if_dcmpge(asm_dcmpl, asm_ifge),
    ext_if_dcmpgt(asm_dcmpl, asm_ifgt),
    ext_if_dcmple(asm_dcmpg, asm_ifle),
    ext_if_dcmplt(asm_dcmpg, asm_iflt),
    ext_if_dcmpne(asm_dcmpl, asm_ifne),

    ;
    
    private final JynxOp[] jynxOps;

    private ExtendedOps(JynxOp... jops) {
        this.jynxOps = jops;
    }

    @Override
    public JynxOp[] getJynxOps() {
        return jynxOps;
    }

    @Override
    public String toString() {
        return name().substring(4);
    }

    public static Stream<ExtendedOps> streamExternal() {
        return Arrays.stream(values())
            .filter(m->m.name().startsWith("ext_"));
    }
    
}

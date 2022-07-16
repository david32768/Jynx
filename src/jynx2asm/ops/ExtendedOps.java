package jynx2asm.ops;

import java.util.Arrays;
import java.util.stream.Stream;

import static jynx2asm.ops.JavaCallOps.*;
import static jynx2asm.ops.JvmOp.*;


public enum ExtendedOps implements MacroOp {

    ext_isignum(asm_i2l,asm_lconst_0,asm_lcmp),
    // extended stack ops; stack-opcode stack-opcode
    ext_swap2(asm_dup2_x2,asm_pop2),
    ext_swap21(asm_dup_x2,asm_pop),
    ext_swap12(asm_dup2_x1,asm_pop2),
    //
    ext_icmp(inv_icompare, ext_isignum),
    ext_iucmp(inv_iucompare, ext_isignum),
    ext_lucmp(inv_lucompare, ext_isignum),
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

    // unsigned comparisons
    ext_if_iucmpge(inv_iucompare, asm_ifge),
    ext_if_iucmpgt(inv_iucompare, asm_ifgt),
    ext_if_iucmple(inv_iucompare, asm_ifle),
    ext_if_iucmplt(inv_iucompare, asm_iflt),

    ext_if_lucmpge(inv_lucompare, asm_ifge),
    ext_if_lucmpgt(inv_lucompare, asm_ifgt),
    ext_if_lucmple(inv_lucompare, asm_ifle),
    ext_if_lucmplt(inv_lucompare, asm_iflt),

    // raw float
    xxx_fraw(asm_ldc,inv_iasf),
    xxx_draw(opc_ldc2_w,inv_lasd),
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

    @Override
    public boolean isExternal() {
        return name().startsWith("ext_");
    }

    public static Stream<ExtendedOps> streamExternal() {
        return Arrays.stream(values())
            .filter(ExtendedOps::isExternal);
    }
    
}

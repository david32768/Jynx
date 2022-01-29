package jynx2asm.ops;

import java.util.Arrays;
import java.util.stream.Stream;

import static jvm.AsmOp.*;
import static jvm.Op.opc_labelweak;
import static jynx2asm.ops.AliasOps.opc_xreturn;
import static jynx2asm.ops.ExtendedOps.*;
import static jynx2asm.ops.LineOps.*;


public enum StructuredOps implements MacroOp {

    // structured ops
    ext_BLOCK(push_mac_label),
    ext_LOOP(push_mac_label,lab_peek,xxx_label),
    ext_TRY(mac_label_try,xxx_catch, lab_peek_try,xxx_label),
    ext_RETURN(opc_xreturn),

    ext_ELSE(lab_else,asm_goto,lab_peek,xxx_label),
    ext_CATCH_ALL(lab_else,asm_goto,lab_peek,xxx_label),
    ext_END(lab_peek_else,opc_labelweak,lab_pop,opc_labelweak),

    ext_IF_NEZ(mac_label_else,asm_ifeq),
    ext_IF_EQZ(mac_label_else,asm_ifne),
    ext_IF_LTZ(mac_label_else,asm_ifge),
    ext_IF_LEZ(mac_label_else,asm_ifgt),
    ext_IF_GTZ(mac_label_else,asm_ifle),
    ext_IF_GEZ(mac_label_else,asm_iflt),

    ext_IF_ICMPNE(mac_label_else,asm_if_icmpeq),
    ext_IF_ICMPEQ(mac_label_else,asm_if_icmpne),
    ext_IF_ICMPLT(mac_label_else,asm_if_icmpge),
    ext_IF_ICMPLE(mac_label_else,asm_if_icmpgt),
    ext_IF_ICMPGT(mac_label_else,asm_if_icmple),
    ext_IF_ICMPGE(mac_label_else,asm_if_icmplt),

    ext_IF_LCMPNE(mac_label_else,ext_if_lcmpeq),
    ext_IF_LCMPEQ(mac_label_else,ext_if_lcmpne),
    ext_IF_LCMPLT(mac_label_else,ext_if_lcmpge),
    ext_IF_LCMPLE(mac_label_else,ext_if_lcmpgt),
    ext_IF_LCMPGT(mac_label_else,ext_if_lcmple),
    ext_IF_LCMPGE(mac_label_else,ext_if_lcmplt),

    ext_IF_FCMPNE(mac_label_else,ext_if_fcmpeq),
    ext_IF_FCMPEQ(mac_label_else,ext_if_fcmpne),
    ext_IF_FCMPLT(mac_label_else,ext_if_fcmpge),
    ext_IF_FCMPLE(mac_label_else,ext_if_fcmpgt),
    ext_IF_FCMPGT(mac_label_else,ext_if_fcmple),
    ext_IF_FCMPGE(mac_label_else,ext_if_fcmplt),

    ext_IF_DCMPNE(mac_label_else,ext_if_dcmpeq),
    ext_IF_DCMPEQ(mac_label_else,ext_if_dcmpne),
    ext_IF_DCMPLT(mac_label_else,ext_if_dcmpge),
    ext_IF_DCMPLE(mac_label_else,ext_if_dcmpgt),
    ext_IF_DCMPGT(mac_label_else,ext_if_dcmple),
    ext_IF_DCMPGE(mac_label_else,ext_if_dcmplt),

    ext_BR(lab_get,asm_goto),
    ext_BR_IFEQZ(lab_get,asm_ifeq),
    ext_BR_IFNEZ(lab_get,asm_ifne),
    ext_BR_IFLTZ(lab_get,asm_iflt),
    ext_BR_IFLEZ(lab_get,asm_ifle),
    ext_BR_IFGTZ(lab_get,asm_ifgt),
    ext_BR_IFGEZ(lab_get,asm_ifge),

    ext_BR_IF_ICMPEQ(lab_get,asm_if_icmpeq),
    ext_BR_IF_ICMPNE(lab_get,asm_if_icmpne),
    ext_BR_IF_ICMPLT(lab_get,asm_if_icmplt),
    ext_BR_IF_ICMPLE(lab_get,asm_if_icmple),
    ext_BR_IF_ICMPGT(lab_get,asm_if_icmpgt),
    ext_BR_IF_ICMPGE(lab_get,asm_if_icmpge),

    ext_BR_IF_LCMPEQ(lab_get,ext_if_lcmpeq),
    ext_BR_IF_LCMPNE(lab_get,ext_if_lcmpne),
    ext_BR_IF_LCMPLT(lab_get,ext_if_lcmplt),
    ext_BR_IF_LCMPLE(lab_get,ext_if_lcmple),
    ext_BR_IF_LCMPGT(lab_get,ext_if_lcmpgt),
    ext_BR_IF_LCMPGE(lab_get,ext_if_lcmpge),

    ext_BR_IF_FCMPEQ(lab_get,ext_if_fcmpeq),
    ext_BR_IF_FCMPNE(lab_get,ext_if_fcmpne),
    ext_BR_IF_FCMPLT(lab_get,ext_if_fcmplt),
    ext_BR_IF_FCMPLE(lab_get,ext_if_fcmple),
    ext_BR_IF_FCMPGT(lab_get,ext_if_fcmpgt),
    ext_BR_IF_FCMPGE(lab_get,ext_if_fcmpge),

    ext_BR_IF_DCMPEQ(lab_get,ext_if_dcmpeq),
    ext_BR_IF_DCMPNE(lab_get,ext_if_dcmpne),
    ext_BR_IF_DCMPLT(lab_get,ext_if_dcmplt),
    ext_BR_IF_DCMPLE(lab_get,ext_if_dcmple),
    ext_BR_IF_DCMPGT(lab_get,ext_if_dcmpgt),
    ext_BR_IF_DCMPGE(lab_get,ext_if_dcmpge),

    ;
    
    private final JynxOp[] jynxOps;

    private StructuredOps(JynxOp... jops) {
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

    public boolean reduceIndent() {
        switch(this) {
            case ext_ELSE:
            case ext_CATCH_ALL:
            case ext_END:
                return true;
            default:
                return false;
        }
    }
    
    public static Stream<StructuredOps> streamExternal() {
        return Arrays.stream(values())
            .filter(m->m.name().startsWith("ext_"));
    }
    
}

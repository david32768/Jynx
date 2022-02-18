package examples;

import java.lang.invoke.MethodHandle;
import java.util.Arrays;
import java.util.stream.Stream;

import org.objectweb.asm.Type;

import static jvm.AsmOp.*;
import static jynx2asm.ops.AliasOps.*;
import static jynx2asm.ops.ExtendedOps.*;
import static jynx2asm.ops.JavaCallOps.*;
import static jynx2asm.ops.LineOps.*;
import static jynx2asm.ops.StructuredOps.*;

import jynx2asm.ops.DynamicOp;
import jynx2asm.ops.JynxOp;
import jynx2asm.ops.LineOps;
import jynx2asm.ops.MacroLib;
import jynx2asm.ops.MacroOp;

public class WasmMacroLib  extends MacroLib {
    
        public final static String WASM_STORAGE = "wasmrun/Storage";
        public final static String WASM_STORAGE_L = "L" + WASM_STORAGE + ";";
        public final static String WASM_HELPER = "wasmrun/Helper";
        public final static String WASM_TABLE = "wasmrun/Table";
        public final static String WASM_TABLE_L =  "L" + WASM_TABLE + ";";
        public final static String WASM_DATA = "wasmrun/Data";
        public final static String MH_ARRAY = Type.getType(MethodHandle[].class).getInternalName();
        public final static String MH = "L" + Type.getType(MethodHandle.class).getInternalName() + ";";
        
        @Override
        public Stream<MacroOp> streamExternal() {
            return Arrays.stream(WasmOp.values())
                .filter(m-> Character.isUpperCase(m.toString().codePointAt(0)))
                .map(m->(MacroOp)m);
        }
        
        public static DynamicOp dynStorage(String method, String parms) {
            return DynamicOp.withBootParms(method, parms, WASM_STORAGE,
                "storageBootstrap",MH + "I","GS:MEMORY0()" + WASM_STORAGE_L);
        }
        
    private enum WasmOp implements MacroOp {

    aux_ilt(asm_iconst_m1,asm_iushr),
    // boolean result; top of stack must be one of (-1, 0, 1)
    aux_ine_m101(asm_iconst_1,asm_iand),
    aux_ieq_m101(aux_ine_m101,asm_iconst_1,asm_ixor),
    aux_ilt_m101(aux_ilt),
    aux_ile_m101(asm_iconst_1,asm_isub,aux_ilt),
    aux_igt_m101(asm_iconst_1,asm_iadd,asm_iconst_1,asm_iushr),
    aux_ige_m101(asm_ineg,aux_ile_m101),
    // boolean result 
    aux_ine(ext_isignum,aux_ine_m101),
    aux_ieq(ext_isignum,aux_ieq_m101),
    aux_ile(asm_i2l,asm_lconst_1,asm_lcmp,aux_ilt),
    aux_igt(asm_i2l,asm_lneg,asm_iconst_m1,asm_lushr,asm_l2i),
    aux_ige(aux_ilt,asm_iconst_1,asm_ixor),

    aux_newtable(LineOps.insert(WASM_TABLE,"getInstance","()" + WASM_TABLE_L),asm_invokestatic),
    aux_newmem(LineOps.insert(WASM_STORAGE,"getInstance","(II)" + WASM_STORAGE_L),asm_invokestatic),

    // init functions
    MEMORY_NEW(asm_ldc,asm_ldc,aux_newmem),
    MEMORY_CHECK(asm_ldc,asm_ldc,WasmMacroLib.dynStorage("checkIntance", "(II)V")),
    ADD_SEGMENT(asm_ldc,tok_swap,asm_ldc,WasmMacroLib.dynStorage("putBase64String", "(ILjava/lang/String;)V")),
    
    TABLE_NEW(aux_newtable),
    ADD_ENTRY(DynamicOp.withBootParms("add", "()V",
            WASM_TABLE, "handleBootstrap",MH + "II" + MH_ARRAY,"GS:TABLE0()" + WASM_TABLE_L)),
    
        // control operators
        UNREACHABLE(LineOps.insert(WASM_HELPER ,"unreachable","()Ljava/lang/AssertionError;"),asm_invokestatic,asm_athrow),
        IF(ext_IF_NEZ),
        BR_IF(ext_BR_IFNEZ),
        BR_TABLE(asm_tableswitch),
        CALL(asm_invokestatic),
//        CALL_INDIRECT(DynamicOp.of("table", null, WASM_TABLE, "callIndirectBootstrap")),
        CALL_INDIRECT(DynamicOp.withBootParms("table", null, WASM_TABLE,
                "callIndirectBootstrap",MH,"GS:TABLE0()" + WASM_TABLE_L)),
        // parametric operators
        NOP(asm_nop),
        DROP(opc_popn),
        SELECT(mac_label, asm_ifne, opc_dupn_xn, opc_popn, mac_label, xxx_label,  opc_popn),
        UNWIND(DynamicOp.of("unwind", null, WASM_HELPER, "unwindBootstrap")),
        // variable access
        LOCAL_GET(opc_xload_rel),
        LOCAL_SET(opc_xstore_rel),
        LOCAL_TEE(opc_dupn,opc_xstore_rel), // TEE pops and pushes value on stack
        GLOBAL_GET(asm_getstatic),
        GLOBAL_SET(asm_putstatic),

        // memory - args are alignment and offset(alignment currently ignored)
        I32_LOAD(tok_skip,WasmMacroLib.dynStorage("loadInt", "(I)I")),
        I64_LOAD(tok_skip,WasmMacroLib.dynStorage("loadLong", "(I)J")),
        F32_LOAD(tok_skip,WasmMacroLib.dynStorage("loadFloat", "(I)F")),
        F64_LOAD(tok_skip,WasmMacroLib.dynStorage("loadDouble", "(I)D")),

        I32_LOAD8_S(tok_skip,WasmMacroLib.dynStorage("loadByte", "(I)I")),
        I32_LOAD8_U(tok_skip,WasmMacroLib.dynStorage("loadUByte", "(I)I")),
        I32_LOAD16_S(tok_skip,WasmMacroLib.dynStorage("loadShort", "(I)I")),
        I32_LOAD16_U(tok_skip,WasmMacroLib.dynStorage("loadUShort", "(I)I")),

        I64_LOAD8_S(tok_skip,WasmMacroLib.dynStorage("loadByte2Long", "(I)J")),
        I64_LOAD8_U(tok_skip,WasmMacroLib.dynStorage("loadUByte2Long", "(I)J")),
        I64_LOAD16_S(tok_skip,WasmMacroLib.dynStorage("loadShort2Long", "(I)J")),
        I64_LOAD16_U(tok_skip,WasmMacroLib.dynStorage("loadUShort2Long", "(I)J")),
        I64_LOAD32_S(tok_skip,WasmMacroLib.dynStorage("loadInt2Long", "(I)J")),
        I64_LOAD32_U(tok_skip,WasmMacroLib.dynStorage("loadUInt2Long", "(I)J")),

        I32_STORE(tok_skip,WasmMacroLib.dynStorage("storeInt", "(II)V")),
        I64_STORE(tok_skip,WasmMacroLib.dynStorage("storeLong", "(IJ)V")),
        F32_STORE(tok_skip,WasmMacroLib.dynStorage("storeFloat", "(IF)V")),
        F64_STORE(tok_skip,WasmMacroLib.dynStorage("storeDouble", "(ID)V")),

        I32_STORE8(tok_skip,WasmMacroLib.dynStorage("storeByte", "(II)V")),
        I32_STORE16(tok_skip,WasmMacroLib.dynStorage("storeShort", "(II)V")),

        I64_STORE8(tok_skip,WasmMacroLib.dynStorage("storeLong2Byte", "(IJ)V")),
        I64_STORE16(tok_skip,WasmMacroLib.dynStorage("storeLong2Short", "(IJ)V")),
        I64_STORE32(tok_skip,WasmMacroLib.dynStorage("storeLong2Int", "(IJ)V")),


        // memory number is passed as 'disp' parameter
//    inv_current(LineOps.insert(WASM_STORAGE ,"current","(I)I"),asm_invokestatic),
//    inv_grow(LineOps.insert(WASM_STORAGE ,"grow","(II)I"),asm_invokestatic),
//    
//        MEMORY_SIZE(opc_ildc,inv_current),
//        MEMORY_GROW(opc_ildc,inv_grow),
//
        MEMORY_SIZE(WasmMacroLib.dynStorage("currentPages", "()I")),
        MEMORY_GROW(WasmMacroLib.dynStorage("grow", "(I)I")),

        // constants
        I32_CONST(opc_ildc),
        I64_CONST(opc_lldc),
        F32_CONST(opc_fldc),
        F64_CONST(opc_dldc),

        // comparison operators
            // call static method would be length 3
            // jump version would be shorter in some cases but extra stack attribute size
        I32_EQZ(asm_i2l,asm_lconst_0, asm_lcmp, aux_ieq_m101),
        I32_EQ(ext_irevcmp, aux_ieq_m101),
        I32_NE(ext_irevcmp, aux_ine_m101),
        I32_LT_S(ext_icmp, aux_ilt_m101),
        I32_LT_U(inv_iucompare, aux_ilt),
        I32_GT_S(ext_irevcmp, aux_ilt_m101),
        I32_GT_U(inv_iucompare, aux_igt),
        I32_LE_S(ext_irevcmp, aux_ige_m101),
        I32_LE_U(inv_iucompare, aux_ile),
        I32_GE_S(ext_irevcmp, aux_ile_m101),
        I32_GE_U(inv_iucompare, aux_ige),

        I64_EQZ(asm_lconst_0, asm_lcmp, aux_ieq_m101),
        I64_EQ(asm_lcmp, aux_ieq_m101),
        I64_NE(asm_lcmp, aux_ine_m101),
        I64_LT_S(asm_lcmp, aux_ilt_m101),
        I64_LT_U(inv_lucompare, aux_ilt),
        I64_GT_S(asm_lcmp, aux_igt_m101),
        I64_GT_U(inv_lucompare, aux_igt),
        I64_LE_S(asm_lcmp, aux_ile_m101),
        I64_LE_U(inv_lucompare, aux_ile),
        I64_GE_S(asm_lcmp, aux_ige_m101),
        I64_GE_U(inv_lucompare, aux_ige),

        F32_EQ(asm_fcmpl, aux_ieq_m101),
        F32_NE(asm_fcmpl, aux_ine_m101),
        F32_LT(asm_fcmpg, aux_ilt_m101),
        F32_GT(asm_fcmpl, aux_igt_m101),
        F32_LE(asm_fcmpg, aux_ile_m101),
        F32_GE(asm_fcmpl, aux_ige_m101),

        F64_EQ(asm_dcmpl, aux_ieq_m101),
        F64_NE(asm_dcmpl, aux_ine_m101),
        F64_LT(asm_dcmpg, aux_ilt_m101),
        F64_GT(asm_dcmpl, aux_igt_m101),
        F64_LE(asm_dcmpg, aux_ile_m101),
        F64_GE(asm_dcmpl, aux_ige_m101),

        // numeric operators
        I32_CLZ(inv_iclz),
        I32_CTZ(inv_ictz),
        I32_POPCNT(inv_ipopct),

        I32_ADD(asm_iadd),
        I32_SUB(asm_isub),
        I32_MUL(asm_imul),
        I32_DIV_S(asm_idiv),
        I32_DIV_U(inv_iudiv),
        I32_REM_S(asm_irem),
        I32_REM_U(inv_iurem),

        I32_AND(asm_iand),
        I32_OR(asm_ior),
        I32_XOR(asm_ixor),

        I32_SHL(asm_ishl),
        I32_SHR_S(asm_ishr),
        I32_SHR_U(asm_iushr),
        I32_ROTL(inv_irotl),
        I32_ROTR(inv_irotr),

        I64_CLZ(inv_lclz, asm_i2l),
        I64_CTZ(inv_lctz, asm_i2l),
        I64_POPCNT(inv_lpopct, asm_i2l),

        I64_ADD(asm_ladd),
        I64_SUB(asm_lsub),
        I64_MUL(asm_lmul),
        I64_DIV_S(asm_ldiv),
        I64_DIV_U(inv_ludiv),
        I64_REM_S(asm_lrem),
        I64_REM_U(inv_lurem),

        I64_AND(asm_land),
        I64_OR(asm_lor),
        I64_XOR(asm_lxor),

        I64_SHL(asm_l2i, asm_lshl),
        I64_SHR_S(asm_l2i, asm_lshr),
        I64_SHR_U(asm_l2i, asm_lushr),
        I64_ROTL(asm_l2i, inv_lrotl),
        I64_ROTR(asm_l2i, inv_lrotr),

        F32_ABS(inv_fabs),
        F32_NEG(asm_fneg),
        F32_CEIL(asm_f2d, inv_dceil, asm_d2f),
        F32_FLOOR(asm_f2d, inv_dfloor, asm_d2f),
        F32_TRUNC(LineOps.insert(WASM_HELPER ,"truncFloat","(F)F"), asm_invokestatic),
        F32_NEAREST(asm_f2d, inv_drint, asm_d2f),
        F32_SQRT(asm_f2d, inv_dsqrt, asm_d2f),

        F32_ADD(asm_fadd),
        F32_SUB(asm_fsub),
        F32_MUL(asm_fmul),
        F32_DIV(asm_fdiv),
        F32_MIN(inv_fmin),
        F32_MAX(inv_fmax),
        F32_COPYSIGN(inv_fcopysign),

        F64_ABS(inv_dabs),
        F64_NEG(asm_dneg),
        F64_CEIL(inv_dceil),
        F64_FLOOR(inv_dfloor),
        F64_TRUNC(LineOps.insert(WASM_HELPER ,"truncDouble","(D)D"),asm_invokestatic),
        F64_NEAREST(inv_drint),
        F64_SQRT(inv_dsqrt),

        F64_ADD(asm_dadd),
        F64_SUB(asm_dsub),
        F64_MUL(asm_dmul),
        F64_DIV(asm_ddiv),
        F64_MIN(inv_dmin),
        F64_MAX(inv_dmax),
        F64_COPYSIGN(inv_dcopysign),

        // conversions
        I32_WRAP_I64(asm_l2i),
        I32_TRUNC_S_F32(asm_f2i),
        I32_TRUNC_U_F32(LineOps.insert(WASM_HELPER ,"float2unsignedInt","(F)I"),asm_invokestatic),
        I32_TRUNC_S_F64(asm_d2i),
        I32_TRUNC_U_F64(LineOps.insert(WASM_HELPER ,"double2unsignedInt","(D)I"),asm_invokestatic),

        I64_EXTEND_S_I32(asm_i2l),
        I64_EXTEND_U_I32(inv_iu2l),
        I64_TRUNC_S_F32(asm_f2l),
        I64_TRUNC_U_F32(LineOps.insert(WASM_HELPER ,"float2unsignedLong","(F)J"),asm_invokestatic),
        I64_TRUNC_S_F64(asm_d2l),
        I64_TRUNC_U_F64(LineOps.insert(WASM_HELPER ,"double2unsignedLong","(D)J"),asm_invokestatic),

        F32_CONVERT_S_I32(asm_i2f),
        F32_CONVERT_U_I32(inv_iu2l,asm_l2f),
        F32_CONVERT_S_I64(asm_l2f),
        F32_CONVERT_U_I64(LineOps.insert(WASM_HELPER ,"unsignedLong2float","(J)F"),asm_invokestatic),
        F32_DEMOTE_F64(asm_d2f),

        F64_CONVERT_S_I32(asm_i2d),
        F64_CONVERT_U_I32(inv_iu2l,asm_l2d),
        F64_CONVERT_S_I64(asm_l2d),
        F64_CONVERT_U_I64(LineOps.insert(WASM_HELPER ,"unsignedLong2double","(J)D"),asm_invokestatic),
        F64_PROMOTE_F32(asm_f2d),
        // reinterpret
        I32_REINTERPRET_F32(inv_fasi),
        I64_REINTERPRET_F64(inv_dasl),
        F32_REINTERPRET_I32(inv_iasf),
        F64_REINTERPRET_I64(inv_lasd),

        // optimizations
        I32_IFEQZ(ext_IF_EQZ),
        I32_IFEQ(ext_IF_ICMPEQ),
        I32_IFNE(ext_IF_ICMPNE),
        I32_IFLT_S(ext_IF_ICMPLT),
        I32_IFLT_U(inv_iucompare, ext_IF_LTZ),
        I32_IFGT_S(ext_IF_ICMPGT),
        I32_IFGT_U(inv_iucompare, ext_IF_GTZ),
        I32_IFLE_S(ext_IF_ICMPLE),
        I32_IFLE_U(inv_iucompare, ext_IF_LEZ),
        I32_IFGE_S(ext_IF_ICMPGE),
        I32_IFGE_U(inv_iucompare, ext_IF_GEZ),

        I64_IFEQZ(asm_lconst_0, ext_IF_LCMPEQ),
        I64_IFEQ(ext_IF_LCMPEQ),
        I64_IFNE(ext_IF_LCMPNE),
        I64_IFLT_S(ext_IF_LCMPLT),
        I64_IFLT_U(inv_lucompare, ext_IF_LTZ),
        I64_IFGT_S(ext_IF_LCMPGT),
        I64_IFGT_U(inv_lucompare, ext_IF_GTZ),
        I64_IFLE_S(ext_IF_LCMPLE),
        I64_IFLE_U(inv_lucompare, ext_IF_LEZ),
        I64_IFGE_S(ext_IF_LCMPGE),
        I64_IFGE_U(inv_lucompare, ext_IF_GEZ),

        F32_IFEQ(ext_IF_FCMPEQ),
        F32_IFNE(ext_IF_FCMPNE),
        F32_IFLT(ext_IF_FCMPLT),
        F32_IFGT(ext_IF_FCMPGT),
        F32_IFLE(ext_IF_FCMPLE),
        F32_IFGE(ext_IF_FCMPGE),

        F64_IFEQ(ext_IF_DCMPEQ),
        F64_IFNE(ext_IF_DCMPNE),
        F64_IFLT(ext_IF_DCMPLT),
        F64_IFGT(ext_IF_DCMPGT),
        F64_IFLE(ext_IF_DCMPLE),
        F64_IFGE(ext_IF_DCMPGE),

        I32_BR_IFEQZ(ext_BR_IFEQZ),
        I32_BR_IFEQ(ext_BR_IF_ICMPEQ),
        I32_BR_IFNE(ext_BR_IF_ICMPNE),
        I32_BR_IFLT_S(ext_BR_IF_ICMPLT),
        I32_BR_IFLT_U(inv_iucompare, ext_BR_IFLTZ),
        I32_BR_IFGT_S(ext_BR_IF_ICMPGT),
        I32_BR_IFGT_U(inv_iucompare, ext_BR_IFGTZ),
        I32_BR_IFLE_S(ext_BR_IF_ICMPLE),
        I32_BR_IFLE_U(inv_iucompare, ext_BR_IFLEZ),
        I32_BR_IFGE_S(ext_BR_IF_ICMPGE),
        I32_BR_IFGE_U(inv_iucompare, ext_BR_IFGEZ),

        I64_BR_IFEQZ(asm_lconst_0, ext_BR_IF_LCMPEQ),
        I64_BR_IFEQ(ext_BR_IF_LCMPEQ),
        I64_BR_IFNE(ext_BR_IF_LCMPNE),
        I64_BR_IFLT_S(ext_BR_IF_LCMPLT),
        I64_BR_IFLT_U(inv_lucompare, ext_BR_IFLTZ),
        I64_BR_IFGT_S(ext_BR_IF_LCMPGT),
        I64_BR_IFGT_U(inv_lucompare, ext_BR_IFGTZ),
        I64_BR_IFLE_S(ext_BR_IF_LCMPLE),
        I64_BR_IFLE_U(inv_lucompare, ext_BR_IFLEZ),
        I64_BR_IFGE_S(ext_BR_IF_LCMPGE),
        I64_BR_IFGE_U(inv_lucompare, ext_BR_IFGEZ),

        F32_BR_IFEQ(ext_BR_IF_FCMPEQ),
        F32_BR_IFNE(ext_BR_IF_FCMPNE),
        F32_BR_IFLT(ext_BR_IF_FCMPLT),
        F32_BR_IFGT(ext_BR_IF_FCMPGT),
        F32_BR_IFLE(ext_BR_IF_FCMPLE),
        F32_BR_IFGE(ext_BR_IF_FCMPGE),

        F64_BR_IFEQ(ext_BR_IF_DCMPEQ),
        F64_BR_IFNE(ext_BR_IF_DCMPNE),
        F64_BR_IFLT(ext_BR_IF_DCMPLT),
        F64_BR_IFGT(ext_BR_IF_DCMPGT),
        F64_BR_IFLE(ext_BR_IF_DCMPLE),
        F64_BR_IFGE(ext_BR_IF_DCMPGE),

        ;

        private final JynxOp[] jynxOps;

        private WasmOp(JynxOp... jops) {
            this.jynxOps = jops;
        }

        @Override
        public JynxOp[] getJynxOps() {
            return jynxOps;
        }

    }
}

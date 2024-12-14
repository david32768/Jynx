package jvm;

import java.util.EnumSet;

import static jvm.JvmVersion.*;

public enum Feature implements JvmVersioned {

    never(NEVER,NEVER),
    unlimited(MIN_VERSION,NEVER),
    
    invokenonvirtual(MIN_VERSION,V1_0_2),

    invokespecial(V1_0_2),
    superflag(V1_0_2),
    finalize(V1_0_2,V9,NEVER),
    v1_0_2(V1_0_2),
    
    deprecated(V1_1),
    inner_classes(V1_1),
    signature(V1_1),
    synthetic(V1_1), // synthetic attribute in [V1_1, V1_4], real acc flag from V1_5 
    synthetic_attribute(V1_1, V1_5),
    
    fpstrict(V1_2,V17), // access flag
    strictfp_rw(V1_2,NEVER), // rw = RESERVED WORD
    
    V3methods(V1_3),

    assertions(V1_4),
    
    enums(V1_5),
    annotations(V1_5),
    package_info(V1_5), // javac uses this version
    bitops(V1_5),
    V5methods(V1_5),
    enclosing_method(V1_5),
    source_debug(V1_5),
    local_variable_type_table(V1_5),
    bridge(V1_5),
    varargs(V1_5),

    stackmap(V1_6),
    subroutines(MIN_VERSION,V1_6), // jvms 4.9.1
    V6methods(V1_6),
    
    invokeDynamic(V1_7),
    V7methods(V1_7),

    type_annotations(V1_8),
    invokespecial_interface(V1_8),
    invokestatic_interface(V1_8),
    unsigned(V1_8),
    method_parameters(V1_8),
    mandated(V1_8),

    modules(V9),
    static_phase_transitive(V9,V10),   // not >= V10 unless java.base module
    underline(V9),

    var_type(V10),
    
    nests(V11),
    constant_dynamic(V11),
    
    typedesc(V12), // invokedynamic third parameter is Ljava/lang/invoke/TypeDescriptor;
    switch_expression(EnumSet.of(V12_PREVIEW), V13, NEVER, NEVER),
    
    record(EnumSet.of(V14_PREVIEW, V15_PREVIEW), V16, NEVER, NEVER),
    
    sealed(EnumSet.of(V15_PREVIEW, V16_PREVIEW), V17, NEVER, NEVER),
    
    valhalla(V23,V24),
    value(EnumSet.of(V23_PREVIEW),NEVER,NEVER,NEVER),
    ;
    
    private final JvmVersionRange jvmRange;

    private Feature(JvmVersion start) {
        this(start, NEVER);
    }
    
    private Feature(JvmVersion start, JvmVersion end) {
        this(EnumSet.noneOf(JvmVersion.class), start, end, end);
    }

    private Feature(JvmVersion start, JvmVersion deprecate, JvmVersion end) {
        this(EnumSet.noneOf(JvmVersion.class), start, deprecate, end);
    }

    private Feature(EnumSet<JvmVersion> preview, JvmVersion start, JvmVersion deprecate, JvmVersion end) {
        this.jvmRange = new JvmVersionRange(preview, start, deprecate, end);
    }

    @Override
    public JvmVersionRange range() {
        return jvmRange;
    }
    
    
    @Override
    public String toString() {
        return name().toUpperCase();
    }
}
    


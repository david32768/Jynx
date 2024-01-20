package jvm;

import java.util.Objects;

import static jvm.JvmVersion.*;

public enum Feature implements JvmVersioned {

    never(MIN_VERSION,MIN_VERSION),
    unlimited(MIN_VERSION,NEVER),
    
    invokenonvirtual(MIN_VERSION,V1_0_2),

    invokespecial(V1_0_2),
    superflag(V1_0_2),
    finalize(V1_0_2,V1_0_2,V9,NEVER),
    v1_0_2(V1_0_2),
    
    deprecated(V1_1),
    inner_classes(V1_1),
    signature(V1_1),
    synthetic(V1_1), // synthetic attribute in [V1_1, V1_4], real acc flag from V1_5 
    synthetic_attribute(V1_1, V1_5),
    
    fpstrict(V1_2,V17),
    strictfp_rw(V1_2,NEVER),
    
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
    switch_expression(V12_PREVIEW,V13,NEVER,NEVER),
    
    record(V14_PREVIEW,V16,NEVER, NEVER),
    
    sealed(V15_PREVIEW,V17,NEVER,NEVER),
    ;
    
    private final JvmVersionRange jvmRange;

    private Feature(JvmVersion start) {
        this(start,start,NEVER,NEVER);
    }
    
    private Feature(JvmVersion start,JvmVersion end) {
        this(start,start,end,end);
    }

    private Feature(JvmVersion preview, JvmVersion start, JvmVersion deprecate, JvmVersion end) {
        Objects.nonNull(preview);
        Objects.nonNull(start);
        Objects.nonNull(deprecate);
        Objects.nonNull(end);
        assert start.compareTo(end) <= 0;
        assert preview.compareTo(start) < 0 && preview.isPreview() || preview.equals(start);
        assert deprecate.compareTo(start) >= 0 && deprecate.compareTo(end) <= 0;
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
    


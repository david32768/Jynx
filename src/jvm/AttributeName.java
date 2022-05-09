package jvm;

import java.util.EnumSet;
import java.util.stream.Stream;

import static jvm.Context.*;

public enum AttributeName implements JvmVersioned {

    // jvms Table 4.7B - attributes
    ConstantValue(Feature.v1_0_2,EnumSet.of(FIELD)),
    Code(Feature.v1_0_2,EnumSet.of(METHOD)),
    StackMapTable(Feature.stackmap,EnumSet.of(CODE)),
    Exceptions(Feature.v1_0_2,EnumSet.of(METHOD)),
    InnerClasses(Feature.inner_classes,EnumSet.of(CLASS)),
    EnclosingMethod(Feature.enclosing_method, EnumSet.of(CLASS)),
    Synthetic(Feature.synthetic,EnumSet.of(CLASS,FIELD,METHOD)),
    Signature(Feature.signature,EnumSet.of(CLASS,FIELD,METHOD)),
    SourceFile(Feature.v1_0_2,EnumSet.of(CLASS,MODULE)),
    SourceDebugExtension(Feature.source_debug,EnumSet.of(CLASS)),
    LineNumberTable(Feature.v1_0_2,EnumSet.of(CODE)),
    LocalVariableTable(Feature.v1_0_2,EnumSet.of(CODE)),
    LocalVariableTypeTable(Feature.local_variable_type_table,EnumSet.of(CODE)),
    Deprecated(Feature.deprecated,EnumSet.of(CLASS,FIELD,METHOD)),
    RuntimeVisibleAnnotations(Feature.annotations,EnumSet.of(CLASS,FIELD,METHOD)),
    RuntimeInvisibleAnnotations(Feature.annotations,EnumSet.of(CLASS,FIELD,METHOD)),
    RuntimeVisibleParameterAnnotations(Feature.annotations,EnumSet.of(METHOD)),
    RuntimeInvisibleParameterAnnotations(Feature.annotations,EnumSet.of(METHOD)),
    RuntimeVisibleTypeAnnotations(Feature.type_annotations,EnumSet.of(CLASS,FIELD,METHOD,CODE)),
    RuntimeInvisibleTypeAnnotations(Feature.type_annotations,EnumSet.of(CLASS,FIELD,METHOD,CODE)),
    AnnotationDefault(Feature.annotations,EnumSet.of(METHOD)),
    BootstrapMethods(Feature.invokeDynamic,EnumSet.of(CLASS)),
    MethodParameters(Feature.method_parameters,EnumSet.of(METHOD)),
    Module(Feature.modules,EnumSet.of(MODULE)),
    ModulePackages(Feature.modules,EnumSet.of(MODULE)),
    ModuleMainClass(Feature.modules,EnumSet.of(MODULE)),
    NestHost(Feature.nests,EnumSet.of(CLASS)),
    NestMembers(Feature.nests,EnumSet.of(CLASS)),
    Record(Feature.record,EnumSet.of(CLASS)),
    PermittedSubclasses(Feature.sealed,EnumSet.of(CLASS)),
    // end Table 4.7B
    ;
    
    private final Feature feature;
    private final EnumSet<Context> where;

    private AttributeName(Feature feature, EnumSet<Context> where) {
        this.feature = feature;
        this.where = where;
    }


    @Override
    public JvmVersionRange range() {
        return feature.range();
    }

    public boolean inContext(Context context) {
        return where.contains(context);
    }

    public static AttributeName getInstance(String attrname) {
        return Stream.of(values())
                .filter(s->s.name().equals(attrname))
                .findFirst()
                .orElse(null);
    }
    
    @Override
    public String toString() {
        return name();
    }
    
}

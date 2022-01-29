package jvm;

import java.util.EnumSet;
import java.util.Objects;
import java.util.stream.Stream;

import static jvm.AttributeType.*;
import static jvm.Context.*;

public enum AttributeName implements JvmVersioned {

    // jvms Table 4.7B - attributes
    ConstantValue(Feature.v1_0_2,EnumSet.of(FIELD),FIXED,2),
    Code(Feature.v1_0_2,EnumSet.of(METHOD)),
    StackMapTable(Feature.stackmap,EnumSet.of(CODE)),
    Exceptions(Feature.v1_0_2,EnumSet.of(METHOD),ARRAY,2),
    InnerClasses(Feature.inner_classes,EnumSet.of(CLASS),ARRAY,8),
    EnclosingMethod(Feature.enclosing_method, EnumSet.of(CLASS),FIXED,4),
    Synthetic(Feature.synthetic,EnumSet.of(CLASS,FIELD,METHOD),FIXED,0),
    Signature(Feature.signature,EnumSet.of(CLASS,FIELD,METHOD),FIXED,2),
    SourceFile(Feature.v1_0_2,EnumSet.of(CLASS,MODULE),FIXED,2),
    SourceDebugExtension(Feature.source_debug,EnumSet.of(CLASS)),
    LineNumberTable(Feature.v1_0_2,EnumSet.of(CODE),ARRAY,4),
    LocalVariableTable(Feature.v1_0_2,EnumSet.of(CODE),ARRAY,10),
    LocalVariableTypeTable(Feature.local_variable_type_table,EnumSet.of(CODE),ARRAY,10),
    Deprecated(Feature.deprecated,EnumSet.of(CLASS,FIELD,METHOD),FIXED,0),
    RuntimeVisibleAnnotations(Feature.annotations,EnumSet.of(CLASS,FIELD,METHOD)),
    RuntimeInvisibleAnnotations(Feature.annotations,EnumSet.of(CLASS,FIELD,METHOD)),
    RuntimeVisibleParameterAnnotations(Feature.annotations,EnumSet.of(METHOD)),
    RuntimeInvisibleParameterAnnotations(Feature.annotations,EnumSet.of(METHOD)),
    RuntimeVisibleTypeAnnotations(Feature.type_annotations,EnumSet.of(CLASS,FIELD,METHOD,CODE)),
    RuntimeInvisibleTypeAnnotations(Feature.type_annotations,EnumSet.of(CLASS,FIELD,METHOD,CODE)),
    AnnotationDefault(Feature.annotations,EnumSet.of(METHOD)),
    BootstrapMethods(Feature.invokeDynamic,EnumSet.of(CLASS)),
    MethodParameters(Feature.method_parameters,EnumSet.of(METHOD)), // array countt is u1 not u2
    Module(Feature.modules,EnumSet.of(MODULE)),
    ModulePackages(Feature.modules,EnumSet.of(MODULE),ARRAY,2),
    ModuleMainClass(Feature.modules,EnumSet.of(MODULE),FIXED,2),
    NestHost(Feature.nests,EnumSet.of(CLASS),FIXED,2),
    NestMembers(Feature.nests,EnumSet.of(CLASS),ARRAY,2),
    Record(Feature.record,EnumSet.of(CLASS)),
    PermittedSubclasses(Feature.sealed,EnumSet.of(CLASS),ARRAY,2),
    // end Table 4.7B
    ;
    
    private final Feature feature;
    private final EnumSet<Context> where;
    private final AttributeType type;
    private final Integer length;

    private AttributeName(Feature feature, EnumSet<Context> where) {
        this(feature,where,VARIABLE,null);
    }
    
    private AttributeName(Feature feature, EnumSet<Context> where, AttributeType type, Integer length) {
        this.feature = feature;
        this.where = where;
        this.type = type;
        this.length = length;
        assert type == VARIABLE ^ length != null;
    }


    public Feature feature() {
        return feature;
    }

    @Override
    public JvmVersionRange range() {
        return feature.range();
    }

    public AttributeType getType() {
        return type;
    }

    public int getLength() {
        Objects.nonNull(length);
        return length;
    }

    public boolean inContext(Context context) {
        return where.contains(context);
    }
    
    public static boolean isKnownAttribute(String attrname) {
        if (attrname.isEmpty() || !Character.isUpperCase(attrname.charAt(0))) {
            return false;
        }
        return Stream.of(values())
                .filter(s->s.name().equals(attrname))
                .findFirst()
                .isPresent();
    }
    
    @Override
    public String toString() {
        return name();
    }
}

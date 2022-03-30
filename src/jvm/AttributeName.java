package jvm;

import java.util.EnumSet;
import java.util.Objects;
import java.util.stream.Stream;

import static jvm.AttributeEntry.*;
import static jvm.AttributeType.*;
import static jvm.Context.*;

public enum AttributeName implements JvmVersioned {

    // jvms Table 4.7B - attributes
    ConstantValue(Feature.v1_0_2,EnumSet.of(FIELD),FIXED,CONSTANT),
    Code(Feature.v1_0_2,EnumSet.of(METHOD)),
    StackMapTable(Feature.stackmap,EnumSet.of(CODE)),
    Exceptions(Feature.v1_0_2,EnumSet.of(METHOD),ARRAY,CLASSNAME),
    InnerClasses(Feature.inner_classes,EnumSet.of(CLASS),ARRAY,CLASSNAME,OPT_CLASSNAME,OPT_UTF8,ACCESS),
    EnclosingMethod(Feature.enclosing_method, EnumSet.of(CLASS),FIXED,CLASSNAME,OPT_NAME_TYPE),
    Synthetic(Feature.synthetic,EnumSet.of(CLASS,FIELD,METHOD),FIXED),
    Signature(Feature.signature,EnumSet.of(CLASS,FIELD,METHOD),FIXED,UTF8),
    SourceFile(Feature.v1_0_2,EnumSet.of(CLASS,MODULE),FIXED,UTF8),
    SourceDebugExtension(Feature.source_debug,EnumSet.of(CLASS)),
    LineNumberTable(Feature.v1_0_2,EnumSet.of(CODE),ARRAY,LABEL,USHORT),
    LocalVariableTable(Feature.v1_0_2,EnumSet.of(CODE),ARRAY,LABEL,USHORT,UTF8,UTF8,USHORT),
    LocalVariableTypeTable(Feature.local_variable_type_table,EnumSet.of(CODE),
            ARRAY,LABEL,USHORT,UTF8,UTF8,USHORT),
    Deprecated(Feature.deprecated,EnumSet.of(CLASS,FIELD,METHOD),FIXED),
    RuntimeVisibleAnnotations(Feature.annotations,EnumSet.of(CLASS,FIELD,METHOD)),
    RuntimeInvisibleAnnotations(Feature.annotations,EnumSet.of(CLASS,FIELD,METHOD)),
    RuntimeVisibleParameterAnnotations(Feature.annotations,EnumSet.of(METHOD)),
    RuntimeInvisibleParameterAnnotations(Feature.annotations,EnumSet.of(METHOD)),
    RuntimeVisibleTypeAnnotations(Feature.type_annotations,EnumSet.of(CLASS,FIELD,METHOD,CODE)),
    RuntimeInvisibleTypeAnnotations(Feature.type_annotations,EnumSet.of(CLASS,FIELD,METHOD,CODE)),
    AnnotationDefault(Feature.annotations,EnumSet.of(METHOD)),
    BootstrapMethods(Feature.invokeDynamic,EnumSet.of(CLASS)),
    MethodParameters(Feature.method_parameters,EnumSet.of(METHOD),ARRAY1,OPT_UTF8,ACCESS),
    Module(Feature.modules,EnumSet.of(MODULE)),
    ModulePackages(Feature.modules,EnumSet.of(MODULE),ARRAY,PACKAGENAME),
    ModuleMainClass(Feature.modules,EnumSet.of(MODULE),FIXED,CLASSNAME),
    NestHost(Feature.nests,EnumSet.of(CLASS),FIXED,CLASSNAME),
    NestMembers(Feature.nests,EnumSet.of(CLASS),ARRAY,CLASSNAME),
    Record(Feature.record,EnumSet.of(CLASS)),
    PermittedSubclasses(Feature.sealed,EnumSet.of(CLASS),ARRAY,CLASSNAME),
    // end Table 4.7B
    ;
    
    private final Feature feature;
    private final EnumSet<Context> where;
    private final AttributeType type;
    private final AttributeEntry[] entries;

    private AttributeName(Feature feature, EnumSet<Context> where) {
        this(feature,where,VARIABLE,(AttributeEntry[])null);
    }
    
    private AttributeName(Feature feature, EnumSet<Context> where, AttributeType type, AttributeEntry... entries) {
        this.feature = feature;
        this.where = where;
        this.type = type;
        this.entries = entries;
        assert type == VARIABLE ^ entries != null;
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

    public AttributeEntry[] getEntries() {
        Objects.nonNull(entries);
        return entries;
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

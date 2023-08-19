package jvm;

import java.util.EnumSet;
import java.util.stream.Stream;

import static jvm.AttributeEntry.*;
import static jvm.AttributeType.ARRAY;
import static jvm.AttributeType.ARRAY1;
import static jvm.AttributeType.FIXED;
import static jvm.Context.*;

public enum StandardAttribute implements Attribute {

    // jvms Table 4.7B - attributes
    ConstantValue(Feature.v1_0_2,EnumSet.of(FIELD),FIXED,CONSTANT),
    Code(Feature.v1_0_2,EnumSet.of(METHOD),  AttributeType.CODE),
    StackMapTable(Feature.stackmap,EnumSet.of(CODE),ARRAY,FRAME),
    Exceptions(Feature.v1_0_2,EnumSet.of(METHOD),ARRAY,CLASSNAME),
    InnerClasses(Feature.inner_classes,EnumSet.of(CLASS),ARRAY,CLASSNAME,OPT_CLASSNAME,OPT_UTF8,ACCESS),
    EnclosingMethod(Feature.enclosing_method, EnumSet.of(CLASS),FIXED,CLASSNAME,OPT_NAME_TYPE),
    Synthetic(false,Feature.synthetic,EnumSet.of(CLASS,FIELD,METHOD),FIXED),
    Signature(Feature.signature,EnumSet.of(CLASS,FIELD,METHOD,COMPONENT),FIXED,UTF8),
    SourceFile(Feature.v1_0_2,EnumSet.of(CLASS,MODULE),FIXED,UTF8),
    SourceDebugExtension(Feature.source_debug,EnumSet.of(CLASS),FIXED,INLINE_UTF8),
    LineNumberTable(false,Feature.v1_0_2,EnumSet.of(CODE),ARRAY,LABEL,USHORT),
    LocalVariableTable(false,Feature.v1_0_2,EnumSet.of(CODE),ARRAY,LABEL_LENGTH,UTF8,UTF8,LV_INDEX),
    LocalVariableTypeTable(false,Feature.local_variable_type_table,EnumSet.of(CODE),
            ARRAY,LABEL_LENGTH,UTF8,UTF8,LV_INDEX),
    Deprecated(false,Feature.deprecated,EnumSet.of(CLASS,FIELD,METHOD),FIXED),
    RuntimeVisibleAnnotations(Feature.annotations,EnumSet.of(CLASS,FIELD,METHOD,COMPONENT),ARRAY, AttributeEntry.ANNOTATION),
    RuntimeInvisibleAnnotations(Feature.annotations,EnumSet.of(CLASS,FIELD,METHOD,COMPONENT),ARRAY, AttributeEntry.ANNOTATION),
    RuntimeVisibleParameterAnnotations(Feature.annotations,EnumSet.of(METHOD),ARRAY1, PARAMETER_ANNOTATION),
    RuntimeInvisibleParameterAnnotations(Feature.annotations,EnumSet.of(METHOD),ARRAY1, PARAMETER_ANNOTATION),
    RuntimeVisibleTypeAnnotations(Feature.type_annotations,EnumSet.of(CLASS,FIELD,METHOD,CODE,COMPONENT),ARRAY, TYPE_ANNOTATION),
    RuntimeInvisibleTypeAnnotations(Feature.type_annotations,EnumSet.of(CLASS,FIELD,METHOD,CODE,COMPONENT),ARRAY, TYPE_ANNOTATION),
    AnnotationDefault(Feature.annotations,EnumSet.of(METHOD),FIXED, AttributeEntry.DEFAULT_ANNOTATION),
    BootstrapMethods(Feature.invokeDynamic,EnumSet.of(CLASS),ARRAY,BOOTSTRAP),
    MethodParameters(Feature.method_parameters,EnumSet.of(METHOD),ARRAY1,OPT_UTF8,ACCESS),
    Module(Feature.modules,EnumSet.of(MODULE), AttributeType.MODULE),
    ModulePackages(Feature.modules,EnumSet.of(MODULE),ARRAY,PACKAGENAME),
    ModuleMainClass(Feature.modules,EnumSet.of(MODULE),FIXED,CLASSNAME),
    NestHost(Feature.nests,EnumSet.of(CLASS),FIXED,CLASSNAME),
    NestMembers(Feature.nests,EnumSet.of(CLASS),ARRAY,CLASSNAME),
    Record(Feature.record,EnumSet.of(CLASS),  AttributeType.RECORD),
    PermittedSubclasses(Feature.sealed,EnumSet.of(CLASS),ARRAY,CLASSNAME),
    // end Table 4.7B
    ;
    
    private final boolean unique;
    private final Feature feature;
    private final EnumSet<Context> where;

    private final AttributeType type;
    private final AttributeEntry[] cpentries;
    
    private StandardAttribute(Feature feature, EnumSet<Context> where, AttributeType type, AttributeEntry... entries) {
        this(true, feature, where, type, entries);
    }
    
    private StandardAttribute(boolean unique,Feature feature, EnumSet<Context> where, AttributeType type, AttributeEntry... entries) {
        this.unique = unique;
        this.feature = feature;
        this.where = where;
        this.type = type;
        this.cpentries = entries;
    }

    @Override
    public JvmVersionRange range() {
        return feature.range();
    }

    @Override
    public boolean inContext(Context context) {
        return where.contains(context);
    }

    @Override
    public AttributeType type() {
        return type;
    }

    @Override
    public AttributeEntry[] entries() {
        return cpentries.clone();
    }

    @Override
    public boolean isUnique() {
        return unique;
    }
    
    public static StandardAttribute getInstance(String attrname) {
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

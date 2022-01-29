package jvm;

import java.util.Arrays;
import java.util.Optional;

import static jvm.ConstantPoolType.EntryType.*;
import static jvm.JvmVersion.*;
import static jynx.Global.LOG;
import static jynx.Message.*;

public enum ConstantPoolType implements JvmVersioned {
    // Table 4.4B; and 4.4C
    CONSTANT_Utf8(1,Feature.v1_0_2,null,UTF8),
    CONSTANT_Integer(3,Feature.v1_0_2,V1_0_2,INTEGER),
    CONSTANT_Float(4,Feature.v1_0_2,V1_0_2,FLOAT),
    CONSTANT_Long(5,Feature.v1_0_2,V1_0_2,LONG),
    CONSTANT_Double(6,Feature.v1_0_2,V1_0_2,DOUBLE),
    
    CONSTANT_Class(7,Feature.v1_0_2,V1_5,INDIRECT,CONSTANT_Utf8),
    CONSTANT_String(8,Feature.v1_0_2,V1_0_2,INDIRECT,CONSTANT_Utf8),

    CONSTANT_NameAndType(12,Feature.v1_0_2,null,INDIRECT,CONSTANT_Utf8,CONSTANT_Utf8),
    CONSTANT_Fieldref(9,Feature.v1_0_2,null,INDIRECT,CONSTANT_Class,CONSTANT_NameAndType),
    CONSTANT_Methodref(10,Feature.v1_0_2,null,INDIRECT,CONSTANT_Class,CONSTANT_NameAndType),
    CONSTANT_InterfaceMethodref(11,Feature.v1_0_2,null,INDIRECT,CONSTANT_Class,CONSTANT_NameAndType),

    CONSTANT_MethodHandle(15,Feature.invokeDynamic,V1_7,HANDLE),
    CONSTANT_MethodType(16,Feature.invokeDynamic,V1_7,INDIRECT,CONSTANT_Utf8),
    CONSTANT_InvokeDynamic(18,Feature.invokeDynamic,null,BOOTSTRAP,CONSTANT_NameAndType),
    
    CONSTANT_Module(19,Feature.modules,null,INDIRECT,CONSTANT_Utf8),
    CONSTANT_Package(20,Feature.modules,null,INDIRECT,CONSTANT_Utf8),

    CONSTANT_Dynamic(17,Feature.constant_dynamic,V11,BOOTSTRAP,CONSTANT_NameAndType),
    // end Table 4.4B       
    ;

    private final int tag;
    private final Feature feature;
    private final JvmVersion loadable;
    private final EntryType et;
    private final ConstantPoolType[] pool;

    private ConstantPoolType(int tag, Feature feature, JvmVersion loadable, EntryType et, ConstantPoolType... pool) {
        this.tag = tag;
        this.feature = feature;
        this.loadable = loadable;
        this.et = et;
        this.pool = pool;
    }

    public Feature feature() {
        return feature;
    }

    @Override
    public JvmVersionRange range() {
        return feature.range();
    }

    public int tag() {
        return tag;
    }

    public String abbrev() {
        return name().substring(name().indexOf('_') + 1);
    }
    
    public int poolct() {
        return pool.length;
    }

    public EntryType getEntryType() {
        return et;
    }

    public ConstantPoolType[] getPool() {
        return pool;
    }
    
    public static Optional<ConstantPoolType> getInstance(int tag) {
        return Arrays.stream(values())
                .filter(cp->cp.tag == tag)
                .findFirst();
    }
    
    public boolean isLoadableBy(JvmVersion jvmversion) {
        boolean ok = loadable != null && jvmversion.compareTo(loadable)>= 0;
        if (!ok) {
            LOG(M63,this,jvmversion);    // "Loading of %s not supported in %s"
        }
        return ok;
    }

    public enum EntryType {
        UTF8,
        INTEGER,
        FLOAT,
        LONG,
        DOUBLE,
        INDIRECT,
        HANDLE,
        BOOTSTRAP,
        ;
    }
}

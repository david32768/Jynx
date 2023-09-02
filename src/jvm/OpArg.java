package jvm;

import java.util.EnumSet;
import java.util.stream.Stream;

import static jvm.ConstantPoolType.*;
import static jvm.OpPart.*;

public enum OpArg {
    
    arg_atype(2, TYPE),
    arg_byte(2, BYTE),
    arg_callsite(5, CONSTANT_InvokeDynamic, CP, ZERO, ZERO),
    arg_class(3, CONSTANT_Class, CP),
    arg_constant(2, OpArg.getLoadable(), CP),
    arg_dir(0),    // pseudo instructions e.g. .line
    arg_field(3, CONSTANT_Fieldref, CP),
    arg_incr(3, VAR, INCR),
    arg_interface(5, CONSTANT_InterfaceMethodref, CP, UBYTE, ZERO),
    arg_label(3, LABEL),
    arg_lookupswitch(null, LOOKUPSWITCH),
    arg_marray(4, CONSTANT_Class, CP, UBYTE),
    arg_method(3, EnumSet.of(CONSTANT_Methodref, CONSTANT_InterfaceMethodref), CP),
    arg_none(1),
    arg_short(3, SHORT),
    arg_stack(1),
    arg_tableswitch(null, TABLESWITCH),
    arg_var(2, VAR),
    ;

    private final Integer length;
    private final EnumSet<ConstantPoolType> cptypes;
    private final OpPart[] parts;

    private OpArg(Integer length) {
        this(length, EnumSet.noneOf(ConstantPoolType.class));
    }

    private OpArg(Integer length, OpPart... parts) {
        this(length, EnumSet.noneOf(ConstantPoolType.class), parts);
    }

    private OpArg(Integer length, ConstantPoolType first) {
        this(length, EnumSet.of(first));
    }

    private OpArg(Integer length, ConstantPoolType first, OpPart... parts) {
        this(length, EnumSet.of(first),parts);
    }

    private OpArg(Integer length, EnumSet<ConstantPoolType> cptypes, OpPart... parts) {
        this.length = length;
        this.cptypes = cptypes;
        this.parts = parts;
        assert !cptypes.isEmpty() == (parts.length > 0 && parts[0] == CP):name();
    }

    public Integer length() {
        return length;
    }
    
    public boolean hasCPEntry() {
        return !cptypes.isEmpty();
    }

    public OpPart[] getParts() {
        return parts.clone();
    }
    
    public void checkCPType(ConstantPoolType actual) {
        actual.checkCPType(cptypes);
    }
    
    private static EnumSet<ConstantPoolType> getLoadable() {
        EnumSet<ConstantPoolType>  cpset = EnumSet.noneOf(ConstantPoolType.class);
        Stream.of(ConstantPoolType.values())
                .filter(cpt -> cpt.isLoadableBy(JvmVersion.MAX_VERSION))
                .forEach(cpt -> cpset.add(cpt));
        return cpset;
    }
    
    @Override
    public String toString() {
        return name().substring(4);
    }

}

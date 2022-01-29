package jvm;

import java.util.EnumSet;
import java.util.Optional;
import java.util.stream.Stream;

import static org.objectweb.asm.Opcodes.*;

import static jvm.Context.*;
import static jynx.Global.LOG;
import static jynx.Message.*;

// Class - Table 4.1B
// Field - Table 4.5A
// Method - Table 4.6A
// nested class - Table 4.7.6A
public enum AccessFlag implements JvmVersioned {
        acc_public(ACC_PUBLIC,0x0001,CLASS,INNER_CLASS,FIELD,METHOD),
        acc_private(ACC_PRIVATE, 0x0002,INNER_CLASS,FIELD,METHOD),
        acc_protected(ACC_PROTECTED,0x0004,INNER_CLASS,FIELD,METHOD),
        acc_static(ACC_STATIC,0x0008,INNER_CLASS,FIELD,METHOD),
        acc_final(ACC_FINAL,0x0010,CLASS,INNER_CLASS,FIELD,METHOD,PARAMETER),
        acc_super(Feature.superflag,ACC_SUPER,0x0020,CLASS),
        acc_synchronized(ACC_SYNCHRONIZED,0x0020,METHOD),
        acc_open(Feature.modules,ACC_OPEN,0x0020,MODULE),
        acc_transitive(Feature.modules,ACC_TRANSITIVE,0x0020,REQUIRE),
        acc_volatile(ACC_VOLATILE,0x0040,FIELD),
        acc_bridge(Feature.bridge,ACC_BRIDGE,0x0040,METHOD),
        acc_static_phase(Feature.modules,ACC_STATIC_PHASE,0x0040,REQUIRE),
        acc_transient(ACC_TRANSIENT,0x0080,FIELD),
        acc_varargs(Feature.varargs,ACC_VARARGS,0x0080,METHOD),
        acc_native(ACC_NATIVE,0x0100,METHOD),
        acc_interface(ACC_INTERFACE,0x0200,CLASS,INNER_CLASS),
        acc_abstract(ACC_ABSTRACT,0x0400,CLASS,INNER_CLASS,METHOD),
        acc_fpstrict(Feature.fpstrict,ACC_STRICT,0x0800,METHOD),
        acc_synthetic(Feature.synthetic,ACC_SYNTHETIC,0x1000,CLASS,INNER_CLASS,FIELD,METHOD,PARAMETER,MODULE,REQUIRE),
        acc_annotation(Feature.annotations,ACC_ANNOTATION,0x2000,CLASS,INNER_CLASS),
        acc_enum(Feature.enums,ACC_ENUM,0x4000,CLASS,INNER_CLASS,FIELD),
        acc_module(Feature.modules,ACC_MODULE,0x8000,CLASS,INNER_CLASS),
        acc_mandated(Feature.mandated,ACC_MANDATED,0x8000,PARAMETER,MODULE,REQUIRE),
    // ASM specific pseudo access flags - not written to class file
        acc_record(Feature.record,ACC_RECORD,0x10000,CLASS,INNER_CLASS),
        acc_deprecated(Feature.deprecated,ACC_DEPRECATED, 0x20000,CLASS,INNER_CLASS, FIELD, METHOD),
    // flag for internal use - xxx_ prefix and 0x0
        xxx_component(Feature.record,0,0,FIELD,METHOD),
    ;

    private final int access_flag;
    private final EnumSet<Context> where;
    private final Feature feature;

    private AccessFlag(Feature feature, int access, int hex,  Context state1, Context... states) {
        assert access == hex:M161.format(name(),access,hex); // "%s: asm value (%d) does not agree with jvm value(%d)"
        this.access_flag = access;
        this.where = EnumSet.of(state1, states);
        this.feature = feature;
    }

    private AccessFlag(int access, int hex, Context state1, Context... states) {
        this(Feature.unlimited,access, hex, state1, states);
    }

    public int getAccessFlag() {
        return access_flag;
    }

    public Feature feature() {
        return feature;
    }

    
    @Override
    public JvmVersionRange range() {
        return feature.range();
    }

    public boolean isValid(Context state) {
        return where.contains(state);
    }

    @Override
    public String toString() {
        return name().substring(4);
    }

    public static Optional<AccessFlag> fromString(String token) {
        return Stream.of(values())
                .filter(acc->acc.toString().equals(token))
                .findFirst();
    }

    private boolean isPresent(int access,Context state) {
        return isValid(state) && (this.access_flag & access) != 0;
    }

    private boolean isPresent(int access,Context state, JvmVersion jvmversion) {
        return isPresent(access,state) && jvmversion.supports(this);
    }

    // basic check and disambiguate
    public static EnumSet<AccessFlag> getEnumSet(final int access,final Context acctype, final JvmVersion jvmversion) {
        assert acctype.isBasic(): "" + acctype.toString();
        EnumSet<AccessFlag> flags = Stream.of(values())
                .filter(flag->flag.isPresent(access,acctype,jvmversion))
                .collect(()->EnumSet.noneOf(AccessFlag.class),EnumSet::add,EnumSet::addAll);
        int invalid = access &~ getAccess(flags);
        if (invalid != 0) {
            EnumSet<AccessFlag> posflags = Stream.of(values())
                    .filter(flag->flag.isPresent(invalid,acctype))
                    .collect(()->EnumSet.noneOf(AccessFlag.class),EnumSet::add,EnumSet::addAll);
            int unknown = invalid &~ getAccess(posflags);
            if (!posflags.isEmpty()) {
                LOG(M110,posflags,jvmversion); // "access flag(s) %s not valid for version %s"
            }
            if (unknown != 0) {
                LOG(M107,invalid,acctype);   // "unknown access flag (%#04x) in context %s ignored"
            }
        }
        return flags;
    }

    public static String[] stringArrayOf(EnumSet<AccessFlag> flags) {
        return flags.stream()
                .map(AccessFlag::toString)
                .toArray(String[]::new);
    }

    private static int getAccess(EnumSet<AccessFlag> flags) {
        return flags.stream()
                .mapToInt(AccessFlag::getAccessFlag)
                .reduce(0, (accumulator, _item) -> accumulator | _item);
    }

}

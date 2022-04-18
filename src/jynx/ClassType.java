package jynx;

import java.util.EnumSet;
import java.util.Optional;
import java.util.stream.Stream;

import static jvm.AccessFlag.*;
import static jynx.Directive.*;

import jvm.AccessFlag;
import jvm.Context;
import jvm.JvmVersion;

public enum ClassType {
    
    // directive, determinator, must_have, must_not ...
        // mustnot == empty means only must_have allowed
    
            // ANNOTATION must come before INTERFACE
    ANNOTATION_CLASS(dir_define_annotation, dir_inner_define_annotation, acc_annotation,
            EnumSet.of(acc_annotation, acc_interface, acc_abstract)),
            // INTERFACE must be after ANNOTATION
    INTERFACE(dir_interface, dir_inner_interface, acc_interface,
            EnumSet.of(acc_interface, acc_abstract)),
            // PACKAGE must be after INTERFACE
    PACKAGE(dir_package, null, null,
            EnumSet.of(acc_interface,acc_abstract)),
    ENUM(dir_enum, dir_inner_enum, acc_enum,
            EnumSet.of(acc_enum, acc_super)),
    MODULE_CLASS(dir_module, null, acc_module, EnumSet.of(acc_module)),
    RECORD(dir_record, dir_inner_record, acc_record,
            EnumSet.of(acc_record, acc_super)),
    BASIC(dir_class, dir_inner_class, acc_super,
            EnumSet.of(acc_super)),
    ;

    private final Directive dir;
    private final Directive innerDir;
    private final AccessFlag determinator;
    private final EnumSet<AccessFlag> must;

    private ClassType(Directive dir, Directive innerdir, AccessFlag determinator,
            EnumSet<AccessFlag>  must) {
        assert determinator == null || must.contains(determinator);
        this.dir = dir;
        this.innerDir = innerdir;
        this.determinator = determinator;
        this.must = must;
    }

    private boolean isMe(EnumSet<AccessFlag> accflags) {
        return determinator != null && accflags.contains(determinator);
    }
    
    public static ClassType from(EnumSet<AccessFlag> accflags) {
        return Stream.of(values())
                .filter(ct->ct.isMe(accflags))
                .findAny()
                .orElse(BASIC); // super flag may not be present
    }

    public static ClassType of(Context context) {
        return valueOf(context.toString());
    }
    
    public static ClassType of(Directive dir) {
        return Stream.of(values())
                .filter(ct->ct.dir == dir)
                .findAny()
                .orElse(BASIC);
    }
    
    public AccessFlag getDeterminator() {
        return determinator;
    }

    public Directive getDir() {
        return dir;
    }

    public Directive getInnerDir() {
        return innerDir;
    }

    private EnumSet<AccessFlag> getMustHave(JvmVersion jvmversion, boolean inner) {
        return must.stream()
                .filter(flag->jvmversion.supports(flag))
                .filter(flag->!(inner && flag == acc_super))
                .collect(()->EnumSet.noneOf(AccessFlag.class),EnumSet::add,EnumSet::addAll);
    }

    public EnumSet<AccessFlag> getMustHave4Class(JvmVersion jvmversion) {
        return getMustHave(jvmversion, false);
    }

    public EnumSet<AccessFlag> getMustHave4Inner(JvmVersion jvmversion) {
        return getMustHave(jvmversion, true);
    }

}

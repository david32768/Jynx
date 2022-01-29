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
    
            // ANNOTATION must come before interface
    ANNOTATION(dir_define_annotation,acc_annotation, EnumSet.of(acc_annotation, acc_interface, acc_abstract),
            acc_final, acc_super, acc_enum,acc_module,acc_record),
            // INTERFACE must be after annotation
    INTERFACE(dir_interface, acc_interface, EnumSet.of(acc_interface, acc_abstract),
            acc_final, acc_super, acc_enum,acc_module,acc_annotation,acc_record),
    PACKAGE(dir_package, null, EnumSet.of(acc_interface)),
    ENUM(dir_enum, acc_enum, EnumSet.of(acc_enum, acc_super),
            acc_annotation,acc_module,acc_interface,acc_record),
    MODULE(dir_module,acc_module, EnumSet.of(acc_module)),
    RECORD(dir_record,acc_record, EnumSet.of(acc_record, acc_super),
            acc_annotation,acc_enum,acc_module,acc_interface),
    CLASS(dir_class,acc_super, EnumSet.of(acc_super),
            acc_annotation,acc_module,acc_enum,acc_interface,acc_record),
    ;

    private final Directive dir;
    private final AccessFlag determinator;
    private final EnumSet<AccessFlag> must;
    private final AccessFlag[] mustnot;

    private ClassType(Directive dir, AccessFlag determinator, EnumSet<AccessFlag>  must, AccessFlag...  mustnot) {
        assert determinator == null || must.contains(determinator);
        this.dir = dir;
        this.determinator = determinator;
        this.must = must.clone();
        if (mustnot.length == 0) {
            EnumSet<AccessFlag> notflags = EnumSet.allOf(AccessFlag.class);
            notflags.removeAll(this.must);
            this.mustnot = notflags.toArray(new AccessFlag[0]);
        } else {
            this.mustnot = mustnot.clone();
        }
    }

    private boolean isMe(EnumSet<AccessFlag> accflags) {
        return determinator != null && accflags.contains(determinator);
    }
    
    public static ClassType from(EnumSet<AccessFlag> accflags) {
        return Stream.of(values())
                .filter(ct->ct.isMe(accflags))
                .findAny()
                .orElse(CLASS);
    }

    public static ClassType of(Context context) {
        return valueOf(context.toString());
    }
    
    public static ClassType of(Directive dir) {
        return Stream.of(values())
                .filter(ct->ct.dir == dir)
                .findAny()
                .orElse(CLASS);
    }
    
    public AccessFlag getDeterminator() {
        return determinator;
    }

    public Directive getDir() {
        return dir;
    }

    public EnumSet<AccessFlag> getMustHave(JvmVersion jvmversion, boolean inner) {
        return must.stream()
                .filter(flag->jvmversion.supports(flag))
                .filter(flag->!(inner && flag == acc_super))
                .collect(()->EnumSet.noneOf(AccessFlag.class),EnumSet::add,EnumSet::addAll);
    }

    public AccessFlag[] getMustNot() {
        return mustnot.clone();
    }
    
    public static Optional<ClassType> getInnerClassType(String str) {
        if (str.equalsIgnoreCase(PACKAGE.name())) {
            return Optional.empty();
        }
        return Stream.of(values())
                .filter(c->c.name().equalsIgnoreCase(str))
                .findAny();
    }

    public String getTokenStr() {
        return toString().toLowerCase();
    }
}

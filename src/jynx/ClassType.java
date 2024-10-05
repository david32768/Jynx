package jynx;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static jvm.AccessFlag.*;
import static jynx.Directive.*;
import static jynx.Global.LOG;
import static jynx.GlobalOption.VALHALLA;
import static jynx.Message.M186;
import static jynx.Message.M283;
import static jynx.Message.M327;
import static jynx2asm.NameDesc.CLASS_NAME;

import jvm.AccessFlag;
import jvm.Constants;
import jvm.JvmVersion;

public enum ClassType {
    
    // default super class, mandated super, directive, inner directive, determinator, must_have
    
            // ANNOTATION must come before INTERFACE
    ANNOTATION_CLASS(Constants.OBJECT_CLASS, true,
            dir_define_annotation, dir_inner_define_annotation,
            EnumSet.of(acc_annotation),
            EnumSet.of(acc_annotation, acc_interface, acc_abstract)),
            // INTERFACE must be after ANNOTATION
    INTERFACE(Constants.OBJECT_CLASS, false,
            dir_interface, dir_inner_interface,
            EnumSet.of(acc_interface),
            EnumSet.of(acc_interface, acc_abstract)),
            // PACKAGE must be after INTERFACE
    PACKAGE(Constants.OBJECT_CLASS, true,
            dir_package, null,
            null,
            EnumSet.of(acc_interface,acc_abstract)),
    ENUM(Constants.ENUM_SUPER, false,
            dir_enum, dir_inner_enum,
            EnumSet.of(acc_enum),
            EnumSet.of(acc_enum, acc_super)),
    MODULE_CLASS(null, true,
            dir_define_module, null,
            EnumSet.of(acc_module),
            EnumSet.of(acc_module)),
    RECORD(Constants.RECORD_SUPER, true,
            dir_record, dir_inner_record,
            EnumSet.of(acc_record),
            EnumSet.of(acc_record, acc_super)),
    BASIC(Constants.OBJECT_CLASS, false,
            dir_class, dir_inner_class,
            EnumSet.of(acc_super),
            EnumSet.of(acc_super)),
    VALUE_CLASS(Constants.OBJECT_CLASS, false,
            dir_class, dir_inner_class,
            EnumSet.of(valhalla_acc_value),
            EnumSet.noneOf(AccessFlag.class)),
    ;

    private final String defaultSuper;
    private final boolean mandatedSuper;
    private final Directive dir;
    private final Directive innerDir;
    private final EnumSet<AccessFlag> determinator;
    private final EnumSet<AccessFlag> must;

    private ClassType(Constants defaultSuper, boolean mandated,
            Directive dir, Directive innerDir,
            EnumSet<AccessFlag> determinator,
            EnumSet<AccessFlag> must) {
        assert determinator == null || must.isEmpty() || must.containsAll(determinator);
        this.defaultSuper = defaultSuper == null? null: defaultSuper.stringValue();
        this.mandatedSuper = mandated;
        this.dir = dir;
        this.innerDir = innerDir;
        this.determinator = determinator;
        this.must = must;
    }

    public String defaultSuper() {
        if (defaultSuper != null) {
            LOG(M327, Directive.dir_super, defaultSuper); // "added: %s %s"
        }
        return defaultSuper;
    }
    
    private boolean isMe(EnumSet<AccessFlag> accflags) {
        return determinator != null && accflags.containsAll(determinator);
    }
    
    public static ClassType from(EnumSet<AccessFlag> accflags) {
        var classtype = Stream.of(values())
                .filter(ct->ct.isMe(accflags))
                .findFirst()
                .orElse(BASIC);
        return classtype;
    }

    public static ClassType of(Directive dir, EnumSet<AccessFlag> flags) {
        var classtype =  Stream.of(values())
                .filter(ct->ct.dir == dir)
                .findFirst()
                .orElseThrow();
        if (Global.OPTION(VALHALLA) && classtype == BASIC
                && !flags.contains(acc_super)) {
            classtype = VALUE_CLASS;
        }
        return classtype;
    }
    
    public Directive getDir() {
        return dir;
    }

    public Directive getInnerDir() {
        Objects.nonNull(innerDir);
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

    private final static Set<String> SUPERS = Stream.of(values())
                .map(ct -> ct.defaultSuper)
                .filter(s -> s != null)
                .collect(Collectors.toSet());
    
    public String checkSuper(String csuper) {
        CLASS_NAME.validate(csuper);
        if (Objects.equals(csuper, defaultSuper)) {
            return csuper;
        }
        if (mandatedSuper) {
            // "%s for %s must be %s"
            LOG(M186, Directive.dir_super, this, defaultSuper);
            return defaultSuper;
        }
        if (SUPERS.contains(csuper)) {
            // "%s for %s cannot be %s"
            LOG(M283, Directive.dir_super, this, csuper);
            return defaultSuper;
        }
        return csuper;
    }

}

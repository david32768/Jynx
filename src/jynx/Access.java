package jynx;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.function.LongPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static jvm.AccessFlag.*;
import static jvm.Context.*;
import static jynx.Directive.*;
import static jynx.Global.LOG;
import static jynx.Global.OPTION;
import static jynx.Global.SUPPORTS;
import static jynx.GlobalOption.VALHALLA;
import static jynx.Message.*;

import jvm.AccessFlag;
import jvm.Constants;
import jvm.Context;
import jvm.Feature;
import jvm.JvmVersion;
import jynx2asm.NameDesc;

public class Access {

    private final EnumSet<AccessFlag> accflags;
    private final JvmVersion jvmVersion;
    private final String name;
    private final ClassType classType;

    private Access(EnumSet<AccessFlag> accflags, JvmVersion jvmVersion, String name, ClassType classtype) {
        this.accflags = accflags;
        this.jvmVersion = jvmVersion;
        this.name = name;
        this.classType = classtype;
    }

    public static Access getInstance(EnumSet<AccessFlag> accflags,
            JvmVersion jvmversion, String name, ClassType classtype) {
        var myflags = accflags.clone();
        if (OPTION(GlobalOption.VALHALLA) && SUPPORTS(Feature.value) && myflags.contains(acc_strict)) {
            myflags.remove(acc_strict);
            myflags.add(valhalla_acc_strict);
        }
        return new Access(myflags, jvmversion, name, classtype);
    }

    public String name() {
        return name;
    }

    public ClassType classType() {
        return classType;
    }

    public JvmVersion jvmVersion() {
        return jvmVersion;
    }

    public boolean is(AccessFlag af) {
        return accflags.contains(af);
    }

    public void setComponent() {
        accflags.add(xxx_component);
    }
    
    private boolean isComponent() {
        return is(xxx_component);
    }

    public int getAccess() {
        return AccessFlag.getAccess(accflags);
    }

    private String access2string(EnumSet<AccessFlag> flags) {
        return flags.stream()
                .map(AccessFlag::toString)
                .collect(Collectors.joining(" "));
    }

    private String access2string(AccessFlag... flags) {
        EnumSet<AccessFlag> flagset = EnumSet.of(flags[0], Arrays.copyOfRange(flags, 1, flags.length));
        return access2string(flagset);
    }

    private boolean checkCount(LongPredicate pred, AccessFlag... flags) {
        long ct = Stream.of(flags)
                .filter(accflags::contains)
                .count();
        boolean valid = pred.test(ct);
        if (!valid) {
            accflags.removeAll(Arrays.asList(flags));
        }
        return valid;
    }

    private void oneOf(AccessFlag... flags) {
        boolean valid = checkCount(ct -> ct == 1, flags);
        if (!valid) {
            LOG(M120,access2string(flags));  // "Requires only one of {%s} specified"
            accflags.add(flags[0]);
        }
    }

    private void mostOneOf(AccessFlag... flags) {
        boolean valid = checkCount(ct -> ct <= 1, flags);
        if (!valid) {
            LOG(M114,access2string(flags));  // "Requires at most one of {%s} specified"
            accflags.add(flags[0]);
        }
    }

    private void allOf(AccessFlag... flags) {
        boolean valid = checkCount(ct -> ct == flags.length, flags);
        if (!valid) {
            LOG(M118,access2string(flags));  // "Requires all of {%s} specified"
            accflags.addAll(Arrays.asList(flags));
        }
    }

    private void allOf(EnumSet<AccessFlag> flags) {
        allOf(flags.toArray(AccessFlag[]::new));
    }
    
    private void noneOf(AccessFlag... flags) {
        boolean valid = checkCount(ct -> ct == 0, flags);
        if (!valid) {
            LOG(M125,access2string(flags));  // "Requires none of {%s} specified"
        }
    }

    private void checkValid(Context state, Directive dir) {
        mostOneOf(acc_public, acc_protected, acc_private);
        mostOneOf(acc_final, acc_abstract);
        EnumSet<AccessFlag> unknown = EnumSet.noneOf(AccessFlag.class);
        accflags.stream()
                .filter(flag -> !flag.isValid(state,dir))
                .forEach(unknown::add);
        if (!unknown.isEmpty()) {
            LOG(M160,unknown,state);  // "invalid access flags %s for %s are dropped"
            accflags.removeAll(unknown);
        }
        EnumSet<AccessFlag> invalid = EnumSet.noneOf(AccessFlag.class);
        accflags.stream()
                .filter(flag -> !jvmVersion.supports(flag))
                .forEach(invalid::add);
        if (!invalid.isEmpty()) {
            LOG(M110, invalid, state, jvmVersion);  // "access flag(s) %s in context %s not valid for version %s"
            accflags.removeAll(invalid);
        }
    }

    // Class - Table 4.1B
    public void check4Class() {
        checkValid(CLASS,classType.getDir());
        allOf(classType.getMustHave4Class(jvmVersion));
        if (OPTION(VALHALLA)) {
            oneOf(acc_super, valhalla_acc_value);
        }
    }

    // nested class - Table 4.7.6A
    public void check4InnerClass() {
        if (classType == ClassType.MODULE_CLASS) {
            // "inner class cannot be module"
            throw new LogIllegalArgumentException(M197);
        }
        if (!NameDesc.isInnerClass(name)) {
            LOG(M195,name); // "inner class name (%s) does not contain '$'"
        }
        checkValid(INNER_CLASS,classType.getInnerDir());
        allOf(classType.getMustHave4Inner(jvmVersion));
        if (OPTION(VALHALLA)) {
            oneOf(valhalla_acc_identity, valhalla_acc_value);
        }
    }

    // Field - Table 4.5A
    public void check4Field() {
        checkValid(FIELD,dir_field);
        mostOneOf(acc_final, acc_volatile);
        if (OPTION(GlobalOption.VALHALLA) && SUPPORTS(Feature.value) && is(valhalla_acc_strict)) {
            noneOf(acc_static);
            allOf(acc_final);
        }
        switch (classType) {
            case ANNOTATION_CLASS:
            case INTERFACE:
                allOf(acc_public, acc_static, acc_final);
                noneOf(acc_volatile, acc_transient, acc_enum);
                break;
            case RECORD:
                if (isComponent()) {
                    allOf(acc_private, acc_final);
                    noneOf(acc_static,acc_volatile,acc_transient,acc_enum);
                } else {
                    allOf(acc_static);
                    noneOf(acc_enum);
                }
                break;
            case ENUM:
                break;
            case BASIC:
                noneOf(acc_enum);
                break;
            case VALUE_CLASS:
                noneOf(acc_enum);
                oneOf(acc_static, valhalla_acc_strict);
                break;
            default:
                throw new EnumConstantNotPresentException(classType.getClass(),classType.name());
        }
    }

    public void check4InitMethod() {
        checkValid(INIT_METHOD,dir_method);
        if (classType == ClassType.INTERFACE || classType == ClassType.ANNOTATION_CLASS) {
            LOG(M235,NameDesc.CLASS_INIT_NAME); // "%s method appears in an interface"
        }
    }

    // Method - Table 4.6A
    public void check4Method() {
        checkValid(METHOD,dir_method);
        if (classType == ClassType.RECORD) {
            if (isComponent()) {
                if (is(acc_static) || !is(acc_public)) {
                    LOG(M226,accflags);  // "invalid access flags %s for component"
                }
            }
            noneOf(acc_native,acc_abstract);
        }
        if (name.equals(Constants.STATIC_INIT.stringValue())) {
            if (jvmVersion.compareTo(JvmVersion.V1_7) >= 0) {
                allOf(acc_static);
            }
        } else {
            if (accflags.contains(acc_abstract)) {
                noneOf(acc_private, acc_static, acc_final, acc_synchronized, acc_native, acc_strict);
            }
            if (classType == ClassType.INTERFACE || classType == ClassType.ANNOTATION_CLASS) {
                noneOf(acc_protected, acc_final, acc_synchronized, acc_native);
                if (jvmVersion.compareTo(JvmVersion.V1_8) < 0) {
                    allOf(acc_public, acc_abstract);
                } else {
                    oneOf(acc_public, acc_private);
                }
            }
        }
    }

    public void check4Parameter() {
        checkValid(PARAMETER,dir_parameter);
        mostOneOf(acc_synthetic, acc_mandated);
    }

    public void check4Module() {
        checkValid(MODULE,dir_define_module);
        mostOneOf(acc_synthetic, acc_mandated);
    }

    public void check4Export() {
        checkValid(EXPORT,dir_exports);
        mostOneOf(acc_synthetic, acc_mandated);
    }

    public void check4Open() {
        checkValid(OPEN,dir_opens);
        mostOneOf(acc_synthetic, acc_mandated);
    }

    public void check4Require() {
        checkValid(REQUIRE,dir_requires);
        mostOneOf(acc_synthetic, acc_mandated);
        if (!jvmVersion.supports(Feature.static_phase_transitive)
                && !NameDesc.isJavaBase(name.replace('.','/'))) {
            noneOf(acc_transitive, acc_static_phase);
        }
    }

}

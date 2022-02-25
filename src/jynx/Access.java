package jynx;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.function.LongPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static jvm.AccessFlag.*;
import static jvm.Context.*;
import static jynx.Global.LOG;
import static jynx.Message.*;

import jvm.AccessFlag;
import jvm.AttributeName;
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
        this.accflags = accflags.clone();
        this.jvmVersion = jvmVersion;
        this.name = name;
        this.classType = classtype;
    }

    public static Access getInstance(EnumSet<AccessFlag> accflags, JvmVersion jvmversion, String name, ClassType classtype) {
        return new Access(accflags, jvmversion, name, classtype);
    }

    public String getName() {
        return name;
    }

    public boolean is(AccessFlag af) {
        return accflags.contains(af);
    }
    
    public boolean isComponent() {
        return accflags.contains(xxx_component);
    }

    public int getAccess() {
        return accflags.stream()
                .mapToInt(AccessFlag::getAccessFlag)
                .reduce(0, (accumulator, _item) -> accumulator | _item);
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
        }
    }

    private void allOf(AccessFlag... flags) {
        boolean valid = checkCount(ct -> ct == flags.length, flags);
        if (!valid) {
            LOG(M118,access2string(flags));  // "Requires all of {%s} specified"
            accflags.addAll(Arrays.asList(flags));
        }
    }

    private void noneOf(AccessFlag... flags) {
        boolean valid = checkCount(ct -> ct == 0, flags);
        if (!valid) {
            LOG(M125,access2string(flags));  // "Requires none of {%s} specified"
        }
    }

    private void only(AccessFlag... flags) {
        accflags.addAll(Arrays.asList(flags));
        boolean valid = checkCount(ct -> ct == flags.length, flags);
        if (!valid) {
            LOG(M130,access2string(flags));  // "Requires only {%s} specified"
            accflags.clear();
            accflags.addAll(Arrays.asList(flags));
        }
    }

    private void checkValid(Context state) {
        mostOneOf(acc_public, acc_protected, acc_private);
        mostOneOf(acc_final, acc_abstract);
        if (accflags.contains(acc_synthetic)) {
            jvmVersion.checkSupports(AttributeName.Synthetic);
        }
        if (accflags.contains(acc_deprecated)) {
            jvmVersion.checkSupports(AttributeName.Deprecated);
        }
        EnumSet<AccessFlag> unknown = EnumSet.noneOf(AccessFlag.class);
        accflags.stream()
                .filter(flag -> !flag.isValid(state))
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
            LOG(M110,invalid,jvmVersion);  // "access flag(s) %s not valid for version %s"
            accflags.removeAll(invalid);
        }
    }

    private void check4Class(Context major) {
        if (major == INNER_CLASS && !NameDesc.isInnerClass(name)) {
            LOG(M195,name); // "inner class name (%s) does not contain '$'"
        }
        checkValid(major);
        allOf(classType.getMustHave(jvmVersion,major == INNER_CLASS).toArray(new AccessFlag[0]));
        noneOf(classType.getMustNot());
    }

    // Class - Table 4.1B
    public int getCheck4Class() {
        check4Class(CLASS);
        return getAccess();
    }

    // nested class - Table 4.7.6A
    public void getCheck4InnerClass() {
        if (classType == ClassType.MODULE) {
            // "inner class cannot be module"
            throw new LogIllegalArgumentException(M197);
        }
        check4Class(INNER_CLASS);
    }

    // Field - Table 4.5A
    public void getCheck4Field() {
        checkValid(FIELD);
        switch (classType) {
            case ANNOTATION:
            case INTERFACE:
                allOf(acc_public, acc_static, acc_final);
                noneOf(acc_volatile, acc_transient, acc_enum, acc_mandated);
                break;
            case RECORD:
                if (isComponent()) {
                    allOf(acc_private, acc_final);
                    noneOf(acc_static);
                } else {
                    mostOneOf(acc_final, acc_volatile);
                    allOf(acc_static);
                    noneOf(acc_enum);
                }
                break;
            case ENUM:
                mostOneOf(acc_final, acc_volatile);
                break;
            case CLASS:
                mostOneOf(acc_final, acc_volatile);
                noneOf(acc_enum);
                break;
            default:
                throw new EnumConstantNotPresentException(classType.getClass(),classType.name());
        }
    }

    // Method - Table 4.6A
    public void getCheck4Method(boolean isinit) {
        checkValid(METHOD);
        if (classType == ClassType.RECORD) {
            if (isComponent()) {
                if (is(acc_static) || !is(acc_public)) {
                    LOG(M226,accflags);  // "invalid access flags %s for component"
                }
            }
            noneOf(acc_native,acc_abstract);
        }
        if (isinit) {
            if (classType == ClassType.INTERFACE || classType == ClassType.ANNOTATION) {
                LOG(M235,NameDesc.CLASS_INIT_NAME); // "%s method appears in an interface"
            }
            noneOf(acc_static, acc_final, acc_synchronized, acc_bridge, acc_native, acc_abstract);
        } else if (name.equals(Constants.STATIC_INIT.toString())) {
            if (jvmVersion.compareTo(JvmVersion.V1_7) >= 0) {
                allOf(acc_static);
            }
        } else {
            if (accflags.contains(acc_abstract)) {
                noneOf(acc_private, acc_static, acc_final, acc_synchronized, acc_native, acc_fpstrict);
            }
            if (classType == ClassType.INTERFACE || classType == ClassType.ANNOTATION) {
                noneOf(acc_protected, acc_final, acc_synchronized, acc_native);
                if (jvmVersion.compareTo(JvmVersion.V1_8) < 0) {
                    allOf(acc_public, acc_abstract);
                } else {
                    oneOf(acc_public, acc_private);
                }
            }
        }
    }

    public void getCheck4Parameter() {
        checkValid(PARAMETER);
        mostOneOf(acc_synthetic, acc_mandated);
    }

    public void getCheck4Module() {
        checkValid(MODULE);
        mostOneOf(acc_synthetic, acc_mandated);
    }

    public void getCheck4Require() {
        checkValid(REQUIRE);
        mostOneOf(acc_synthetic, acc_mandated);
        if (!jvmVersion.supports(Feature.static_phase_transitive) && !NameDesc.isJavaBase(name)) {
            noneOf(acc_transitive, acc_static_phase);
        }
    }

}

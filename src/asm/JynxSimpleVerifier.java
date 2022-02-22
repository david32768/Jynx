package asm;

import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.SimpleVerifier;
import org.objectweb.asm.Type;

import static jynx.Global.LOG;
import static jynx.Message.M157;
import static jynx.Message.M167;
import static jynx.Message.M403;
import static jynx.Message.M404;
import static jynx.Message.M91;

import jynx.Global;
import jynx.GlobalOption;
import jynx2asm.TypeHints;

public class JynxSimpleVerifier extends SimpleVerifier {

    private final TypeHints hints;
    private final boolean forname;

    public JynxSimpleVerifier(
            final Type currentClass,
            final Type currentSuperClass,
            final List<Type> currentClassInterfaces,
            final boolean isInterface,
            final TypeHints hints) {
        super(Opcodes.ASM9, currentClass, currentSuperClass, currentClassInterfaces, isInterface);
        this.hints = hints;
        this.forname = Global.OPTION(GlobalOption.ALLOW_CLASS_FORNAME);
    }

    @Override
    protected Class<?> getClass(final Type type) {
        String name = type.getInternalName();
        if (!forname) {
            throw new TypeNotPresentException(name, null);
        }
         // "class %s has used Class.forName(); java.runtime.version = %s"
        LOG(M157,this.getClass().getSimpleName(),System.getProperty("java.runtime.version"));
        return super.getClass(type);
    }

    @Override
    protected boolean isSubTypeOf(BasicValue value, BasicValue expected) {
        try {
            return super.isSubTypeOf(value, expected);
        } catch (TypeNotPresentException ex) {
            // "add hint on is %s subtype of %s"
            LOG(M403,value.getType().getInternalName(),expected.getType().getInternalName());
            throw ex;
        }
    }

    @Override
    public BasicValue merge(BasicValue value1, BasicValue value2) {
        try {
            return super.merge(value1, value2);
        } catch (TypeNotPresentException ex) {
            //"add hint for type of merger of %s and %s"
            LOG(M404,value1.getType().getInternalName(),value2.getType().getInternalName());
            throw ex;
        }
    }

    @Override
    protected boolean isInterface(Type type) {
        try {
            return super.isInterface(type);
        } catch (TypeNotPresentException ex) {
            String name = type.getInternalName();
            if (hints.isKnownInterface(name)) {
                return true;
            } else if (hints.isKnownClass(name)) {
                return false;
            }
            LOG(M91, name); // "not known whether %s is a class or an interface"
            throw ex;
        }
    }

    @Override
    protected Type getSuperClass(Type type) {
        try {
            return super.getSuperClass(type);
        } catch (TypeNotPresentException ex) {
            String name = type.getInternalName();
            String superClass = hints.getSuper(name);
            if (superClass != null) {
                return Type.getObjectType(superClass);
            }
            LOG(M167, name); // "super class of %s not known"
            throw ex;
        }
    }

    @Override
    protected boolean isAssignableFrom(final Type type1, final Type type2) {
        try {
            return super.isAssignableFrom(type1, type2);
        } catch (TypeNotPresentException ex) {
            return hints.isAssignableFrom(type1, type2);
        }
    }

}

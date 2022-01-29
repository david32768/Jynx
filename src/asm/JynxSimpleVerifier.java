package asm;

import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.analysis.SimpleVerifier;
import org.objectweb.asm.Type;

import static jynx.Global.LOG;
import static jynx.Message.M157;
import static jynx.Message.M167;
import static jynx.Message.M87;
import static jynx.Message.M91;

import jynx2asm.TypeHints;

public class JynxSimpleVerifier extends SimpleVerifier {

    private final TypeHints hints;

    public JynxSimpleVerifier(
            final Type currentClass,
            final Type currentSuperClass,
            final List<Type> currentClassInterfaces,
            final boolean isInterface,
            final TypeHints hints) {
        super(Opcodes.ASM9, currentClass, currentSuperClass, currentClassInterfaces, isInterface);
        this.hints = hints;
    }

    @Override
    protected Class<?> getClass(final Type type) {
        LOG(M157,this.getClass().getSimpleName()); // "Class.forName() has been used in class %s"
        return super.getClass(type);
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
            if (hints.isAssignableFrom(type1, type2)) {
                return true;
            } else if (hints.isAssignableFrom(type2, type1)) {
                return false;
            }
            LOG(M87, type1.getInternalName(), type2.getInternalName()); // "add hint if %s is assignable from %s"
            throw ex;
        }
    }

}

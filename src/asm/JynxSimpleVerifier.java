package asm;

import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.SimpleVerifier;
import org.objectweb.asm.Type;

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
        if (hints.useClassForName(type)) {
            return super.getClass(type);
        } else {
            throw new TypeNotPresentException(type.getInternalName(), null);
        }
    }

    @Override
    protected boolean isSubTypeOf(BasicValue value, BasicValue expected) {
        try {
            return super.isSubTypeOf(value, expected);
        } catch (TypeNotPresentException ex) {
            return hints.isSubTypeOf(value, expected); 
        }
    }

    @Override
    public BasicValue merge(BasicValue value1, BasicValue value2) {
        try {
            return super.merge(value1, value2);
        } catch (TypeNotPresentException ex) {
            return hints.merge(value1, value2,ex.typeName());
        }
    }

}

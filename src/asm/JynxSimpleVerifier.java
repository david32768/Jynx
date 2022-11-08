package asm;

import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.SimpleVerifier;
import org.objectweb.asm.Type;

import static jynx.Global.LOG;
import static jynx.Message.M157;
import static jynx.Message.M404;

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
        LOG(M157,this.getClass().getSimpleName(),Global.javaRuntimeVersion());
        return super.getClass(type);
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
            Type type1 = value1.getType();
            Type type2 = value2.getType();
            Type common = hints.getCommonType(type1, type2);
            if (common != null) {
                return new BasicValue(common);
            }
            // "(redundant?) checkcasts or hint needed to obtain common supertype of%n    %s and %s"
            LOG(M404,value1.getType().getInternalName(),value2.getType().getInternalName());
            throw ex;
        }
    }

}

package asm;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.tree.analysis.SimpleVerifier;
import org.objectweb.asm.Type;

import jynx2asm.TypeHints;

public class VerifierFactory {

    private final Type classNameType;
    private final Type superType;
    private final List<Type> interfaceTypes;
    private final List<String> permitted;
    private final TypeHints hints;

    public VerifierFactory(String classname, String supername, List<String> interfaces,
            List<String> permitted, TypeHints hints) {
        this.classNameType = Type.getObjectType(classname);
        this.superType = supername == null ? null : Type.getObjectType(supername);
        this.interfaceTypes = new ArrayList<>();
        this.permitted = permitted;
        this.hints = hints;
        for (String itf : interfaces) {
            this.interfaceTypes.add(Type.getObjectType(itf));
        }
    }
    
    public SimpleVerifier getSimpleVerifier(boolean itf) {
        return  new JynxSimpleVerifier(
                        classNameType,
                        superType,
                        interfaceTypes,
                        itf,
                        hints);
        
    }
}

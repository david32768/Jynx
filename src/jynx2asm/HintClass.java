package jynx2asm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class HintClass {

    enum Type {
        CLASS,
        INTERFACE,
        UNKNOWN,
        ;
    }
    
    private final String name;
    private final Set<String> assignableTo;
    private final Map<HintClass,String> commons;

    private String superClass;
    private Type type;

    HintClass(String name) {
        this.name = name;
        this.superClass = null;
        this.assignableTo = new HashSet<>();
        this.commons =  new HashMap<>();
        this.type = Type.UNKNOWN;
    }

    HintClass setAsClass() {
        if (type == Type.UNKNOWN) {
            type = Type.CLASS;
        } else if (type != Type.CLASS) {
            throw new AssertionError();
        }
        return this;
    }
    
    HintClass setAsInterface() {
        if (type == Type.UNKNOWN) {
            type = Type.INTERFACE;
        } else if (type != Type.INTERFACE) {
            throw new AssertionError();
        }
        return this;
    }
    
    void addAssignableTo(String sup) {
        assignableTo.add(sup);
    }

    void addExtends(String sup) {
        if (superClass != null && !superClass.equals(sup)) {
            throw new AssertionError();
        }
        superClass = sup;
        addAssignableTo(sup);
    }

    void addCommon(HintClass other, String common) {
        String last = commons.put(other, common);
        if (last != null) {
            throw new AssertionError();
        }
    }
    
    boolean isClass() {
        return type == Type.CLASS;
    }
    
    boolean isInterface() {
        return type == Type.INTERFACE;
    }

    String getSuperClass() {
        return superClass;
    }
    
    boolean isAssignable2(String base) {
        return assignableTo.contains(base);
    }
    
    String common(HintClass other) {
        return commons.get(other);
    }

}

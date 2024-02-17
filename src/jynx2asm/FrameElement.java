package jynx2asm;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.objectweb.asm.Type;

import static jynx.Message.M206;
import static jynx.Message.M906;

import jvm.ConstType;
import jvm.FrameType;
import jynx.LogAssertionError;
import jynx.LogIllegalArgumentException;

public enum FrameElement {

    ANY(' ','?',true),
    ERROR('X','?',true),
    UNUSED('_', '?',true),
    TOP('2','?',true),

    RETURN_ADDRESS('R','?'),
    THIS('T','a'), // uninitialised this and this in <init> methods

    OBJECT('A','a'),
    INTEGER('I','i'),
    FLOAT('F','f'),
    DOUBLE('D','d'),
    LONG('J','l'),
;

    private final char typeChar;
    private final char instChar;
    private final boolean localsOnly;

    private FrameElement(char typeLetter, Character instChar) {
        this(typeLetter, instChar, false);
    }

    private FrameElement(char typeChar, char instChar, boolean localsOnly) {
        this.typeChar = typeChar;
        this.instChar = instChar;
        this.localsOnly = localsOnly;
    }

    private final static Map<Character, FrameElement> TYPE_MAP;
    
    static {
        TYPE_MAP = new HashMap<>();
        for (FrameElement fe:values()) {
            FrameElement shouldbenull = TYPE_MAP.put(fe.typeChar,fe);
            assert shouldbenull == null;
        }
    }

    private boolean isObject() {
        return instChar == 'a';
    }
    
    private char typeLetter() {
        return typeChar;
    }

    public boolean isCompatibleWith(FrameElement that) {
        return this == that || this == ERROR;
    }
    
    public boolean isAfterCompatibleWith(FrameElement that) {
        return isCompatibleWith(that) 
                || this == FrameElement.UNUSED
                || this == FrameElement.ANY;
    }

    public char instLetter() {
        return instChar;
    }
    
    public boolean isLocalsOnly() {
        return localsOnly;
    }
    
    public boolean isTwo() {
        return this == DOUBLE || this == LONG;
    }

    public int slots() {
        return isTwo()?2:1;
    }
    
    public FrameElement next() {
        if (isTwo()) {
            return TOP;
        }
        throw new AssertionError();
    } 

    public boolean checkNext(FrameElement fe) {
        return isTwo() && fe == TOP;
    } 

    public boolean matchStack(FrameElement required) {
        assert !required.isLocalsOnly();
        return this == required || isObject() && required == FrameElement.OBJECT;
    }
    
    public boolean matchLocal(FrameElement required) {
        return this == required || isObject() && required == FrameElement.OBJECT;
    }
    
    public boolean isValidInContext(FrameClass fc) {
        switch(fc) {
            case STACK:
                return !isLocalsOnly();
            case LOCALS:
                return true;
            case MAPLOCALS:
                return !isLocalsOnly() || this == TOP;
            default:
                throw new EnumConstantNotPresentException(fc.getClass(), fc.name());
        }
    }
    

    public static FrameElement combine(FrameElement fe1, FrameElement fe2) {
        if (fe1 == fe2) {
            return fe1;
        }
        if (fe1.isObject() && fe2.isObject()) {
            return OBJECT;
        }
        return ERROR;
    }

    public static boolean equivalent(FrameElement fe1, FrameElement fe2) {
        return fe1.compLetter() == fe2.compLetter();
    }
    
    private char compLetter() {
        return isObject()? OBJECT.typeChar: typeChar;
    }
    
    public static FrameElement fromStack(char type) {
        FrameElement stack = TYPE_MAP.get(type);
        if (stack == null || stack.isLocalsOnly()) {
            throw new LogIllegalArgumentException(M206, type,(int)type); // "Invalid type letter '%c' (%d)"
        }
        return stack;
    }
    
    public static Optional<FrameElement> fromReturn(char type) {
        if (type == 'V') {
            return Optional.empty();
        }
        return Optional.of(fromStack(type));
    }
    
    public static FrameElement fromLocal(char type) {
        FrameElement local = TYPE_MAP.get(type);
        if (local == null) {
            throw new LogIllegalArgumentException(M206, type,(int)type); // "Invalid type letter '%c' (%d)"
        }
        return local;
    }
    
    public static String stringForm(Stream<FrameElement> festream) {
        return festream
                .map(FrameElement::typeLetter)
                .map(String::valueOf)
                .collect(Collectors.joining()); 
    }
    
    public static FrameElement fromType(Type type) {
        int sort = type.getSort();
        switch (sort) {
            case Type.BOOLEAN:
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                return INTEGER;
            case Type.FLOAT:
                return FLOAT;
            case Type.LONG:
                return LONG;
            case Type.DOUBLE:
                return DOUBLE;
            case Type.OBJECT:
            case Type.ARRAY:
                return OBJECT;
            default:
                throw new LogAssertionError(M906, type); // "unknown ASM type - %s"
        }
    }
    
    public static Optional<FrameElement> fromReturnType(Type rt) {
        if (rt == Type.VOID_TYPE) {
            return Optional.empty();
        }
        return Optional.of(fromType(rt));
    }
    
    public static FrameElement fromDesc(String typestr) {
        Type type = Type.getType(typestr);
        return fromType(type);
    }
    
    public static FrameElement fromFrame(FrameType ft) {
        switch (ft) {
            case ft_Double:
                return DOUBLE;
            case ft_Float:
                return FLOAT;
            case ft_Integer:
                return INTEGER;
            case ft_Long:
                return LONG;
            case ft_Null:
            case ft_Object:
            case ft_Uninitialized:
                return OBJECT;
            case ft_UninitializedThis:
                return THIS;
            case ft_Top:
                return TOP;
            default:
                throw new EnumConstantNotPresentException(ft.getClass(), ft.name());
        }
    }

    public String desc() {
       switch(this) {
           case INTEGER:
           case LONG:
           case FLOAT:
           case DOUBLE:
               return "" + typeChar;
           default:
               return ConstType.ct_object.getDesc();
       } 
    }
}

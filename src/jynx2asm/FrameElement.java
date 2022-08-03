package jynx2asm;

import org.objectweb.asm.Type;

import static jynx.Message.M206;
import static jynx.Message.M906;

import jvm.FrameType;
import jynx.LogAssertionError;
import jynx.LogIllegalArgumentException;

public enum FrameElement {

    UNUSED('_',' ',true),
    RETURN_ADDRESS('R',' '),
    INTEGER('I','i'),
    FLOAT('F','f'),
    DOUBLE('D','d'),
    TOP('2', ' ',true),
    LONG('J','l'),
    OBJECT('A','a'),
    EXCEPTION('A','a'),
    ERROR('X',' ',true),
    IRRELEVANT(' ',' ',true),
;

    private final char typeLetter;
    private final char instChar;
    private final boolean localsOnly;

    private FrameElement(char typeLetter, Character instChar) {
        this(typeLetter, instChar, false);
    }

    private FrameElement(char typeLetter, char instChar, boolean localsOnly) {
        this.typeLetter = typeLetter;
        this.instChar = instChar;
        this.localsOnly = localsOnly;
    }

    public char typeLetter() {
        return typeLetter;
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

    public static FrameElement fromStack(char type) {
        switch (type) {
            case 'I':
                return INTEGER;
            case 'F':
                return FLOAT;
            case 'J':
                return LONG;
            case 'D':
                return DOUBLE;
            case 'A':
                return OBJECT;
            case 'R':
                return RETURN_ADDRESS;
            default:
                throw new LogIllegalArgumentException(M206, type,(int)type); // "Invalid type letter '%c' (%d)"
        }
    }
    
    public static FrameElement fromLocal(char type) {
        switch (type) {
            case 'I':
                return INTEGER;
            case 'F':
                return FLOAT;
            case 'J':
                return LONG;
            case 'D':
                return DOUBLE;
            case 'A':
                return OBJECT;
            case 'R':
                return RETURN_ADDRESS;
            case 'X':
                return ERROR;
            case 'U':
                return UNUSED;
            case '2' :
                return TOP;
            default:
                throw new LogIllegalArgumentException(M206, type,(int)type); // "Invalid type letter '%c' (%d)"
        }
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
            case ft_UninitializedThis:
                return OBJECT;
            case ft_Top:
                return TOP;
            default:
                throw new EnumConstantNotPresentException(ft.getClass(), ft.name());
        }
    }

}

package jvm;

import java.util.Optional;
import java.util.stream.Stream;

import static org.objectweb.asm.Opcodes.*;

import static jynx.Message.*;

import jynx.LogIllegalArgumentException;

public enum NumType {

    t_boolean(T_BOOLEAN,0, 1),
    t_byte(T_BYTE, Byte.MIN_VALUE, Byte.MAX_VALUE),
    t_char(T_CHAR, Character.MIN_VALUE, Character.MAX_VALUE),
    t_short(T_SHORT, Short.MIN_VALUE, Short.MAX_VALUE),
    t_int(T_INT, Integer.MIN_VALUE, Integer.MAX_VALUE),
    t_long(T_LONG,  Long.MIN_VALUE, Long.MAX_VALUE),
    t_float(T_FLOAT),
    t_double(T_DOUBLE),
    ;

    private final int typecode;
    private final long minvalue;
    private final long maxvalue;
    

    private NumType(int typecode, long minvalue, long maxvalue) {
        this.typecode = typecode;
        this.minvalue = minvalue;
        this.maxvalue = maxvalue;
    }

    private NumType(int typecode) {
        this(typecode, 0, 0);
    }
    
    public int typecode() {
        return typecode;
    }

    public static NumType getInstance(int typecode) {
        return Stream.of(values())
                .filter(nt->nt.typecode == typecode)
                .findFirst()
                .orElseThrow(()->new LogIllegalArgumentException(M129,typecode)); // "invalid typecode - %d" 
    }
    
    @Override
    public String toString() {
        return name().substring(2);
    }
    
    public static Optional<NumType> fromString(String token) {
        return Stream.of(values())
                .filter(nt->token.equals(nt.toString()))
                .findFirst();
    }

    public boolean isInRange(long var) {
        if (maxvalue == 0) {
            // "cannot range check floating point numbers"
            throw new LogIllegalArgumentException(M70);
        }
        return var >= minvalue && var <= maxvalue;
    }
    
    public boolean isInUnsignedRange(long var) {
        if (maxvalue == 0) {
            // "cannot range check floating point numbers"
            throw new LogIllegalArgumentException(M70);
        }
        if (this == t_long) {
            return var >= 0;
        }
        return var >= 0 && var <= 2*maxvalue + 1;
    }

    public void checkInRange(long var) {
        if (!isInRange(var)) {
            throw new LogIllegalArgumentException(M77,this,var,minvalue,maxvalue); // "%s value %d is not in range [%d,%d]"
        }
    }
    
    public void checkInUnsignedRange(long var) {
        if (!isInUnsignedRange(var)) {
            throw new LogIllegalArgumentException(M77,this,var,0,1+2*maxvalue); // "%s value %d is not in range [%d,%d]"
        }
    }
    
}

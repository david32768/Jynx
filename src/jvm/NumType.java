package jvm;

import java.util.Optional;
import java.util.stream.Stream;

import static org.objectweb.asm.Opcodes.*;

import static jynx.Message.M129;
import static jynx.Message.M70;
import static jynx.Message.M77;

import jynx.LogIllegalArgumentException;

public enum NumType {

    t_boolean(T_BOOLEAN,0, 1),
    t_byte(T_BYTE, Byte.MIN_VALUE, Byte.MAX_VALUE, Byte.toUnsignedLong((byte)-1)),
    t_char(T_CHAR, Character.MIN_VALUE, Character.MAX_VALUE),
    t_short(T_SHORT, Short.MIN_VALUE, Short.MAX_VALUE, Short.toUnsignedLong((short)-1)),
    t_int(T_INT, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.toUnsignedLong(-1)),
    t_long(T_LONG,  Long.MIN_VALUE, Long.MAX_VALUE),
    t_float(T_FLOAT),
    t_double(T_DOUBLE),
    ;

    private final int typecode;
    private final long minvalue;
    private final long maxvalue;
    private final long unsignedMaxvalue;
    

    private NumType(int typecode) {
        this(typecode, 0, 0, 0);
    }
    
    private NumType(int typecode, long minvalue, long maxvalue) {
        this(typecode,minvalue, maxvalue, maxvalue);
    }

    private NumType(int typecode, long minvalue, long maxvalue, long unsignedmaxvalue) {
        this.typecode = typecode;
        this.minvalue = minvalue;
        this.maxvalue = maxvalue;
        this.unsignedMaxvalue = unsignedmaxvalue;
        assert maxvalue == unsignedmaxvalue || unsignedmaxvalue == 2*maxvalue + 1:name();
    }

    public int typecode() {
        return typecode;
    }

    public long unsignedMaxvalue() {
        return unsignedMaxvalue;
    }

    public static NumType getInstance(int typecode) {
        return Stream.of(values())
                .filter(nt->nt.typecode == typecode)
                .findFirst()
                .orElseThrow(()->new LogIllegalArgumentException(M129,typecode)); // "invalid typecode - %d" 
    }
    
    public String externalName() {
        return name().substring(2);
    }
    
    @Override
    public String toString() {
        return externalName();
    }
    
    public static Optional<NumType> fromString(String token) {
        return Stream.of(values())
                .filter(nt->token.equals(nt.externalName()))
                .findFirst();
    }

    public boolean isInRange(long var) {
        if (maxvalue == 0) {
            // "cannot range check floating point numbers"
            throw new LogIllegalArgumentException(M70);
        }
        return var >= minvalue && var <= maxvalue;
    }

    // NB long is always signed
    public boolean isInUnsignedRange(long var) {
        if (maxvalue == 0) {
            // "cannot range check floating point numbers"
            throw new LogIllegalArgumentException(M70);
        }
        return var >= 0 && var <= unsignedMaxvalue;
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

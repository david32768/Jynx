package asm;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

public class CheckOpcodes {

    private CheckOpcodes() {}
    
    private final static Class<?> OPCODES;
    private final static MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    static {
        try {
            OPCODES = Class.forName("org.objectweb.asm.Opcodes");
        } catch (ClassNotFoundException ex) {
            throw new AssertionError(ex);
        }
    }
    
    public static int getStaticFieldValue(String name) {
        try {
            return getStatic(name);
        } catch (Throwable ex) {
            throw new AssertionError(ex);
        }
    }

    private static int getStatic(String name) throws NoSuchFieldException {
        MethodHandle field;
        try {
            field  = LOOKUP.findStaticGetter(OPCODES, name, int.class);
        } catch (IllegalAccessException ex) {
            throw new AssertionError(ex);
        }
        try {
            return (int)field.invoke();
        } catch (Throwable ex) {
            throw new AssertionError(ex);
        }
    }

    public static int getMaxJavaVersion() {
        for (int i = 9;i < 32768; ++i) {
            try {
                getStatic("V" + i);
            } catch (NoSuchFieldException ex) {
                return i - 1;
            }
        }
        throw new AssertionError();
    }
    
    public static void main(String[] args) {
        if (args.length > 0) {
            int value = getStaticFieldValue(args[0]);
            System.out.format("value of %s is %d%n", args[0],value);
        }
        System.out.format("max java = V%d%n", getMaxJavaVersion());
    }
}

package jynx2asm.handles;

import org.objectweb.asm.Handle;

import static jynx.Message.M99;

import jvm.HandleType;
import jynx.LogIllegalArgumentException;

public interface JynxHandle {
    
    public String desc();

    public default HandleType ht() {
        throw new AssertionError(); // return null;
    }

    public String name();

    public default String owner() {
        throw new AssertionError(); // return null;
    }

    public default boolean isInterface() {
        return false;
    }

    public default Handle handle() {
        return new Handle(ht().reftype(),owner(),name(),desc(),isInterface());
    }
    
    public String ond();

    public default String iond() {
        if (isInterface()) {
            return HandlePart.INTERFACE_PREFIX + ond();
        }
        return ond();
    }
    
    public static JynxHandle of(Handle handle) {
        HandleType ht = HandleType.of(handle);
        if (ht.isField()) {
            return FieldHandle.of(handle);
        } else {
            return MethodHandle.of(handle);
        }
    }
    
    public static Handle getHandle(String token) {
        int colon = token.indexOf(HandleType.SEP);
        if (colon < 0) {
            // "Separator '%s' not found in %s"
            throw new LogIllegalArgumentException(M99,HandleType.SEP,token);
        }
        String htag = token.substring(0,colon);
        HandleType ht = HandleType.fromMnemonic(htag);
        String handle = token.substring(colon + 1);
        if (ht.isField()) {
            return FieldHandle.getInstance(handle, ht).handle();
        } else {
            return MethodHandle.getInstance(handle, ht).handle();
        }
    }
}

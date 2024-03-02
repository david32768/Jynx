package asm;

import org.objectweb.asm.ClassTooLargeException;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodTooLargeException;

import static jvm.StandardAttribute.StackMapTable;
import static jynx.Global.*;

import jynx.Access;
import jynx2asm.TypeHints;

public class ASMClassNode extends JynxClassNode {

    private final ClassWriter cw;

    private ASMClassNode(Access accessname, ClassWriter cw, TypeHints hints) {
        super(accessname, cw, hints);
        this.cw = cw;
    }
    
    public static ASMClassNode getInstance(Access accessname, boolean usestack) {
        int cwflags;
        if (accessname.jvmVersion().supports(StackMapTable)) {
            cwflags = usestack? 0: ClassWriter.COMPUTE_FRAMES;
        } else {
            cwflags = usestack? 0: ClassWriter.COMPUTE_MAXS;
        }
        TypeHints hints = new TypeHints();
        JynxClassWriter cw = new JynxClassWriter(cwflags, hints);
        return new ASMClassNode(accessname, cw, hints);
    }

    @Override
    public byte[] toByteArray() {
        byte[] ba = null;
        try {
            ba = cw.toByteArray();
        } catch (ClassTooLargeException | MethodTooLargeException ex) {
            LOG(ex);
        }
        return ba;
    }
    
}

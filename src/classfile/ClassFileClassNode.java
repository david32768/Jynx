package classfile;

import org.objectweb.asm.tree.ClassNode;

import asm.JynxClassNode;
import jynx.Access;
import jynx2asm.TypeHints;

public class ClassFileClassNode extends JynxClassNode {

    private final ClassNode cn;
    
    private ClassFileClassNode(Access accessname, ClassNode cn, TypeHints hints) {
        super(accessname, cn, hints);
        this.cn = cn;
    }
    
    public static ClassFileClassNode getInstance(Access accessname, boolean usestack) {
        TypeHints hints = new TypeHints();
        ClassNode cn = new ClassNode();
        ClassFileClassNode unused = new ClassFileClassNode(accessname, cn, hints);
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] toByteArray() {
        throw new UnsupportedOperationException();
    }
}

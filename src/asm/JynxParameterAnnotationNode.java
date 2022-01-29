package asm;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;

public class JynxParameterAnnotationNode extends AnnotationNode implements AcceptsVisitor {

    private final boolean visible;
    private final int parmnum;

    private JynxParameterAnnotationNode(String desc, boolean visible, int parmnum) {
        super(Opcodes.ASM9, desc);
        this.visible = visible;
        this.parmnum = parmnum;
    }

    @Override
    public void accept(MethodVisitor mv) {
        accept(mv.visitParameterAnnotation(parmnum, desc, visible));
    }

    public static JynxParameterAnnotationNode getInstance(int parmnum, String desc, boolean visible) {
        return new JynxParameterAnnotationNode(desc, visible,parmnum);
    }
    
}

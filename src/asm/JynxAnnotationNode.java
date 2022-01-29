package asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.RecordComponentVisitor;
import org.objectweb.asm.tree.AnnotationNode;

public class JynxAnnotationNode extends AnnotationNode implements AcceptsVisitor {

    private final boolean visible;

    private JynxAnnotationNode(String desc, boolean visible) {
        super(Opcodes.ASM9, desc);
        this.visible = visible;
    }

    @Override
    public void accept(ClassVisitor cv) {
        accept(cv.visitAnnotation(desc, visible));
    }
    
    @Override
    public void accept(RecordComponentVisitor rcv) {
        accept(rcv.visitAnnotation(desc, visible));
    }

    @Override
    public void accept(FieldVisitor fv) {
        accept(fv.visitAnnotation(desc, visible));
    }

    @Override
    public void accept(MethodVisitor mv) {
        accept(mv.visitAnnotation(desc, visible));
    }

    public static JynxAnnotationNode getInstance(String desc, boolean visible) {
        return new JynxAnnotationNode(desc, visible);
    }
    
}

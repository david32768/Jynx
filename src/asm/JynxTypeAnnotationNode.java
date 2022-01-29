package asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.RecordComponentVisitor;
import org.objectweb.asm.tree.TypeAnnotationNode;
import org.objectweb.asm.TypePath;

public class JynxTypeAnnotationNode  extends TypeAnnotationNode implements AcceptsVisitor {

    private final boolean visible;

    private JynxTypeAnnotationNode(int typeref, TypePath tp, String desc, boolean visible) {
        super(Opcodes.ASM9, typeref, tp, desc);
        this.visible = visible;
    }

    @Override
    public void accept(ClassVisitor cv) {
        accept(cv.visitTypeAnnotation(typeRef, typePath, desc, visible));
    }
    
    @Override
    public void accept(RecordComponentVisitor rcv) {
        accept(rcv.visitTypeAnnotation(typeRef, typePath, desc, visible));
    }

    @Override
    public void accept(FieldVisitor fv) {
        accept(fv.visitTypeAnnotation(typeRef, typePath, desc, visible));
    }

    @Override
    public void accept(MethodVisitor mv) {
        accept(mv.visitTypeAnnotation(typeRef, typePath, desc, visible));
    }

    public static JynxTypeAnnotationNode getInstance(int typeref, TypePath tp, String desc, boolean visible) {
        return new JynxTypeAnnotationNode(typeref, tp, desc, visible);
    }
    
}

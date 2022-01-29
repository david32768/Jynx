package asm;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;

public class JynxDefaultAnnotationNode extends AnnotationNode implements AcceptsVisitor {

    private JynxDefaultAnnotationNode() {
        super(Opcodes.ASM9, "throw_away_name_for_default"); // checkAdapter requires non_null
    }

    @Override
    public void accept(MethodVisitor mv) {
        accept(mv.visitAnnotationDefault());
    }

    public static JynxDefaultAnnotationNode getInstance(boolean visible) {
        return new JynxDefaultAnnotationNode();
    }
    
}

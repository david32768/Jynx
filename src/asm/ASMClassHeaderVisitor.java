package asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.ModuleVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.RecordComponentVisitor;

public class ASMClassHeaderVisitor extends ClassVisitor {
    
    public ASMClassHeaderVisitor(ClassVisitor cv) {
        super(Opcodes.ASM9, cv);
    }

    @Override
    public RecordComponentVisitor visitRecordComponent(String name, String descriptor, String signature) {
        throw new AssertionError();
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        throw new AssertionError();
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        throw new AssertionError();
    }

    @Override
    public ModuleVisitor visitModule(String name, int access, String version) {
        throw new AssertionError();
    }

    @Override
    public void visitEnd() {
        // Header only, add co,pomrets , fiealds ,methods, module later
    }
        
}

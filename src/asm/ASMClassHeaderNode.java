package asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.ModuleVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.RecordComponentVisitor;
import org.objectweb.asm.tree.ClassNode;

public class ASMClassHeaderNode extends ClassNode {
    
    public ASMClassHeaderNode() {
        super(Opcodes.ASM9);
    }

    public ASMClassHeaderNode(int version, String name, int access) {
        super(Opcodes.ASM9);
        this.version = version;
        this.name = name;
        this.access = access;
    }

    @Override
    public RecordComponentVisitor visitRecordComponent(String name, String descriptor, String signature) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ModuleVisitor visitModule(String name, int access, String version) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void accept(ClassVisitor classVisitor) {
        ClassVisitor noend = new ASMClassHeaderVisitor(classVisitor);
        super.accept(noend);
    }

}

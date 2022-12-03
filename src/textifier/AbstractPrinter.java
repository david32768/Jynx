package textifier;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.Printer;

public class AbstractPrinter extends Printer {

    protected AbstractPrinter() {
        super(Opcodes.ASM9);
    }
    
    private static final String MSG = "Unexpected visit.";

    
    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public void visitSource(String source, String debug) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public void visitOuterClass(String owner, String name, String descriptor) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public Printer visitClassAnnotation(String descriptor, boolean visible) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public void visitClassAttribute(Attribute attribute) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public Printer visitField(int access, String name, String descriptor, String signature, Object value) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public Printer visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public void visitClassEnd() {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public void visit(String name, Object value) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public void visitEnum(String name, String descriptor, String value) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public Printer visitAnnotation(String name, String descriptor) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public Printer visitArray(String name) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public void visitAnnotationEnd() {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public Printer visitFieldAnnotation(String descriptor, boolean visible) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public void visitFieldAttribute(Attribute attribute) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public void visitFieldEnd() {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public Printer visitAnnotationDefault() {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public Printer visitMethodAnnotation(String descriptor, boolean visible) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public Printer visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public void visitMethodAttribute(Attribute attribute) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public void visitCode() {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public void visitInsn(int opcode) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public void visitVarInsn(int opcode, int varIndex) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public void visitLabel(Label label) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public void visitLdcInsn(Object value) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public void visitIincInsn(int varIndex, int increment) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public void visitMethodEnd() {
        throw new UnsupportedOperationException(MSG);
    }
    
}

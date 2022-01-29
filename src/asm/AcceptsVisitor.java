package asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.RecordComponentVisitor;

public interface AcceptsVisitor {

    public default void accept(ClassVisitor cv) {
        throw new AssertionError();
    }

    public default void accept(RecordComponentVisitor rcv) {
        throw new AssertionError();
    }

    public default void accept(FieldVisitor fv) {
        throw new AssertionError();
    }

    public default void accept(MethodVisitor mv)  {
        throw new AssertionError();
    }

}

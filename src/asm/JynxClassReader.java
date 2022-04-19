package asm;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;

import org.objectweb.asm.ClassReader;

import static jynx.Global.LOG;
import static jynx.Message.M238;

public class JynxClassReader extends ClassReader {

    public JynxClassReader(final java.lang.String className) throws IOException {
        this(path4Class(className.replace('.', '/') + ".class"));
    }

    public JynxClassReader(final Path path) throws IOException {
        this(Files.readAllBytes(path));
    }

    public JynxClassReader(final byte[] classFileBuffer, final int classFileOffset, final int classFileLength) {
        this(Arrays.copyOfRange(classFileBuffer, classFileOffset, classFileOffset + classFileLength));
    }

    // main constructor
    JynxClassReader(final byte[] classFile) {
        super(classFile, 0, classFile.length);
        int expectedLength = classFile.length;
        int actualLength;
        try {
            int classAttributesOffset = getClassAttribuesOffset();
            actualLength = bypass_attrs(classAttributesOffset);
        } catch (ArithmeticException | ArrayIndexOutOfBoundsException | IllegalStateException aex) {
            String msg = "Unable to calculate length";
            throw new IllegalArgumentException(msg, aex);
        }
        if (actualLength != expectedLength) {
            String msg = String.format(" expected length %d is not equal to actual length %d",
                    expectedLength, actualLength);
            throw new IllegalArgumentException(msg);
        }
    }

    public static byte[] getClassBytes(String name) throws IOException {
        Path path;
        if (name.endsWith(".class")) {
            path = Paths.get(name);
        } else {
            path = JynxClassReader.path4Class(name);
        }
        return Files.readAllBytes(path);
    }

    public static Optional<ClassReader> getClassReader(String name) {
        try {
            byte[] ba = getClassBytes(name);
            ClassReader cr = new JynxClassReader(ba);
            return Optional.of(cr);
        } catch (Exception ex) {
            LOG(M238,ex); // "error reading class file: %s"
            return Optional.empty();
        }
    }

    private static Path path4Class(String name) throws IOException {
        URL url = ClassLoader.getSystemResource(name.replace('.', '/') +".class");
        try {
            URI uri = url.toURI();
            return Paths.get(uri);
        } catch (URISyntaxException | NullPointerException ex) {
            throw new IOException("unable to get class " + name,ex);
        }
    }
    
    private int readUnsignedInt(int offset) {
        int size = readInt(offset);
        if (size < 0) {
            throw new IllegalStateException("negative size");
        }
        return size;
    }

    private int bypass_attrs(final int start_offset) {
        assert start_offset >= header;
        int attrs_ct = readUnsignedShort(start_offset);
        int offset = Math.addExact(start_offset, 2); // attrs_ct
        while (attrs_ct-- > 0) {
            offset = Math.addExact(offset, 2); // name
            int size = readUnsignedInt(offset);
            offset = Math.addExact(offset, 4); // size
            offset = Math.addExact(offset, size);
        }
        return offset;
    }

    private int bypass_fields_or_methods(final int start_offset) {
        assert start_offset >= header;
        int ct = readUnsignedShort(start_offset);
        int offset = Math.addExact(start_offset, 2); // field_ct or method ct
        while (ct-- > 0) {
            offset = Math.addExact(offset, 2 + 2 + 2); // access, name, type
            offset = bypass_attrs(offset);
        }
        return offset;
    }

    private int getClassAttribuesOffset() {
        int offset = header;
        assert offset >= 4 + 2 + 2 + 2; // magic, minor version, major version, constant ct
        offset = Math.addExact(offset, 2 + 2 + 2); // access, this class, super
        int interfaces_ct = readUnsignedShort(offset);
        offset = Math.addExact(offset, 2 + 2 * interfaces_ct); // interfaces_ct, interfaces
        offset = bypass_fields_or_methods(offset); // fields
        offset = bypass_fields_or_methods(offset); // methods
        return offset;
    }

}

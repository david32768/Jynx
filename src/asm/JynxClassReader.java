package asm;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.objectweb.asm.ClassReader;

import static jynx.Global.LOG;
import static jynx.Message.M238;

public class JynxClassReader {

    public static byte[] getClassBytes(String name) throws IOException {
        Path path;
        if (name.endsWith(".class")) {
            path = Paths.get(name);
        } else {
            URL url = ClassLoader.getSystemResource(name.replace('.', '/') +".class");
            try {
                URI uri = url.toURI();
                path = Paths.get(uri);
            } catch (URISyntaxException | NullPointerException ex) {
                throw new IOException("unable to get class " + name,ex);
            }
        }
        return Files.readAllBytes(path);
    }

    public static Optional<ClassReader> getClassReader(String name) {
        try {
            byte[] ba = getClassBytes(name);
            ClassReader cr = new ClassReader(ba);
            assert ba.length == getLength(cr);
            return Optional.of(cr);
        } catch (Exception ex) {
            LOG(M238,ex); // "error reading class file: %s"
            return Optional.empty();
        }
    }

    
    private static int bypass_attrs(final ClassReader cr, final int start_offset) {
        assert start_offset >= 0;
        int attrs_ct = cr.readUnsignedShort(start_offset);
        int offset = Math.addExact(start_offset,2); // attrs_ct
        for (int i = 0; i < attrs_ct; ++i) {
            offset = Math.addExact(offset,2); // name
            int size = cr.readInt(offset);
            if (size < 0) {
                throw new IllegalStateException();
            }
            offset = Math.addExact(offset,4); // size
            offset = Math.addExact(offset,size);
        }
        return offset;
    }
    
    private static int bypass_fields_or_methods(final ClassReader cr, final int start_offset) {
        assert start_offset >= 0;
        int ct = cr.readUnsignedShort(start_offset);
        int offset = Math.addExact(start_offset,2); // field_ct or method ct
        for (int i = 0; i < ct; ++i) {
            offset = Math.addExact(offset,2+2+2); // access, name, type
            offset = bypass_attrs(cr,offset);
        }
        return offset;
    }
    
    public static int getLength(final ClassReader cr) {
        int offset = cr.header;
        assert offset >= 4+2+2+2; // magic, minor version, major version, constant ct
        try {
            offset = Math.addExact(offset,2+2+2); // access, this class, super
            int interfaces_ct = cr.readUnsignedShort(offset);
            offset = Math.addExact(offset,2+2*interfaces_ct); // interfaces_ct, interfaces
            offset = bypass_fields_or_methods(cr,offset); // fields
            offset = bypass_fields_or_methods(cr,offset); // methods
            offset = bypass_attrs(cr,offset);
            return offset;
        } catch (ArithmeticException ex) {
            throw new IllegalStateException(ex);
        }
    }

}

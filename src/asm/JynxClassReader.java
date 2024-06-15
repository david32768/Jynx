package asm;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Optional;

import org.objectweb.asm.ClassReader;

import static jynx.Global.LOG;
import static jynx.Global.OPTION;
import static jynx.GlobalOption.DOWN_CAST;
import static jynx.Message.M238;
import static jynx.Message.M285;
import static jynx.Message.M287;
import static jynx.Message.M288;

import jvm.JvmVersion;
import jynx.ClassUtil;
import jynx.LogIllegalArgumentException;

public class JynxClassReader extends ClassReader {

    private JynxClassReader(final byte[] classFile) {
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

    public static Optional<ClassReader> getClassReader(String name) {
        try {
            byte[] ba = ClassUtil.getClassBytes(name);
            ClassReader cr = getClassReader(ba);
            return Optional.of(cr);
        } catch (IOException ex) {
            LOG(M238,ex.getMessage()); // "error reading class file: %s"
            return Optional.empty();
        }
    }

    public static ClassReader getClassReader(byte[] ba) {
            ba = checkVersion(ba);
            ClassReader cr = new JynxClassReader(ba);
            return cr;
    }

    private static byte[] checkVersion(byte[] ba) {
        ByteBuffer bb = ByteBuffer.wrap(ba);
        bb = bb.asReadOnlyBuffer();
        bb.order(ByteOrder.BIG_ENDIAN);
        int magic = bb.getInt();
        if (magic != 0xcafebabe) {
            // "magic number is %#x; should be 0xcafebabe"
            throw new LogIllegalArgumentException(M285,magic);
        }
        int release = bb.getInt();
        JvmVersion jvmversion = JvmVersion.fromASM(release);
        if (jvmversion.compareTo(JvmVersion.MAX_VERSION) > 0) {
            if (OPTION(DOWN_CAST)) {
                // "JVM version %s is not supported by the version of ASM used; %s substituted"
                LOG(M287,jvmversion,JvmVersion.MAX_VERSION);
                bb = ByteBuffer.wrap(ba);
                bb.order(ByteOrder.BIG_ENDIAN);
                bb.getInt(); // magic
                bb.putInt(JvmVersion.MAX_VERSION.toASM());
            } else {
                // "JVM version %s is not supported by the version of ASM used; maximum version is %s "
                throw new LogIllegalArgumentException(M288,jvmversion,JvmVersion.MAX_VERSION);
            }
        }
        return ba;
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

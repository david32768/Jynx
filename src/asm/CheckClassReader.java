package asm;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.objectweb.asm.ClassReader;

public class CheckClassReader extends ClassReader {

    /**
     * The maximum size of array to allocate.
     */
    private static final int MAX_BUFFER_SIZE = 1024 * 1024;

    /**
     * The size of the temporary byte array used to read class input streams
     * chunk by chunk.
     */
    private static final int INPUT_STREAM_DATA_CHUNK_SIZE = 4096;

    public CheckClassReader​(java.io.InputStream inputStream) throws IOException {
        this(readStream(inputStream, false));
    }

    public CheckClassReader​(java.lang.String className) throws IOException {
        this(
                readStream(
                        ClassLoader.getSystemResourceAsStream(className.replace('.', '/') + ".class"), true));
    }

    public CheckClassReader(final Path path) throws IOException {
        this(Files.readAllBytes(path));
    }

    public CheckClassReader​(final byte[] classFileBuffer, final int classFileOffset, final int classFileLength) {
        this(Arrays.copyOfRange(classFileBuffer, classFileOffset, classFileOffset + classFileLength));
    }

    // main constructor
    CheckClassReader​(final byte[] classFile) {
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

    public final static byte[] readStream(final InputStream inputStream, final boolean close)
            throws IOException {
        if (inputStream == null) {
            throw new IOException("Class not found");
        }
        try {
            int expectedLength = inputStream.available();
            /*
         * Some implementations can return 0 while holding available data
         * (e.g. new FileInputStream("/proc/a_file"))
         * Also in some pathological cases a very small number might be returned,
         * and in this case we use default size
             */
            if (expectedLength < INPUT_STREAM_DATA_CHUNK_SIZE) {
                expectedLength = INPUT_STREAM_DATA_CHUNK_SIZE;
            } else if (expectedLength > MAX_BUFFER_SIZE) {
                expectedLength = MAX_BUFFER_SIZE - INPUT_STREAM_DATA_CHUNK_SIZE;
            }
            int inputBufferSize = expectedLength + INPUT_STREAM_DATA_CHUNK_SIZE;
            byte[] data = new byte[inputBufferSize];
            int bytesRead = inputStream.read(data, 0, expectedLength);
            if (bytesRead < 0) {
                return new byte[0];
            }
            int bytesRead2 = inputStream.read(data, bytesRead, inputBufferSize - bytesRead);
            if (bytesRead2 < 0) {
                byte[] result = new byte[bytesRead];
                System.arraycopy(data, 0, result, 0, bytesRead);
                return result;
            }
            bytesRead += bytesRead2;
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(bytesRead);
            // default buffer size for ByteArrayOutputStream is 32 
            do {
                outputStream.write(data, 0, bytesRead);
            } while ((bytesRead = inputStream.read(data, 0, inputBufferSize)) != -1);
            outputStream.flush();
            return outputStream.toByteArray();
        } finally {
            if (close) {
                inputStream.close();
            }
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

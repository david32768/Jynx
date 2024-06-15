package jynx;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static jynx.Message.M286;

public class ClassUtil {
    
    private static final int BUFFER_SIZE = 1<<14;
    
    public static byte[] getClassBytes(String name) throws IOException {
        Path path;
        if (name.endsWith(".class")) {
            path = Paths.get(name);
            return Files.readAllBytes(path);
        } else {
            return bytes4Class(name);
        }
    }

    private static byte[] bytes4Class(String name) throws IOException {
        assert !name.endsWith(".class");
        InputStream isx = ClassLoader.getSystemResourceAsStream(name.replace('.', '/') + ".class");
        if (isx == null) {
            //"%s is not (a known) class"
            throw new LogIllegalArgumentException(M286, name);
        }
        try (InputStream is = isx) {
            byte[] ba = new byte[BUFFER_SIZE];
            int readct = is.read(ba, 0, BUFFER_SIZE >> 1);
            if (readct < 0) {
                return new byte[0];
            }
            int readct2 = is.read(ba, readct, BUFFER_SIZE - readct);
            if (readct2 < 0) {
                return Arrays.copyOf(ba, readct);
            }
            readct += readct2;
            ByteArrayOutputStream os = new ByteArrayOutputStream(BUFFER_SIZE << 1);
            do {
                os.write(ba, 0, readct);
            } while ((readct = is.read(ba))>= 0);
            return os.toByteArray();
        }
    }
    
}

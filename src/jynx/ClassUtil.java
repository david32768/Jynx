package jynx;

import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static jynx.Message.M286;
import static jynx.Message.M289;

public class ClassUtil {
        
    public static byte[] getClassBytes(String name) throws IOException {
        Path path;
        if (name.endsWith(".class")) {
            path = Paths.get(name);
            if (!Files.exists(path)) {
                // "file %s does not exist"
                throw new LogIllegalArgumentException(M289, name);
            }
            return Files.readAllBytes(path);
        } else {
            InputStream isx = ClassLoader.getSystemResourceAsStream(name.replace('.', '/') + ".class");
            if (isx == null) {
                //"%s is not (a known) class"
                throw new LogIllegalArgumentException(M286, name);
            }
            return isx.readAllBytes();
        }
    }

}

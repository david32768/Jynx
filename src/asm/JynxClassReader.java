package asm;

import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.objectweb.asm.ClassReader;

import static jynx.Global.LOG;
import static jynx.Message.M238;

public class JynxClassReader {

    public static byte[] getClassBytes(String name) throws IOException {
        if (name.endsWith(".class")) {
            Path path = Paths.get(name);
            return Files.readAllBytes(path);
        } else {
            InputStream is = ClassLoader.getSystemResourceAsStream(name.replace('.', '/') +".class");
            return CheckClassReader.readStream(is, true);
        }
    }

    public static Optional<ClassReader> getClassReader(String name) {
        try {
            byte[] ba = getClassBytes(name);
            ClassReader cr = new CheckClassReader(ba);
            return Optional.of(cr);
        } catch (Exception ex) {
            LOG(M238,ex); // "error reading class file: %s"
            return Optional.empty();
        }
    }

}

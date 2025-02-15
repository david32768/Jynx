package jynx;

import java.io.PrintWriter;
import java.util.Optional;
import java.util.ServiceLoader;

import static jynx.Message.M331;

public interface MainOptionService {

    MainOption main();

    default boolean call(Optional<String> optfname)  {
        return call(optfname.get(), new PrintWriter(System.out));
    }

    default boolean call(String fname, PrintWriter pw) {
        throw new UnsupportedOperationException();
    }

    public static MainOptionService find(MainOption main) {
        var loader = ServiceLoader.load(MainOptionService.class);
        for (var mainx : loader) {
            if (mainx.main() == main) {
                return mainx;
            }
        }
        // "MainOption service for %s not found"
        String msg = M331.format(main);
        throw new UnsupportedOperationException(msg);
    }
}

package checker;

import java.io.PrintWriter;
import java.util.Optional;

import jynx.MainOption;
import jynx.MainOptionService;

public class MainStructure implements MainOptionService {

    @Override
    public MainOption main() {
        return MainOption.STRUCTURE;
    }


    @Override
    public boolean call(Optional<String> optfname) {
        PrintWriter pw = new PrintWriter(System.out);
        return Structure.printClassStructure(optfname.get(),pw);
    }
        
}

package asm2jynx;

import java.io.PrintWriter;

import jynx.MainOption;
import jynx.MainOptionService;

public class Main2Jynx implements MainOptionService {

    @Override
    public MainOption main() {
        return MainOption.DISASSEMBLY;
    }

    @Override
    public boolean call(String fname, PrintWriter pw) {
        return JynxDisassemble.a2jpw(pw,fname);
    }

}

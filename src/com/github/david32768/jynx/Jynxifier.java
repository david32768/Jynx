package com.github.david32768.jynx;

import java.io.IOException;
import java.io.PrintWriter;

import org.objectweb.asm.util.Printer;

import jynx.ClassUtil;
import textifier.JynxText;

public class Jynxifier {

    public static Printer getPrinter() {
        return new JynxText();
    }
    
    public static void main(final String[] args) throws IOException {
        byte[] ba = ClassUtil.getClassBytes(args[0]);
        try (PrintWriter pw = new PrintWriter(System.out)) {
            JynxText.jynxify(ba,pw);
        }
    }

}

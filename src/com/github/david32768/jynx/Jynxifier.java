package com.github.david32768.jynx;

import java.io.IOException;
import java.io.PrintWriter;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.TraceClassVisitor;

import asm.JynxClassReader;
import textifier.JynxText;

public class Jynxifier {

    public static Printer getInstance() {
        return new JynxText();
    }
    
    public static void main(final String[] args) throws IOException {
        byte[] ba = JynxClassReader.getClassBytes(args[0]);
        ClassReader cr = JynxClassReader.getClassReader(ba);
        Printer printer = new JynxText();
        try (PrintWriter pw = new PrintWriter(System.out)) {
            TraceClassVisitor tcv = new TraceClassVisitor(null, printer, pw);
            cr.accept(tcv, ClassReader.EXPAND_FRAMES);
        }
    }

}

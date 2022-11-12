package asm;

import org.objectweb.asm.ClassWriter;

import jynx2asm.TypeHints;

public class JynxClassWriter extends ClassWriter {

    private final TypeHints hints;
    
    public JynxClassWriter(int cwflags, TypeHints  hints) {
        super(cwflags);
        this.hints = hints;
    }

    @Override
    protected String getCommonSuperClass(final String type1, final String type2) {
        String common = hints.getCommonSuperClass(type1, type2);
        return common == null?super.getCommonSuperClass(type1, type2):common;
    }
}

package asm;

import org.objectweb.asm.ClassWriter;

import static jynx.Message.M241;
import static jynx.Message.M82;

import jynx.Global;
import jynx.LogIllegalArgumentException;

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
        if (common == null) {
            // "hints or redundant checkcasts needed to obtain common subtype of%n    %s and %s"
            throw new LogIllegalArgumentException(M241, type1,type2);
        }
        Global.LOG(M82,common,type1,type2); // "used hint for %s <- (%s,%s)"
        return common;
    }
}

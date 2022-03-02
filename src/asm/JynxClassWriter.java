package asm;

import org.objectweb.asm.ClassWriter;

import static jynx.Global.LOG;
import static jynx.Message.M157;
import static jynx.Message.M404;


import jynx.Global;
import jynx.GlobalOption;
import jynx.LogIllegalArgumentException;

import jynx2asm.TypeHints;

public class JynxClassWriter extends ClassWriter {

    private final TypeHints hints;
    private final boolean forname;
    
    public JynxClassWriter(int cwflags, TypeHints  hints) {
        super(cwflags);
        this.hints = hints;
        this.forname = Global.OPTION(GlobalOption.ALLOW_CLASS_FORNAME);
    }

    @Override
    protected String getCommonSuperClass(final String type1, final String type2) {
        if (forname) {
            // "class %s has used Class.forName(); java.runtime.version = %s"
           LOG(M157,this.getClass().getSimpleName(),System.getProperty("java.runtime.version"));
           return super.getCommonSuperClass(type1, type2);
        }
        String common = hints.getCommonSuperClass(type1, type2);
        if (common == null) {
            // "(redundant?) checkcasts or hint needed to obtain common supertype of%n    %s and %s"
            throw new LogIllegalArgumentException(M404, type1,type2);
        }
        return common;
    }
}

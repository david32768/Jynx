package jynx2asm;

import org.objectweb.asm.MethodVisitor;

import static jynx.Global.LOG;
import static jynx.Message.M106;
import static jynx.Message.M203;
import static jynx.Message.M279;

import jynx.Directive;

public class JynxCatch {

    private final JynxLabel fromLab;
    private final JynxLabel toLab;
    private final JynxLabel usingLab;
    private final String exception;
    private final Line line;

    private JynxCatch(JynxLabel fromLab, JynxLabel toLab, JynxLabel usingLab, String exception, Line line) {
        this.fromLab = fromLab;
        this.toLab = toLab;
        this.usingLab = usingLab;
        if (fromLab.equals(usingLab)) {
                LOG(M203); // "potential infinite loop - catch using label equals catch from label"
        }
        if (fromLab.equals(toLab)) {
                LOG(M279); // "empty catch block - from label equals to label"
        }
        this.exception = exception;
        this.line = line;
    }

    public JynxLabel fromLab() {
        return fromLab;
    }

    public JynxLabel toLab() {
        return toLab;
    }

    public JynxLabel usingLab() {
        return usingLab;
    }

    public void accept(MethodVisitor mv) {
        mv.visitTryCatchBlock(fromLab.asmlabel(), toLab.asmlabel(), usingLab.asmlabel(), exception);
    }
    
    public static JynxCatch getInstance(Line line, String fromname, String  toname,
            String usingname, String exception,  JynxLabelMap labelmap) {
        JynxLabel fromref = labelmap.useOfJynxLabel(fromname, line);
        JynxLabel toref = labelmap.useOfJynxLabel(toname, line);
        JynxLabel usingref = labelmap.codeUseOfJynxLabel(usingname, line);
        if (!fromref.isDefined() && !toref.isDefined() && !usingref.isDefined()) {
            JynxCatch jcatch = new JynxCatch(fromref, toref, usingref,exception,line);
            labelmap.addCatch(jcatch, line);
            return jcatch;
        } else {
            LOG(M106,Directive.dir_catch); // "labels in %s must not be defined yet"
            return null;
        }
    }

    @Override
    public String toString() {
        return String.format("%s %s %s %s", fromLab, toLab,usingLab,exception);
    }
}

package jynx2asm.frame;

import static jynx.Global.*;
import static jynx.Message.M221;

import jynx2asm.FrameClass;
import jynx2asm.FrameElement;

public class LocalFrame extends ConstantFrameArray {

    public LocalFrame(FrameElement... fes) {
        this(fes,fes.length);
    }

    public LocalFrame(FrameElement[] locals, int sz) {
        super(locals, sz, FrameClass.LOCALS);
    }


    // static because may be null
    public static LocalFrame combine(LocalFrame osf1,LocalFrame osf2) {
        if (osf1 == null) {
            return osf2;
        }
        if (osf2 == null || osf1.isEquivalent(osf2)) {
            return osf1;
        }
        int maxlen = Math.max(osf1.size(), osf2.size());
        FrameElement[] fes = new FrameElement[maxlen];
        for (int i = 0; i < maxlen;++i) {
            FrameElement fe1 = osf1.atUnchecked(i);
            FrameElement fe2 = osf2.atUnchecked(i);
            fes[i] = FrameElement.combine(fe1, fe2);
        }
        return new LocalFrame(fes);
    }
    
    public static boolean checkLabel(LocalFrame labosf, LocalFrame stackosf, LocalFrame afterlab) {
        boolean result = true;
        for (int i = 0; i < labosf.size();++i) {
            FrameElement labfe = labosf.atUnchecked(i);
            FrameElement stackfe = stackosf.atUnchecked(i);
            FrameElement afterfe = afterlab.atUnchecked(i);
            if (!stackfe.isCompatibleWithAfter(afterfe)) {
                LOG(M221,afterfe,i,stackfe); // "required %s for var %d but found %s"
            }
        }
        return result;
    }
    
}

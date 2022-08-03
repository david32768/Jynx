package jynx2asm;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static jynx.Global.*;
import static jynx.Message.M221;

public class LocalFrame {

    private final FrameElement[] locals;

    public LocalFrame(FrameElement[] locals, int sz) {
        assert sz <= locals.length;
        this.locals = Arrays.copyOf(locals, sz);
    }

    public LocalFrame(FrameElement... fes) {
        this(fes,fes.length);
    }

    public int size() {
        return locals.length;
    }

    public FrameElement at(int index) {
        if (index >= locals.length) {
            throw new IllegalArgumentException();
        }
        FrameElement fe = locals[index];
        return fe == null?FrameElement.UNUSED:fe;
    }
    
    public Stream<FrameElement> stream() {
        return Stream.of(locals)
                .map(fe -> fe == null?FrameElement.UNUSED:fe);
    }
    
    private FrameElement atUnchecked(int index) {
        assert index >= 0 && index < (1 << 16);
        if (index >= locals.length) {
            return FrameElement.UNUSED;
        }
        return at(index);
    }
    
    public String stringForm() {
        if (locals.length == 0) {
            return "empty";
        }
        return stream()
                .map(fe -> String.valueOf(fe.typeLetter()))
                .collect(Collectors.joining());
    }

    // static because may be null
    public static LocalFrame combine(LocalFrame osf1,LocalFrame osf2) {
        if (osf1 == null) {
            return osf2;
        }
        if (osf2 == null || osf1.equals(osf2)) {
            return osf1;
        }
        int maxlen = Math.max(osf1.size(), osf2.size());
        FrameElement[] fes = new FrameElement[maxlen];
        for (int i = 0; i < maxlen;++i) {
            FrameElement fe1 = osf1.atUnchecked(i);
            FrameElement fe2 = osf2.atUnchecked(i);
            if (fe1.typeLetter() == fe2.typeLetter()) {
                fes[i] = fe1;
            } else {
                fes[i] = FrameElement.ERROR;
            }
        }
        return new LocalFrame(fes);
    }
    
    public boolean check(LocalFrame osf2) {
        if (osf2 == null) {
            return true;
        }
        if (this.equals(osf2)) {
            return true;
        }
        for (int i = 0; i < this.size();++i) {
            FrameElement fe1 = this.at(i);
            FrameElement fe2 = osf2.atUnchecked(i);
            if (fe1.typeLetter() != fe2.typeLetter() && fe1 != FrameElement.ERROR) {
                return false;
            }
        }
        return true;
    }
    
    public static boolean checkLabel(LocalFrame labosf, LocalFrame stackosf, LocalFrame afterlab) {
        boolean result = true;
        for (int i = 0; i < labosf.size();++i) {
            FrameElement labfe = labosf.atUnchecked(i);
            FrameElement stackfe = stackosf.atUnchecked(i);
            FrameElement afterfe = afterlab.atUnchecked(i);
            if (afterfe == FrameElement.UNUSED || afterfe == FrameElement.IRRELEVANT || afterfe == FrameElement.ERROR) {
                continue;
            }
            if (afterfe.typeLetter() != stackfe.typeLetter()) {
                LOG(M221,afterfe,i,stackfe); // "required %s for var %d but found %s"
            }
        }
        return result;
    }
    
    @Override
    public String toString() {
        return stringForm();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof LocalFrame) {
            LocalFrame that = (LocalFrame)obj;
            return this.locals.length == that.locals.length && this.stringForm().equals(that.stringForm());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return stringForm().hashCode();
    }

}

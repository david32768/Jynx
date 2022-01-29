package jynx2asm;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static jynx.Global.*;
import static jynx.Message.M221;

public class OperandStackFrame {

    public static final OperandStackFrame EMPTY = new OperandStackFrame();
    public static final OperandStackFrame EXCEPTION = new OperandStackFrame(FrameElement.EXCEPTION);
    
    private final FrameElement[] stack;
    private int sz;

    public OperandStackFrame(FrameElement[] stack, int sz) {
        this.stack = Arrays.copyOf(stack, sz);
        assert sz <= stack.length;
        this.sz = sz;
    }

    public OperandStackFrame(FrameElement... fes) {
        this(fes,fes.length);
    }
  
    public OperandStackFrame(List<FrameElement> felist) {
        this(felist.toArray(new FrameElement[0]),felist.size());
    }
    
    public int size() {
        return sz;
    }

    public FrameElement at(int index) {
        if (index >= sz) {
            throw new IllegalArgumentException();
        }
        FrameElement fe = stack[index];
        return fe == null?FrameElement.UNUSED:fe;
    }
    
    public Stream<FrameElement> stream() {
        return Stream.of(stack)
                .limit(sz)
                .map(fe -> fe == null?FrameElement.UNUSED:fe);
    }
    
    private FrameElement atUnchecked(int index) {
        if (index < 0 || index > Short.MAX_VALUE) {
            throw new AssertionError();
        }
        if (index >= sz) {
            return FrameElement.UNUSED;
        }
        FrameElement fe = stack[index];
        return fe == null?FrameElement.UNUSED:fe;
    }
    
    public String stringForm() {
        if (sz == 0) {
            return "empty";
        }
        return stream()
                .map(fe -> String.valueOf(fe.typeLetter()))
                .collect(Collectors.joining());
    }

    public static OperandStackFrame combine(OperandStackFrame osf1,OperandStackFrame osf2) {
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
        return new OperandStackFrame(fes);
    }
    
    public static boolean check(OperandStackFrame osf1, OperandStackFrame osf2) {
        if (osf2 == null) {
            return true;
        }
        if (osf1.equals(osf2)) {
            return true;
        }
        for (int i = 0; i < osf1.size();++i) {
            FrameElement fe1 = osf1.atUnchecked(i);
            FrameElement fe2 = osf2.atUnchecked(i);
            if (fe1.typeLetter() != fe2.typeLetter() && fe1 != FrameElement.ERROR) {
                return false;
            }
        }
        return true;
    }
    
    public static boolean checkLabel(OperandStackFrame labosf, OperandStackFrame stackosf, OperandStackFrame afterlab) {
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
        if (obj instanceof OperandStackFrame) {
            OperandStackFrame that = (OperandStackFrame)obj;
            return this.sz == that.sz && this.stringForm().equals(that.stringForm());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return stringForm().hashCode();
    }

}

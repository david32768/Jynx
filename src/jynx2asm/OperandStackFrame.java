package jynx2asm;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static jynx.Message.M201;

import jvm.FrameType;
import jynx.LogIllegalArgumentException;

public class OperandStackFrame {

    public static final OperandStackFrame EMPTY = new OperandStackFrame();
    public static final OperandStackFrame EXCEPTION = new OperandStackFrame(FrameElement.EXCEPTION);
    
    private final FrameElement[] stack;

    public OperandStackFrame(FrameElement[] stack, int sz) {
        assert sz <= stack.length;
        this.stack = Arrays.copyOf(stack, sz);
    }

    public OperandStackFrame(FrameElement... fes) {
        this(fes,fes.length);
    }
  
    public static OperandStackFrame getInstance(List<Object> objs, boolean stack) {
        FrameElement[] framestack = new FrameElement[objs.size()];
        int i = 0;
        for (Object obj:objs) {
            FrameType ft = FrameType.fromObject(obj);
            FrameElement fe =  FrameElement.fromFrame(ft);
            if (stack && fe.isLocalsOnly()) {
                throw new LogIllegalArgumentException(M201, ft); // "%s can only occur in locals"
            }
            framestack[i++] = fe;
        }
        return new OperandStackFrame(framestack);
    }
    
    public int size() {
        return stack.length;
    }

    public FrameElement at(int index) {
        if (index < 0 || index >= stack.length) {
            throw new IllegalArgumentException();
        }
        FrameElement fe = stack[index];
        return fe == null?FrameElement.UNUSED:fe;
    }
    
    public FrameElement atUnchecked(int index) {
        if (index >= stack.length) {
            return FrameElement.UNUSED;
        }
        return at(index);
    }
    
    public Stream<FrameElement> stream() {
        return Stream.of(stack)
                .map(fe -> fe == null?FrameElement.UNUSED:fe);
    }
    
    public String stringForm() {
        if (stack.length == 0) {
            return "empty";
        }
        return FrameElement.stringForm(Stream.of(stack));
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
            int max = Math.max(this.stack.length, that.stack.length);
            for (int i = 0; i < max; ++i) {
                FrameElement fe1 = this.atUnchecked(i);
                FrameElement fe2 = that.atUnchecked(i);
                if (!FrameElement.equivalent(fe1, fe2)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return stringForm().hashCode();
    }

}

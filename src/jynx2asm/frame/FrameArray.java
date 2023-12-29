package jynx2asm.frame;

import java.util.Arrays;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

import static jynx.Global.LOG;
import static jynx.Message.M122;
import static jynx.Message.M190;
import static jynx.Message.M201;

import jvm.NumType;
import jynx.LogIllegalArgumentException;
import jynx2asm.FrameClass;
import jynx2asm.FrameElement;

public class FrameArray {

    private final static int MAXSZ = 1 << 16;

    static {
        assert MAXSZ == NumType.t_short.unsignedMaxvalue() + 1:
                String.format("%s != %s",MAXSZ,NumType.t_short.unsignedMaxvalue() + 1);
    }
    
    private final FrameElement[] array;
    private final FrameClass fc;
    private int sz;
    private int hwm;

    protected FrameArray(FrameElement[] array, int sz, FrameClass fc) {
        assert sz <= array.length && sz <= MAXSZ;
        this.array = Arrays.copyOf(array, sz);
        this.sz = sz;
        this.hwm = sz;
        this.fc = fc;
        assert validate(this.array, this.fc);
    }

    public FrameArray(int sz) {
        this(sz,sz);
    }

    public FrameArray() {
        this(0,MAXSZ);
    }

    private FrameArray(int initsz, int maxsz) {
        assert initsz <= maxsz;
        assert maxsz <= MAXSZ;
        this.array = new FrameElement[maxsz];
        this.sz = initsz;
        this.hwm = sz;
        this.fc = FrameClass.LOCALS;
        Arrays.fill(array, 0, maxsz, FrameElement.UNUSED);
    }

    private static boolean validate(FrameElement[] array, FrameClass fc) {
        return array.length == Stream.of(array)
                .filter(fe -> fe != null)
                .filter(fe -> fe.isValidInContext(fc))
                .count();
    }
    
    public int size() {
        return sz;
    }

    public int capacity() {
        return array.length;
    }

    public int hwm() {
        return hwm;
    }
    
    public FrameElement at(int index) {
        if (index < 0 || index >= sz) {
            // "invalid index (%d) for %s frame array [0,%d]"
            throw new LogIllegalArgumentException(M122, index, fc, sz);
        }
        FrameElement fe = array[index];
        return fe;
    }
    
    public FrameElement atUnchecked(int index) {
        if (index < 0 || index >= MAXSZ) {
            // "invalid index (%d) for %s frame array [0,%d]"
            throw new LogIllegalArgumentException(M122, index, fc, MAXSZ);
        }
        if (index >= sz) {
            return FrameElement.UNUSED;
        }
        return at(index);
    }
    
    public FrameElement get(int num) {
        FrameElement fe = at(num);
        int slots = fc.slots(fe);
        if (slots == 2) {
            FrameElement nextfe = at(num + 1);
            if (nextfe != fe.next()) {
                LOG(M190,num,fe.next(),nextfe); // "mismatched local %d: required %s but found %s"
            }
        }
        return fe;
    }
    
    public FrameElement getUnchecked(int num) {
        FrameElement fe = atUnchecked(num);
        int slots = fc.slots(fe);
        if (slots == 2) {
            FrameElement nextfe = atUnchecked(num + 1);
            if (nextfe != fe.next()) {
                LOG(M190,num,fe.next(),nextfe); // "mismatched local %d: required %s but found %s"
            }
        }
        adjust(num, slots);
        return fe;
    }
    
    public void set(int num, FrameElement fe) {
        if (fe == null || !fe.isValidInContext(fc)) {
            // "frame element %s is not valid in %s context"
            throw new LogIllegalArgumentException(M201, fe, fc);
        }
        int slots = fc.slots(fe);
        if (num < 0 || num > array.length - slots) {
            // "invalid index (%d) for %s frame array [0,%d]"
            throw new LogIllegalArgumentException(M122, num, fc, array.length);
        }
        array[num] = fe;
        if (slots == 2) {
            array[num + 1] = fe.next();
        }
        adjust(num, slots);
    }
    
    private void adjust(int num, int slots) {
        sz = Math.max(sz, num + slots);
        hwm = Math.max(sz, hwm);
        assert hwm <= MAXSZ;
    }
    
    public void set(FrameArray fa) {
        clear();
        int index = fa.sz;
        if (index < 0 || index > array.length) {
            // "invalid index (%d) for %s frame array [0,%d]"
            throw new LogIllegalArgumentException(M122, index, fc, array.length);
        }
        System.arraycopy(fa.array, 0, array, 0, fa.sz);
        sz = fa.sz;
        hwm = Math.max(hwm,sz);
    }
    
    public void clear() {
        Arrays.fill(array, 0, sz, FrameElement.UNUSED);
        this.sz = 0;
    }
    
    public Stream<FrameElement> stream() {
        return Stream.of(array)
                .limit(sz);
    }
    
    public String stringForm() {
        if (array.length == 0) {
            return "empty";
        }
        return FrameElement.stringForm(stream());
    }

    @Override
    public String toString() {
        return stringForm();
    }

    public boolean isCompatibleWith(FrameArray fa2) {
        return equivalent(this, fa2, (fe1,fe2) -> fe1.isCompatibleWith(fe2));
    }
    
    public boolean isEquivalent(FrameArray fa2) {
        return equivalent(this, fa2,  FrameElement::equivalent);
    }
    
    private static boolean equivalent(FrameArray fa1, FrameArray fa2, BiPredicate<FrameElement,FrameElement> compfn) {
        if (fa1 == fa2) {
            return true;
        }
        if (fa1 == null || fa2 == null || fa1.fc != fa2.fc) {
            return false;
        }
        int max = Math.max(fa1.sz, fa2.sz);
        for (int i = 0; i < max; ++i) {
            FrameElement fe1 = fa1.atUnchecked(i);
            FrameElement fe2 = fa2.atUnchecked(i);
            if (!compfn.test(fe1, fe2)) {
                return false;
            }
        }
        return true;
    }
    
    public LocalFrame asLocalFrame() {
        assert fc == FrameClass.LOCALS;
        return new LocalFrame(array,sz);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof FrameArray) {
            FrameArray that = (FrameArray)obj;
            return this.fc == that.fc && this.sz == that.sz && equivalent(this, that, (fe1,fe2) -> fe1 == fe2);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return stringForm().hashCode();
    }

}

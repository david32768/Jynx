package jynx2asm;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Optional;

import static jynx.Global.LOG;
import static jynx.Message.*;

import jvm.NumType;
import jynx.LogIllegalArgumentException;

public class LocalVars {
    
    private static final int MAXSTACK = 1 << 16;
    
    private final LimitValue localsz;
    private final FrameElement[] locals;
    private final boolean isStatic;
    private int sz;
    private boolean startblock;
    private OperandStackFrame lastlocals;
    private final BitSet readVars;
    private final BitSet writeVars;
    private final BitSet typedVars;
    private final int parmsz;
    
    
    public LocalVars(OperandStackFrame parmlocals, boolean isStatic) {
        this.localsz = new LimitValue(LimitValue.Type.locals);
        this.locals = new FrameElement[MAXSTACK];
        this.isStatic = isStatic;
        this.sz = 0;
        this.startblock = true;
        this.readVars = new BitSet();
        this.writeVars = new BitSet();
        this.typedVars = new BitSet();
        visitFrame(parmlocals, Optional.empty());  // wiil set startblock to false
        this.parmsz = sz;
    }

    public OperandStackFrame currentOSF() {
        return new OperandStackFrame(locals,sz);
    }
    
    public void setLimit(int num, Line line) {
        sz = Math.max(sz,num);
        localsz.setLimit(num, line);        
    }

    public int absolute(int relnum) {
        assert relnum >= 0;
        int abs = isStatic?0:1; // preserve this and start parms at rel 0
        int rel = 0;
        for (int i = abs; i < sz; ++i)  {
            if (rel == relnum) {
                return abs;
            }
            FrameElement fe = locals[i];
            if (fe.isTwo()) {
                if (i >= sz - 1 || !fe.checkNext(locals[i+1])) {
                    // "unable to calculate relative local position %d:%n   current abs = %d max = %d locals = %s"
                    LOG(M246,relnum,abs,sz,this);
                    return 0;
                }
                abs += 2;
                ++i;
            } else {
                ++abs;
            }
            ++rel;
        }
        if (rel == relnum) {
            return abs;
        }
        // "unable to calculate relative local position %d:%n   current abs = %d max = %d locals = %s"
        LOG(M246,relnum,abs,sz,this);
        return 0;
    }
    
    public void startBlock() {
        lastlocals = currentOSF();
        clear();
        startblock = true;
    }

    public int max() {
        return localsz.checkedValue();
    }

    private void adjustMax(FrameElement fe,int num) {
        int vsz = fe.slots();
        int maxv = num + vsz;
        if (!NumType.t_short.isInUnsignedRange(maxv)) {
            throw new LogIllegalArgumentException(M74,num,vsz); // "Invalid variable number (%d + %d is not unsigned short)"
        }
        localsz.adjust(maxv);
        sz = Math.max(sz, maxv);
    }
    
    // if state changed then method peek needs changing
    public FrameElement load(int num) {
        FrameElement fe = locals[num];
        if (fe == null) {
            fe = FrameElement.UNUSED;
        } else if (fe.isTwo()) {
            FrameElement nextfe = locals[num + 1];
            if (nextfe == null) {
                nextfe = FrameElement.UNUSED;
            }
            if (nextfe != fe.next()) {
                LOG(M190,num,fe.next(),nextfe); // "mismatched local %d: required %s but found %s"
            }
        }
        return fe;
    }
    
    // do not change state
    public FrameElement peek(int num) {
        return load(num);
    }
    
    // aload cannot be used to load a ret address
    public FrameElement loadType(char type, int num) {
        FrameElement fe;
        if (num < sz) {
            fe = load(num);
            char local0 = fe.typeLetter();
            if (type != local0) {
                LOG(M190,num,FrameElement.fromLocal(type),fe); // "mismatched local %d: required %s but found %s"
                if (fe == FrameElement.UNUSED) {
                    fe = FrameElement.fromLocal(type);
                    store(num,fe);
                }
            }
        } else {
            LOG(M212,num,sz); // "attempting to load variable %d but current max is %d"
            fe = FrameElement.fromLocal(type);
            store(num, fe);
        }
        adjustMax(fe,num);
        readVars.set(num);
        if (fe.isTwo()) {
            readVars.set(num + 1);
        }
        return fe;
    }

    public void checkChar(char type, int num) {
        loadType(type, num);
    }
    
    private void store(int num, FrameElement fe) {
        locals[num] = fe;
        writeVars.set(num);
        if (fe.isTwo()) {
            locals[num + 1] = fe.next();
            writeVars.set(num + 1);
        }
    }
    
    public void storeFrameElement(FrameElement fe, int num) {
        adjustMax(fe,num);
        store(num,fe);
    }

    public void preVisitJvmOp(Optional<JynxLabel> lastLab) {
        if (startblock) {
            JynxLabel label = lastLab.get();
            if (label.getLocals() == null) {
                // "label %s defined before use - locals assumed as before last unconditional op"
                LOG(label.definedLine(),M213,label);
                setLocals(lastlocals, lastLab);
            } else {
                setLocals(label.getLocals(),Optional.empty());
            }
        }
        lastLab.ifPresent(JynxLabel::freeze);
        lastlocals = null;
        startblock = false;
    }

    public void typedVar(int num) {
        typedVars.set(num);
    }
    
    private void setLocals(OperandStackFrame osf, Optional<JynxLabel> lastLab) {
        clear();
        sz = osf.size();
        for (int i = 0; i < sz;++i) {
            locals[i] = osf.at(i);
        }
        localsz.adjust(sz);
        lastLab.ifPresent(lab-> updateLocal(lab,osf));
        startblock = false;
    }
    
    private void mergeLocal(JynxLabel label, OperandStackFrame b4osf) {
        OperandStackFrame labosf = label.getLocals();
        if (labosf == null) {
            return;
        }
        OperandStackFrame osf = OperandStackFrame.combine(b4osf, labosf);
        clear();
        sz = osf.size();
        for (int i = 0; i < sz;++i) {
            locals[i] = osf.at(i);
        }
        localsz.adjust(sz);
    }
    
    public void visitLabel(JynxLabel label, Optional<JynxLabel> lastLab) {
        OperandStackFrame b4osf;
        OperandStackFrame labosf = label.getLocals();
        if (startblock) {
            if (labosf != null) {
                setLocals(labosf, lastLab);
            }
            b4osf = lastlocals;
        } else {
            b4osf = currentOSF();
            updateLocal(label,b4osf);
            mergeLocal(label,b4osf);
        }
        label.setLocalsBefore(b4osf);
    }

    public void visitAlias(JynxLabel alias, JynxLabel base, Optional<JynxLabel> lastLab) {
        visitLabel(base,lastLab);
    }
    
    public final void visitFrame(OperandStackFrame osf, Optional<JynxLabel> lastLab) {
        boolean check = !startblock;
        sz = 0;
        for (int i = 0; i < osf.size(); ++i) {
            FrameElement fe = osf.at(i);
            char type = fe.typeLetter();
            if (check && type != FrameElement.ERROR.typeLetter()) {
                int num = sz;
                sz += 2;
                loadType(type,num);
                sz -=2;
            }
            storeFrameElement(fe,sz);
        }
        localsz.adjust(sz);
        if (startblock) {
            lastLab.ifPresent(lab->setFrame(lab,osf));
        }
        startblock = false;
    }
    
    private void setFrame(JynxLabel label,OperandStackFrame osf) {
        label.setLocalsFrame(osf);
        updateLocal(label, osf);
    }
    
    public void clear() {
        Arrays.fill(locals,null);
        sz = 0;
    }
    
    public String stringForm() {
        return currentOSF().stringForm();
    }

    private void updateLocal(JynxLabel label, OperandStackFrame osf) {
        label.updateLocal(osf);
    }

    public boolean visitVarDirective(FrameElement fe, int num) {
        boolean ok = writeVars.get(num);
        if (fe.isTwo()) {
            ok &= writeVars.get(num + 1);
        }
        return ok;
    }
    
    private final static int MAX_GAP = 0;
    
    public void visitEnd() {
        int last = 0;
        for (int i = writeVars.nextSetBit(0); i >= 0; i = writeVars.nextSetBit(i+1)) {
            int gap = i - last - 1;
            if (gap > MAX_GAP) {
                LOG(M56, gap,last,i); // "gap %d between local variables: %d - %d"
            }
            last = i;
            if (i == Integer.MAX_VALUE) {
                break; // or (i+1) would overflow
            }
        }
        BitSet unreadvars = (BitSet)writeVars.clone();
        unreadvars.andNot(readVars);
        if (unreadvars.nextSetBit(parmsz) >= 0) {
            String ranges = rangeString(parmsz,unreadvars);
             // "local variables [%s ] are written but not read"
            LOG(M60,ranges);
        }
        BitSet unwrittenvars = (BitSet)readVars.clone();
        unwrittenvars.andNot(writeVars);
        if (!unwrittenvars.isEmpty()) {
            String ranges = rangeString(0,unwrittenvars);
            // "local variables [%s ] are read but not written"
            LOG(M65,ranges);
        }
        unwrittenvars = (BitSet)typedVars.clone();
        unwrittenvars.andNot(writeVars);
        if (!unwrittenvars.isEmpty()) {
            String ranges = rangeString(0,unwrittenvars);
            // "Annotation for unknown variables [%s ]"
            LOG(M223,ranges);
        }
    }
    
    private String rangeString(int start,BitSet bitset) {
        StringBuilder sb = new StringBuilder();
        int last = -2;
        char spacer = ' ';
        for (int i = bitset.nextSetBit(start); i >= 0; i = bitset.nextSetBit(i+1)) {
            if (i == last + 1) {
                spacer = '-';
            } else {
                if (last >= 0 && spacer != ' ') {
                    sb.append(spacer).append(last);
                }
                spacer = ' ';
                sb.append(' ').append(i);
            }
            last = i;
            if (i == Integer.MAX_VALUE) {
                break; // or (i+1) would overflow
            }
        }
        if (spacer != ' ') {
            sb.append(spacer).append(last);
        }
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return  stringForm();
    }
    
}

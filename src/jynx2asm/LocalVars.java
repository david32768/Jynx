package jynx2asm;

import java.util.Arrays;
import java.util.List;
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
    private LocalFrame lastlocals;
    private final VarAccess varAccess;
    private final int parmsz;
    
    
    private LocalVars(OperandStackFrame parmlocals, boolean isStatic) {
        this.localsz = new LimitValue(LimitValue.Type.locals);
        this.locals = new FrameElement[MAXSTACK];
        this.isStatic = isStatic;
        this.varAccess = new VarAccess();
        this.sz = 0;
        this.startblock = true;
        visitFrame(parmlocals, Optional.empty());  // wiil set startblock to false
        this.parmsz = sz;
        varAccess.completeInit(this.parmsz);
    }

    public static LocalVars getInstance(List<Object> localstack, boolean isStatic) {
        return new LocalVars(OperandStackFrame.getInstance(localstack,false),isStatic);
    }

    public LocalFrame currentFrame() {
        return new LocalFrame(locals,sz);
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
        lastlocals = currentFrame();
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
    
    public FrameElement peek(int num) {
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
    
    private FrameElement peekType(char type, int num) {
        FrameElement fe;
        if (num < sz) {
            fe = peek(num);
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
        return fe;
    }

    // aload cannot be used to load a ret address
    public FrameElement loadType(char type, int num) {
        FrameElement fe = peekType(type, num);
        varAccess.setRead(num, fe);
        return fe;
    }
    
    public void checkChar(char type, int num) {
        loadType(type, num);
    }
    
    private void store(int num, FrameElement fe) {
        locals[num] = fe;
        if (fe.isTwo()) {
            locals[num + 1] = fe.next();
        }
        adjustMax(fe,num);
    }
    
    public void storeFrameElement(FrameElement fe, int num) {
        store(num,fe);
        if (fe == FrameElement.UNUSED || fe == FrameElement.ERROR) {
            return;
        }
        varAccess.setWrite(num, fe);
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

    private void setLocals(LocalFrame osf, Optional<JynxLabel> lastLab) {
        clear();
        sz = osf.size();
        for (int i = 0; i < sz;++i) {
            locals[i] = osf.at(i);
        }
        localsz.adjust(sz);
        lastLab.ifPresent(lab-> updateLocal(lab,osf));
        startblock = false;
    }
    
    private void mergeLocal(JynxLabel label, LocalFrame b4osf) {
        LocalFrame labosf = label.getLocals();
        if (labosf == null) {
            return;
        }
        LocalFrame osf = LocalFrame.combine(b4osf, labosf);
        clear();
        sz = osf.size();
        for (int i = 0; i < sz;++i) {
            locals[i] = osf.at(i);
        }
        localsz.adjust(sz);
    }
    
    public void visitLabel(JynxLabel label, Optional<JynxLabel> lastLab) {
        LocalFrame b4osf;
        LocalFrame labosf = label.getLocals();
        if (startblock) {
            if (labosf != null) {
                setLocals(labosf, lastLab);
            }
            b4osf = lastlocals;
        } else {
            b4osf = currentFrame();
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
            if (check && type != FrameElement.TOP.typeLetter()) {
                int num = sz;
                sz += 2;
                peekType(type,num);
                sz -=2;
            }
            varAccess.setFrame(sz, fe);
            if (fe == FrameElement.TOP) { // as '.stack local Top' means error(local usage ambiguous) or not used
                fe = FrameElement.ERROR;
            }
            store(sz,fe);
        }
        localsz.adjust(sz);
        if (startblock) {
            lastLab.ifPresent(lab->setFrame(lab,currentFrame()));
        }
        startblock = false;
    }
    
    private void setFrame(JynxLabel label,LocalFrame osf) {
        label.setLocalsFrame(osf);
        updateLocal(label, osf);
    }
    
    public void clear() {
        Arrays.fill(locals,null);
        sz = 0;
    }
    
    public String stringForm() {
        return currentFrame().stringForm();
    }

    private void updateLocal(JynxLabel label, LocalFrame osf) {
        label.updateLocal(osf);
    }

    public boolean visitVarDirective(FrameElement fe, int num) {
        return varAccess.checkWritten(fe, num);
    }
    
    public void visitVarAnnotation(int num) {
        varAccess.setTyped(num);
    }
    
    public void visitEnd() {
        varAccess.visitEnd();
    }
    
    @Override
    public String toString() {
        return  stringForm();
    }
    
}

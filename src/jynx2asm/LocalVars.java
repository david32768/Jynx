package jynx2asm;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static jynx.Global.LOG;
import static jynx.Global.OPTION;
import static jynx.Message.*;

import jvm.NumType;
import jynx.GlobalOption;
import jynx.LogIllegalArgumentException;

public class LocalVars {

    private enum VarState {
        NOT_SET,
        ACTUAL,
        SYMBOLIC,
    }
    
    private static final int MAXSTACK = 1 << 16;
    private static final char SYMBOL_MARKER = '$';
    
    private final LimitValue localsz;
    private final FrameElement[] locals;
    private final boolean isStatic;
    private final VarAccess varAccess;
    private final int parmsz;
    private final Map<String,Integer> varmap;
    
    private int sz;
    private boolean startblock;
    private LocalFrame lastlocals;
    private VarState varState;
    
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
        this.varState = OPTION(GlobalOption.USE_STACK_MAP)?VarState.ACTUAL:VarState.NOT_SET;
        this.varmap = new HashMap<>();
    }

    private void setParms(OperandStackFrame osf) {
        int parm0 = 0;
        if (!isStatic) {
            varmap.put(SYMBOL_MARKER + "this",0);
            parm0 = 1;
        }
        int current = parm0;
        for (int i = parm0; i < osf.size(); ++i) {
            FrameElement fe = osf.at(i);
            String parmnumstr = "" + (i - parm0);
            varmap.put(SYMBOL_MARKER + parmnumstr, current);
            current += fe.isTwo()?2:1;
        }
    }
    
    public static LocalVars getInstance(List<Object> localstack, boolean isStatic) {
        OperandStackFrame parmosf = OperandStackFrame.getInstance(localstack,false);
        LocalVars lv = new LocalVars(parmosf,isStatic);
        lv.setParms(parmosf);
        return lv;
    }

    public LocalFrame currentFrame() {
        return new LocalFrame(locals,sz);
    }
    
    public void setLimit(int num, Line line) {
        sz = Math.max(sz,num);
        localsz.setLimit(num, line);        
    }

    private int getVarNumber(Token token, FrameElement fe) {
        String tokenstr = token.asString();
        if (varState == VarState.NOT_SET) {
            varState = tokenstr.charAt(0) == SYMBOL_MARKER?VarState.SYMBOLIC:VarState.ACTUAL;
        }
        switch(varState) {
            case ACTUAL:
                return getActualVarNumber(token);
            case SYMBOLIC:
                return getSymbolicVarNumber(token,fe);
            default:
                throw new EnumConstantNotPresentException(varState.getClass(), varState.name());
        }
    }
    
    private int getSymbolicVarNumber(Token token, FrameElement fe) {
        String tokenstr = token.asString();
        if (tokenstr.charAt(0) != SYMBOL_MARKER) {
            // "cannot mix absolute and relative local variables"
            throw new LogIllegalArgumentException(M255);
        }
        Integer number = varmap.get(tokenstr);
        if (fe != null) {
            if (number == null) {
                number = sz;
                varmap.put(tokenstr,sz);
            } else {
                if (peek(number) != fe) {
                    // "different types for %s; was %s but now %s"
                    LOG(M204,tokenstr,peek(number),fe);
                }
            }
        } else if (number == null) {
            //"unknown variable: %s"
            LOG(M211,tokenstr);
        }
        return number;
    }
    
    private int getActualVarNumber(Token token) {
        String tokenstr = token.asString();
        if (tokenstr.charAt(0) == SYMBOL_MARKER) {
            // "cannot mix absolute and relative local variables"
            throw new LogIllegalArgumentException(M255);
        }
        return token.asUnsignedShort();
    }
    
    public int loadVarNumber(Token token) {
        return getVarNumber(token,null);
    }
    
    public FrameElement peekVarNumber(Token token) {
        int num = loadVarNumber(token);
        return peek(num);
    }

    private int storeVarNumber(Token token, FrameElement fe) {
        return getVarNumber(token,fe);
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
    
    private FrameElement peek(int num) {
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
            if (type != local0 && type != 'A' && fe != FrameElement.THIS) {
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
    
    public int storeFrameElement(FrameElement fe, Token vartoken) {
        int num = storeVarNumber(vartoken, fe);
        store(num,fe);
        if (fe != FrameElement.UNUSED && fe != FrameElement.ERROR) {
            varAccess.setWrite(num, fe);
        }
        return num;
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
            if (fe == FrameElement.TOP) { // as '.stack local Top' means error (ambiguous) or not used
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

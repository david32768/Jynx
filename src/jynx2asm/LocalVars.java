package jynx2asm;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Optional;

import org.objectweb.asm.tree.ParameterNode;

import static jynx.Global.LOG;
import static jynx.Global.OPTION;
import static jynx.Message.*;

import jvm.NumType;
import jynx.GlobalOption;
import jynx.LogIllegalArgumentException;

public class LocalVars {

    private static final int MAXLOCALS = 1 << 16;
    
    private final LimitValue localsz;
    private final FrameElement[] locals;
    private final boolean isStatic;
    private final VarAccess varAccess;
    private final int parmsz;
    private final boolean symbolic;
    private final SymbolicVars symVars;
    
    private int sz;
    private boolean startblock;
    private LocalFrame lastlocals;
    
    private LocalVars(OperandStackFrame parmlocals, boolean isStatic, BitSet finalparms) {
        this.localsz = new LimitValue(LimitValue.Type.locals);
        this.locals = new FrameElement[MAXLOCALS];
        this.isStatic = isStatic;
        this.varAccess = new VarAccess(finalparms);
        this.sz = 0;
        this.startblock = true;
        visitFrame(parmlocals, Optional.empty());  // wiil set startblock to false
        this.parmsz = sz;
        varAccess.completeInit(this.parmsz);
        this.symbolic = OPTION(GlobalOption.SYMBOLIC_LOCAL);
        this.symVars = new SymbolicVars(isStatic);
    }

    public static LocalVars getInstance(List<Object> localstack, List<ParameterNode> parameters,
            boolean isStatic, BitSet finalparms) {
        OperandStackFrame parmosf = OperandStackFrame.getInstance(localstack,false);
        LocalVars lv = new LocalVars(parmosf,isStatic,finalparms);
        if (lv.symbolic) {
            lv.setParms(parmosf);
            lv.setParmNames(parameters);
        }
        return lv;
    }

    private void setParms(OperandStackFrame osf) {
        assert symbolic;
        int parm0 = isStatic? 0: 1;
        int current = parm0;
        for (int i = parm0; i < osf.size(); ++i) {
            FrameElement fe = osf.at(i);
            String parmnumstr = "" + (i - parm0);
            symVars.setNumber(parmnumstr, fe, current);
            current += fe.isTwo()?2:1;
        }
    }

    private void setParmNames(List<ParameterNode> parameters) {
        assert symbolic;
        if (parameters != null) {
            int parmnum = 0;
            for (ParameterNode parameter:parameters) {
                symVars.setAlias(parmnum, parameter.name);
                ++parmnum;
            }
        }
    }
    
    public LocalFrame currentFrame() {
        return new LocalFrame(locals,sz);
    }
    
    public void setLimit(int num, Line line) {
        sz = Math.max(sz,num);
        localsz.setLimit(num, line);        
    }

    private int getVarNumber(Token token, FrameElement fe) {
        return symbolic? getSymbolicVarNumber(token,fe): getActualVarNumber(token);
    }
    
    private int getSymbolicVarNumber(Token token, FrameElement fe) {
        String tokenstr = token.asString();
        int number = symVars.getNumber(tokenstr, fe, sz);
        if (number == sz) {
            adjustMax(symVars.getFrameElement(number),number);
        }
        return number;
    }

    private int getActualVarNumber(Token token) {
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
        FrameElement required = FrameElement.fromLocal(type);
        return peekType(num, required);
    }

    private FrameElement peekType(int num, FrameElement required) {
        FrameElement fe;
        if (num < sz) {
            fe = peek(num);
            if (!fe.matchLocal(required)) {
                LOG(M190,num,required,fe); // "mismatched local %d: required %s but found %s"
                if (fe == FrameElement.UNUSED) {
                    store(num,required);
                }
            }
        } else {
            LOG(M212,num,sz); // "attempting to load variable %d but current max is %d"
            fe = required;
            store(num, required);
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
        if (fe != FrameElement.UNUSED) {
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
            if (check && fe != FrameElement.TOP) {
                int num = sz;
                sz += 2;
                peekType(num,fe);
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

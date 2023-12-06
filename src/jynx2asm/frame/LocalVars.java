package jynx2asm.frame;

import java.util.BitSet;
import java.util.List;
import java.util.Optional;

import org.objectweb.asm.tree.ParameterNode;

import static jynx.Global.LOG;
import static jynx.Global.OPTION;
import static jynx.Message.*;

import jynx.GlobalOption;
import jynx2asm.FrameElement;
import jynx2asm.JynxLabel;
import jynx2asm.LimitValue;
import jynx2asm.Line;
import jynx2asm.SymbolicVars;
import jynx2asm.Token;
import jynx2asm.VarAccess;

public class LocalVars {

    private final LimitValue localsz;
    private final FrameArray locals;
    private final boolean isStatic;
    private final VarAccess varAccess;
    private final int parmsz;
    private final boolean symbolic;
    private final SymbolicVars symVars;
    
    private boolean startblock;
    private LocalFrame lastlocals;
    
    private LocalVars(StackMapLocals parmlocals, boolean isStatic, BitSet finalparms, SymbolicVars symvars) {
        this.localsz = new LimitValue(LimitValue.Type.locals);
        this.locals = new FrameArray();
        this.isStatic = isStatic;
        this.varAccess = new VarAccess(finalparms);
        this.startblock = true;
        visitFrame(parmlocals, Optional.empty());  // wiil set startblock to false
        this.parmsz = locals.size();
        varAccess.completeInit(this.parmsz);
        this.symbolic = symvars != null;
        this.symVars = symvars;
    }

    public static LocalVars getInstance(List<Object> localstack, List<ParameterNode> parameters,
            boolean isStatic, BitSet finalparms) {
        StackMapLocals parmosf = StackMapLocals .getInstance(localstack);
        boolean symbolic = OPTION(GlobalOption.SYMBOLIC_LOCAL);
        SymbolicVars symvar = symbolic? SymbolicVars.getInstance(isStatic, parmosf, parameters): null;
        return new LocalVars(parmosf,isStatic,finalparms, symvar);
    }

    public LocalFrame currentFrame() {
        return locals.asLocalFrame();
    }
    
    public void setLimit(int num, Line line) {
        localsz.setLimit(num, line);        
    }

    public int loadVarNumber(Token token) {
        return symbolic?
                symVars.getLoadNumber(token.asString()):
                getActualVarNumber(token);
    }
    
    private int storeVarNumber(Token token, FrameElement fe) {
        return symbolic?
                symVars.getStoreNumber(token.asString(), fe):
                getActualVarNumber(token);
    }
    
    private int getActualVarNumber(Token token) {
        return token.asUnsignedShort();
    }
    
    public FrameElement peekVarNumber(Token token) {
        int num = loadVarNumber(token);
        return locals.getUnchecked(num);
    }

    public void startBlock() {
        lastlocals = currentFrame();
        clear();
        startblock = true;
    }

    public int max() {
        return localsz.checkedValue();
    }

    private FrameElement peekType(char type, int num) {
        FrameElement required = FrameElement.fromLocal(type);
        return peekType(num, required);
    }

    private FrameElement peekType(int num, FrameElement required) {
        FrameElement fe = locals.getUnchecked(num);
        if (!fe.matchLocal(required)) {
            LOG(M190,num,required,fe); // "mismatched local %d: required %s but found %s"
            if (fe == FrameElement.UNUSED) {
                store(num,required);
            }
        }
        localsz.adjust(locals.size());
        return fe;
    }

    // aload cannot be used to load a ret address
    public FrameElement loadType(char type, int num) {
        FrameElement fe = peekType(type, num);
        varAccess.setRead(num, fe);
        return fe;
    }

    private void store(int num, FrameElement fe) {
        locals.set(num, fe);
        localsz.adjust(locals.size());
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
        locals.set(osf);
        localsz.adjust(locals.size());
        lastLab.ifPresent(lab-> updateLocal(lab,osf));
        startblock = false;
    }
    
    private void mergeLocal(JynxLabel label, LocalFrame b4osf) {
        LocalFrame labosf = label.getLocals();
        if (labosf == null) {
            return;
        }
        LocalFrame osf = LocalFrame.combine(b4osf, labosf);
        locals.set(osf);
        localsz.adjust(locals.size());
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
    
    public final void visitFrame(StackMapLocals smlocals, Optional<JynxLabel> lastLab) {
        if (!startblock) {
            checkFrame(smlocals);
        }
        locals.clear();
        for (int i = 0; i < smlocals.size(); ++i) {
            int sz = locals.size();
            FrameElement fe = smlocals.at(i);
            varAccess.setFrame(sz, fe);
            if (fe == FrameElement.TOP) { // as '.stack local Top' means error (ambiguous) or not used
                fe = FrameElement.ERROR;
            }
            store(sz,fe);
        }
        localsz.adjust(locals.size());
        if (startblock) {
            lastLab.ifPresent(lab->setFrame(lab,currentFrame()));
        }
        startblock = false;
    }
    
    private void checkFrame(StackMapLocals smlocals) {
        int num = 0;
        for (int i = 0; i < smlocals.size(); ++i) {
            FrameElement fe = smlocals.at(i);
            if (fe != FrameElement.TOP) {
                peekType(num,fe);
            }
            num += fe.slots();
        }
    }
    
    private void setFrame(JynxLabel label,LocalFrame osf) {
        label.setLocalsFrame(osf);
        updateLocal(label, osf);
    }
    
    public void clear() {
        locals.clear();
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

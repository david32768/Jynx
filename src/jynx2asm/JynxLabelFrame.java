package jynx2asm;

import java.util.Arrays;

import static jynx.Global.*;
import static jynx.Message.*;
import jynx.LogIllegalArgumentException;
import jynx.ReservedWord;

public class JynxLabelFrame {

    private final String name;
    private OperandStackFrame osfLocals;
    private OperandStackFrame osfStack;

    private OperandStackFrame osfLocalsFrame;

    private OperandStackFrame osfLocalsBefore;
    private FrameElement[] afterframe;

    public JynxLabelFrame(String name) {
        this.name = name;
        this.osfLocals = null;
        this.osfLocalsFrame = null;
        this.osfLocalsBefore = null;
    }

    public JynxLabelFrame merge(JynxLabelFrame  alias) {
        assert alias.osfLocalsFrame == null;
        assert alias.osfLocalsBefore == null;
        assert alias.afterframe == null;
        updateLocal(alias.osfLocals);
        updateStack(alias.osfStack);
        return this;
    }
    
    public OperandStackFrame locals() {
        return osfLocals;
    }

    public OperandStackFrame stack() {
        return osfStack;
    }
    
    public void updateLocal(OperandStackFrame osfx) {
        if (osfx == null) {
            return;
        }
        OperandStackFrame osfy = OperandStackFrame.combine(osfLocals, osfx);
        if (isFrozen()) {
            OperandStackFrame.checkLabel(osfLocals, osfx, new OperandStackFrame(afterframe));
        } else {
            osfLocals = osfy;
        }
        if (osfLocalsFrame != null && !OperandStackFrame.check(osfLocalsFrame,osfLocals)) {
              throw new LogIllegalArgumentException(M216,osfLocalsFrame,osfLocals); // "frame locals %s incompatible with current locals %s"
        }
    }

    private boolean isFrozen() {
        return afterframe != null;
    }
    
    public void updateStack(OperandStackFrame osf) {
        if (osf == null) {
            return;
        }
        if (osfStack == null) {
            osfStack = osf;
        } else if (!osfStack.equals(osf)) {
            LOG(M185,ReservedWord.res_stack,name,osfStack,osf); // "%s required for label %s is %s but currently is %s"
        }
    }
    
    public void setLocalsFrame(OperandStackFrame osfx) {
        osfLocalsFrame = osfx;
        updateLocal(osfx);
    }

    public void setLocalsBefore(OperandStackFrame osfLocalsBefore) {
        this.osfLocalsBefore = osfLocalsBefore;
    }

    public OperandStackFrame localsBefore() {
        return osfLocalsBefore;
    }
    
    public void load(FrameElement fe, int num) {
        assert isFrozen();
        if (num < afterframe.length && afterframe[num] == FrameElement.UNUSED){
            afterframe[num] = fe;
        } else if (fe.isTwo() && num + 1 < afterframe.length && afterframe[num + 1] == FrameElement.UNUSED) {
            afterframe[num + 1] = fe.next();
        }
    }
    
    public void store(FrameElement fe, int num) {
        assert isFrozen();
        if (num < afterframe.length && afterframe[num] == FrameElement.UNUSED){
            afterframe[num] = FrameElement.IRRELEVANT;
        }
        if (fe.isTwo() && num + 1 < afterframe.length && afterframe[num + 1] == FrameElement.UNUSED) {
            afterframe[num + 1] = FrameElement.IRRELEVANT;
        }
    }
    
    public void freeze() {
        afterframe = new FrameElement[osfLocals.size()];
        Arrays.fill(afterframe, FrameElement.UNUSED);
    }
    
    public String print() {
        return String.format("label %s locals = %s stack = %s%n", name,osfLocals,osfStack);
    }
}

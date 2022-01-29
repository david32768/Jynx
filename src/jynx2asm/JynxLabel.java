package jynx2asm;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.objectweb.asm.Label;

import static jynx.Global.*;
import static jynx.Message.*;

public class JynxLabel {

    private final String name;
    private Line defined;
    private boolean usedInCode;
    private final ArrayList<Line> usedList;
    private final ArrayList<Line> weakList;
    private final Label asmlab;

    private JynxLabelFrame jlf;
    private final List<JynxCatch> catchList;

    private final Map<JynxLabel,Line> startLabels;

    public JynxLabel(String name) {
        this.name = name;
        this.defined = null;
        this.usedInCode = false;
        this.usedList = new ArrayList<>();
        this.weakList = new ArrayList<>();
        this.asmlab = new Label();
        this.jlf = new JynxLabelFrame(name);
        this.startLabels = new LinkedHashMap<>();
        this.catchList = new ArrayList<>();
    }
    
    public boolean isDefined() {
        return defined != null;
    }

    public boolean isUnused() {
        return isDefined() && usedList.isEmpty() && weakList.isEmpty();
    }

    public boolean isUsedInCode() {
        return usedInCode;
    }

    public void addAfter(JynxLabel start, Line line) {
        startLabels.put(start,line);
    }

    public boolean isLessThan(JynxLabel after) {
        return this.isDefined() && after.isDefined() && this.definedLine().getLinect() < after.definedLine().getLinect();
    }
    
    public void checkPosition() {
        for (Map.Entry<JynxLabel,Line>  me:startLabels.entrySet()) {
            if (!me.getKey().isLessThan(this)) {
                LOG(me.getValue(),M217,me.getKey().name,this.name); // "from label %s is not before to label %s"
            }
        }
        startLabels.clear();
    }
    
    public void define(Line line) {
        if (isDefined()) {
            LOG(M36,line);   // "label already defined in line:%n  %s"
            return;
        }
        defined = line;
        checkPosition();
        startLabels.clear();
    }
    
    public Line definedLine() {
        return defined;
    }
    
    public void addUsed(Line line) {
        usedList.add(line);
    }
    
    public void addCodeUsed(Line line) {
        addUsed(line);
        usedInCode = true;
    }
    
    public void addWeakUsed(Line line) {
        weakList.add(line);
    }
    
    public String name() {
        return name;
    }

    public Label asmlabel() {
        return asmlab;
    }
    
    public Stream<Line> used() {
        return usedList.stream();
    }
    
    public void addCatch(JynxCatch jcatch) {
        catchList.add(jcatch);
    }
    
    public boolean isCatch() {
        return !catchList.isEmpty();
    }

    public void visitCatch() {
        for (JynxCatch jcatch:catchList) {
            JynxLabel fromref = jcatch.fromLab();
            JynxLabel toref = jcatch.toLab();
            if (!this.equals(fromref) ) {
                updateLocal(fromref.getLocals());
            }
            updateLocal(toref.getLocalsBefore());
        }
    }
    
    public void aliasOf(JynxLabel base) {
        jlf = base.jlf.merge(jlf);
        assert base.isDefined() && isDefined();
        base.usedInCode |= usedInCode;
        base.usedList.addAll(usedList);
        base.weakList.addAll(weakList);
        base.catchList.addAll(catchList);
        base.startLabels.putAll(startLabels);
    }
    
    public void updateLocal(OperandStackFrame osfx) {
        jlf.updateLocal(osfx);
    }

    public OperandStackFrame getLocals() {
        return jlf.locals();
    }

    public OperandStackFrame getStack() {
        return jlf.stack();
    }

    public void updateStack(OperandStackFrame osf) {
        jlf.updateStack(osf);
    }
    
    public void setLocalsFrame(OperandStackFrame osfx) {
        jlf.setLocalsFrame(osfx);
    }

    public void setLocalsBefore(OperandStackFrame osfLocalsBefore) {
        jlf.setLocalsBefore(osfLocalsBefore);
    }

    public OperandStackFrame getLocalsBefore() {
        return jlf.localsBefore(); //osfLocalsBefore;
    }
    
    public void load(FrameElement fe, int num) {
        jlf.load(fe, num);
    }
    
    public void store(FrameElement fe, int num) {
        jlf.store(fe, num);
    }
    
    public void freeze() {
        assert isDefined();
        jlf.freeze();
    }

    public void print() {
        System.err.println(jlf.print());
    }

    @Override
    public String toString() {
        return String.format("%s",name);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof JynxLabel) {
            JynxLabel that = (JynxLabel)obj;
            return this.name.equals(that.name);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
    
}

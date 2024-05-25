package jynx2asm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.objectweb.asm.MethodVisitor;

import static jynx.Global.LOG;
import static jynx.Message.M106;
import static jynx.Message.M217;
import static jynx.Message.M284;

import jynx.Directive;
import jynx.LogIllegalArgumentException;

public class JynxLabelMap {
    
    private final Map<String, JynxLabel> labelmap;
    private final Map<JynxCatch,Line> catches;
    
    private final JynxLabel startlab;
    private final JynxLabel endlab;
    
    public JynxLabelMap() {
        this.labelmap = new HashMap<>();
        this.startlab = new JynxLabel(":start");
        this.endlab = new JynxLabel(":end");
        this.catches = new HashMap<>();
    }
    
    public void start(MethodVisitor mv, Line line) {
        mv.visitLabel(startlab.asmlabel());
        startlab.define(line);
    }

    public void end(MethodVisitor mv, Line line) {
        mv.visitLabel(endlab.asmlabel());
        endlab.define(line);
    }

    public JynxLabel startLabel() {
        return startlab;
    }

    public JynxLabel endLabel() {
        return endlab;
    }
    
    
    public JynxLabel useOfJynxLabel(String name, Line line) {
        NameDesc.LABEL.validate(name);
        JynxLabel lab = labelmap.computeIfAbsent(name, k -> new JynxLabel(name));
        lab.addUsed(line);
        return lab;
    }
    
    public String printJynxlabelFrame(String name,Line line) {
        NameDesc.LABEL.validate(name);
        JynxLabel lab = labelmap.get(name);
        if (lab == null) {
            //"label %s is not (yet?) known"
            throw new LogIllegalArgumentException(M284,name);
        } else {
            return lab.printLabelFrame();
        }
    }

    public JynxLabel codeUseOfJynxLabel(String name, Line line) {
        NameDesc.LABEL.validate(name);
        JynxLabel lab = labelmap.computeIfAbsent(name, k -> new JynxLabel(name));
        lab.addCodeUsed(line);
        return lab;
    }

    public JynxLabel weakUseOfJynxLabel(JynxLabel lastlab, Line line) {
        lastlab.addWeakUsed(line);
        return lastlab;
    }

    public JynxLabel defineJynxLabel(String name, Line line) {
        NameDesc.LABEL.validate(name);
        JynxLabel lab = labelmap.computeIfAbsent(name, k -> new JynxLabel(name));
        lab.define(line);
        return lab;
    }

    public JynxLabel defineWeakJynxLabel(String name, Line line) {
        NameDesc.LABEL.validate(name);
        JynxLabel lab = labelmap.get(name);
        if (lab == null || lab.isDefined()) {
            return null;
        }
        return defineJynxLabel(name, line);
    }

    public void aliasJynxLabel(String alias, Line line, JynxLabel base) {
        NameDesc.LABEL.validate(alias);
        base.addWeakUsed(line);
        labelmap.put(alias,base);
    }

    public void checkCatch() {
        for (Map.Entry<JynxCatch,Line>  me:catches.entrySet()) {
            JynxCatch jcatch = me.getKey();
            Line line = me.getValue();
            JynxLabel from = getBase(jcatch.fromLab());
            JynxLabel to = getBase(jcatch.toLab());
            if (!from.isLessThan(to)) {
                // "from label %s is not before to label %s"
                LOG(line.toString(), M217, from, to);
            }
        }
    }
   
    private JynxLabel getBase(JynxLabel label) {
        return labelmap.get(label.base());
    }
    
    public List<JynxCatch> getCatches(JynxLabel label) {
        label = getBase(label);
        List<JynxCatch> result = new ArrayList<>(); 
        for (Map.Entry<JynxCatch,Line>  me:catches.entrySet()) {
            JynxCatch jcatch = me.getKey();
            JynxLabel using = getBase(jcatch.usingLab());
            if (using == label) {
                result.add(jcatch);
            }
        }
        return result;
    }
   
    public Set<JynxLabel> getThrowsTo(JynxLabel label) {
        label = getBase(label);
        Set<JynxLabel> result = new HashSet<>();
        for (Map.Entry<JynxCatch,Line>  me:catches.entrySet()) {
            JynxCatch jcatch = me.getKey();
            JynxLabel from = getBase(jcatch.fromLab());
            JynxLabel to = getBase(jcatch.toLab());
            if (from.isLessThan(label) && !to.isLessThan(label) && (!to.isStartBlock() || to != label)) {
                JynxLabel using = getBase(jcatch.usingLab());
                result.add(using);
            }
        }
        return result;
    }
    
    public Stream<JynxLabel> stream() {
        return labelmap.entrySet().stream()
                .filter(me->me.getKey().equals(me.getValue().name())) // remove aliases
                .map(me->me.getValue());
    }

    public JynxCatch getCatch(String fromname, String  toname,
            String usingname, String exception, Line line) {
        JynxLabel fromref = useOfJynxLabel(fromname, line);
        JynxLabel toref = useOfJynxLabel(toname, line);
        JynxLabel usingref = codeUseOfJynxLabel(usingname, line);
        if (!fromref.isDefined() && !toref.isDefined() && !usingref.isDefined()) {
            JynxCatch jcatch = new JynxCatch(fromref, toref, usingref,exception,line);
            catches.put(jcatch,line);
            return jcatch;
        } else {
            LOG(M106,Directive.dir_catch); // "labels in %s must not be defined yet"
            return null;
        }
    }

}

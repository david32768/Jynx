package jynx2asm;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.objectweb.asm.MethodVisitor;

import static jynx.Global.LOG;
import static jynx.Message.M217;
import static jynx.Message.M284;

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

    public void addCatch(JynxCatch jcatch, Line line) {
        catches.put(jcatch,line);
    }
    
    public void checkCatch() {
        for (Map.Entry<JynxCatch,Line>  me:catches.entrySet()) {
            JynxCatch jcatch = me.getKey();
            Line line = me.getValue();
            JynxLabel from = jcatch.fromLab();
            from = labelmap.get(from.base());
            JynxLabel to = jcatch.toLab();
            to = labelmap.get(to.base());
            if (!from.isLessThan(to)) {
                LOG(line,M217,from,to); // "from label %s is not before to label %s"
            }
        }
    }
   
     public Stream<JynxLabel> stream() {
        return labelmap.entrySet().stream()
                .filter(me->me.getKey().equals(me.getValue().name())) // remove aliases
                .map(me->me.getValue());
    }

}

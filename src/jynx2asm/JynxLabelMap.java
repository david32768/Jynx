package jynx2asm;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.objectweb.asm.MethodVisitor;


public class JynxLabelMap {
    
    private final Map<String, JynxLabel> labelmap;
    private final Map<String,String> aliases;
    
    private final JynxLabel startlab;
    private final JynxLabel endlab;
    
    public JynxLabelMap() {
        this.labelmap = new HashMap<>();
        this.startlab = new JynxLabel(":start");
        this.endlab = new JynxLabel(":end");
        this.aliases = new HashMap<>();
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
    
    private String useName(String name, Line line) {
        NameDesc.LABEL.validate(name);
        String alias = aliases.get(name);
        return alias == null?name:alias;
    }
    
    public JynxLabel useOfJynxLabel(String name, Line line) {
        String usename = useName(name,line);
        JynxLabel lab = labelmap.computeIfAbsent(usename, k -> new JynxLabel(usename));
        lab.addUsed(line);
        return lab;
    }
    
    public void printJynxlabel(String name,Line line) {
        String usename = useName(name,line);
        JynxLabel lab = labelmap.get(usename);
        if (lab == null) {
            System.err.format("label %s is not found%n", name);
        } else {
            lab.print();
        }
    }

    public JynxLabel codeUseOfJynxLabel(String name, Line line) {
        String usename = useName(name,line);
        JynxLabel lab = labelmap.computeIfAbsent(usename, k -> new JynxLabel(usename));
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
        if (lab == null || lab.isDefined() || aliases.get(name) != null) {
            return null;
        }
        return defineJynxLabel(name, line);
    }

    public void aliasJynxLabel(String alias, Line line, JynxLabel base) {
        NameDesc.LABEL.validate(alias);
        String name = base.name();
        String previous = aliases.put(alias, name);
        assert previous == null:String.format("alias = %s base = %s previous = %s",alias,name,previous);
        base.addWeakUsed(line);
        labelmap.remove(alias);
    }

    public Stream<JynxLabel> stream() {
        return labelmap.values().stream();
    }

}

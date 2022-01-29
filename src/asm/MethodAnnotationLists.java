package asm;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ParameterNode;
import org.objectweb.asm.TypePath;

import static jynx.Global.LOG;
import static jynx.Message.M187;
import static jynx.Message.M194;
import static jynx.Message.M234;

public class MethodAnnotationLists {
    
    private final int numparms;
    private int visibleMaxParm;
    private int invisibleMaxParm;
    private final ParameterNode[] parameters;
    private int parmct;
    private AnnotationNode defaultAnnotation;
    private int visibleDefault;
    private int invisibleDefault;
    private final List<AcceptsVisitor> annotations;
    
    
    public MethodAnnotationLists(int numparms) {
        this.numparms = numparms;
        this.parameters = new ParameterNode[numparms];
        this.visibleMaxParm = -1;
        this.invisibleMaxParm = -1;
        this.visibleDefault = numparms;
        this.invisibleDefault = numparms;
        this.annotations = new ArrayList<>();
    }

    public boolean hasAnnotations() {
        return !annotations.isEmpty();
    }
    
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        JynxAnnotationNode jan = JynxAnnotationNode.getInstance(desc,visible);
        annotations.add(jan);
        return jan;
    }

    public AnnotationVisitor visitTypeAnnotation(int typeref, TypePath tp, String desc, boolean visible) {
        JynxTypeAnnotationNode tan = JynxTypeAnnotationNode.getInstance(typeref, tp, desc,visible);
        annotations.add(tan);
        return tan;
    }
    
    public void add(AcceptsVisitor jan) {
        annotations.add(jan);
    }

    public void visitParameter(String pname, int access) {
        ParameterNode pn = new ParameterNode(pname, access);
        parameters[parmct] = pn;
        ++parmct;
    }
    
    private boolean hasVisibleMax() {
        return visibleMaxParm >= 0;
    }

    private boolean hasInvisibleMax() {
        return invisibleMaxParm >= 0;
    }

    public AnnotationVisitor visitAnnotationDefault() {
        defaultAnnotation = new AnnotationNode("throw_away_name_for_default"); // checkAdapter requires non_null
        return defaultAnnotation;
    }
    
    public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
        int mnodect = numparms;
        if (visible && hasVisibleMax()) {
            mnodect = visibleMaxParm;
        } else if (!visible && hasInvisibleMax()) {
            mnodect = invisibleMaxParm;
        } 
        if (parameter < 0 || parameter >= mnodect) {
            LOG(M234,parameter,mnodect); // "invalid parameter number %d; bounds are [0 - %d)"
            parameter = parameter < 0?0:mnodect;
        }
        JynxParameterAnnotationNode jpan = JynxParameterAnnotationNode.getInstance(parameter, desc, visible);
        annotations.add(jpan);
        if (visible) {
            visibleDefault = Integer.max(parameter + 1, visibleDefault);
        } else {
            invisibleDefault = Integer.max(parameter + 1, invisibleDefault);
        }
        return jpan;
    }

    public void visitAnnotableCount(int count, boolean visible) {
        if (count < 1 || count > numparms) {
            LOG(M187,count,numparms);    // "annotation parameter count(%d) not in range[1,%d]"
            count = numparms;
        }
        boolean already = visible?hasVisibleMax():hasInvisibleMax();
        if (already) {
            LOG(M194);   // "annotation parameter count already been set"
        } else {
            if (visible) {
                visibleMaxParm = count;
            } else {
                invisibleMaxParm = count;
            }
        }
    }

    public void accept(MethodVisitor mv) {
        for (int i = 0; i < parmct; ++i) {
            ParameterNode pn = parameters[i];
            mv.visitParameter(pn.name, pn.access);
        }
        if (defaultAnnotation != null) {
            defaultAnnotation.accept(mv.visitAnnotationDefault());
        }
        annotations.stream()
                .forEach(jan -> jan.accept(mv));
        visibleMaxParm = hasVisibleMax()?visibleMaxParm:visibleDefault;
        mv.visitAnnotableParameterCount(visibleMaxParm, true);
        invisibleMaxParm = hasInvisibleMax()?invisibleMaxParm:invisibleDefault;
        mv.visitAnnotableParameterCount(invisibleMaxParm, false);
    }
    
}

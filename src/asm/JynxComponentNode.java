package asm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.RecordComponentVisitor;
import org.objectweb.asm.TypePath;

import static jynx.ClassType.RECORD;
import static jynx.Global.*;
import static jynx.Message.*;
import static jynx2asm.NameDesc.*;

import jvm.Constants;
import jvm.Context;
import jynx.ClassType;
import jynx.Directive;
import jynx2asm.ClassChecker;
import jynx2asm.handles.LocalFieldHandle;
import jynx2asm.handles.LocalMethodHandle;
import jynx2asm.JynxScanner;
import jynx2asm.Line;

public class JynxComponentNode implements ContextDependent {

    private final Line line;
    private final List<AcceptsVisitor> annotations;
    private final LocalFieldHandle compfh;
    private final LocalMethodHandle compmh;
    private String signature;
    private boolean endVisited;
   
    private final Map<Directive,Line> unique_directives;

    private JynxComponentNode(Line line, LocalFieldHandle compfh, LocalMethodHandle compmh) {
        this.annotations = new ArrayList<>();
        this.compfh = compfh;
        this.compmh = compmh;
        this.line = line;
        this.endVisited = false;
        this.unique_directives = new HashMap<>();
    }

    public static JynxComponentNode getInstance(Line line, ClassChecker checker) {
        ClassType classtype = checker.getClassType();
        if (classtype != RECORD) {
            LOG(M41);    // "component can only appear in a record"
        }
        String name = line.nextToken().asName();
        String descriptor = line.nextToken().asString();
        LocalFieldHandle compfh = LocalFieldHandle.getInstance(name, descriptor);
        LocalMethodHandle compmh = LocalMethodHandle.getInstance(compfh.ond());
        if (Constants.isNameIn(compmh.name(),Constants.INVALID_COMPONENTS)) {
            LOG(M47,compfh.name());   // "Invalid component name - %s"
        }
        JynxComponentNode jcn = new JynxComponentNode(line, compfh, compmh);
        checker.checkComponent(jcn);
        return jcn;
    }

    public boolean hasAnnotations() {
        return !annotations.isEmpty();
    }

    @Override
    public void visitDirective(Directive dir, JynxScanner js) {
        Line linex = js.getLine();
        dir.checkUnique(unique_directives, linex);
        switch (dir) {
            default:
                visitCommonDirective(dir, linex, js);
                break;
        }
    }
    
    private String getSignature(Context context) {
        assert endVisited;
        if (signature != null && context == Context.METHOD) {
            return "()" + signature;
        }
        return signature;
    }
    
    public String getName() {
        return compfh.name();
    }

    public String getDesc() {
        return compfh.desc();
    }
    
    
    public LocalMethodHandle getLocalMethodHandle() {
        return compmh;
    }
    
    public void checkSignature(String fsignature, Context context) {
        String signaturex = getSignature(context);
        if (!Objects.equals(fsignature, signaturex)) {
            LOG(M78,context,fsignature, signaturex);  // "%s has different signature %s to component %s"
        }
    }
    
    public Line getLine() {
        return line;
    }

    @Override
    public Context getContext() {
        return Context.COMPONENT;
    }

    @Override
    public void setSignature(Line line) {
        String signaturex = line.nextToken().asQuoted();
        FIELD_SIGNATURE.validate(signaturex);
        signature = signaturex;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        JynxAnnotationNode jan = JynxAnnotationNode.getInstance(desc,visible);
        annotations.add(jan);
        return jan;
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeref, TypePath tp, String desc, boolean visible) {
        JynxTypeAnnotationNode tan = JynxTypeAnnotationNode.getInstance(typeref, tp, desc,visible);
        annotations.add(tan);
        return tan;
    }

    public void visitEnd(JynxClassHdr jclasshdr, Directive dir) {
        RecordComponentVisitor rcv;
        rcv = jclasshdr.visitRecordComponent(compfh.name(), compfh.desc(), signature);
        if (dir ==  null && !annotations.isEmpty()) {
            LOG(M270, Directive.end_component); // "%s directive missing but assumed"
        }
        annotations.stream()
                .forEach(jan -> jan.accept(rcv));
        rcv.visitEnd();
        endVisited = true;
    }
    
    @Override
    public String toString() {
        return String.format("[%s %s %s]",compfh.name(), compfh.desc(),signature);
    }
    
}

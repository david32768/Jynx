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
import jynx2asm.FieldDesc;
import jynx2asm.JynxScanner;
import jynx2asm.Line;
import jynx2asm.MethodDesc;

public class JynxComponentNode implements ContextDependent {

    private final Line line;
    private final List<AcceptsVisitor> annotations;
    private final FieldDesc compfd;
    private final MethodDesc compmd;
    private String signature;
    private boolean endVisited;
   
    private final Map<Directive,Line> unique_directives;

    private JynxComponentNode(Line line, FieldDesc compfd, MethodDesc compmd) {
        this.annotations = new ArrayList<>();
        this.compfd = compfd;
        this.compmd = compmd;
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
        FieldDesc compfd = FieldDesc.getLocalInstance(name, descriptor);
        MethodDesc compmd = MethodDesc.getLocalInstance(compfd.getName() + "()" + compfd.getDesc());
        if (Constants.isNameIn(compmd.getName(),Constants.INVALID_COMPONENTS)) {
            LOG(M47,compfd.getName());   // "Invalid component name - %s"
        }
        JynxComponentNode jcn = new JynxComponentNode(line, compfd, compmd);
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
        return compfd.getName();
    }

    public String getDesc() {
        return compfd.getDesc();
    }
    
    public FieldDesc getFieldDesc() {
        return compfd;
    }

    public MethodDesc getMethodDesc() {
        return compmd;
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
        rcv = jclasshdr.visitRecordComponent(compfd.getName(), compfd.getDesc(), signature);
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
        return String.format("[%s %s %s]",compfd.getName(), compfd.getDesc(),signature);
    }
    
}

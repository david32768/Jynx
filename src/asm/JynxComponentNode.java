package asm;

import java.util.ArrayList;
import java.util.Arrays;
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
import static jynx.ReservedWord.*;
import static jynx2asm.NameDesc.*;

import jvm.Context;
import jynx.ClassType;
import jynx.Directive;
import jynx2asm.ClassChecker;
import jynx2asm.JynxScanner;
import jynx2asm.Line;

public class JynxComponentNode implements ContextDependent {

    private final JynxClassHdr jclasshdr;
    private final Line line;
    private final List<AcceptsVisitor> annotations;
    private final String name;
    private final String desc;
    private String signature;
    private boolean endVisited;
   
    private final Map<Directive,Line> unique_directives;
    private final Map<String, Line> unique_attributes;

    private JynxComponentNode(JynxClassHdr jclasshdr, Line line,
            String name, String desc, String signature) {
        this.jclasshdr = jclasshdr;
        this.annotations = new ArrayList<>();
        this.name = name;
        this.desc = desc;
        this.line = line;
        this.signature = signature;
        this.endVisited = false;
        this.unique_directives = new HashMap<>();
        this.unique_attributes = new HashMap<>();
        if (this.signature != null) {
            unique_directives.put(Directive.dir_signature, this.line);
        }
    }
    
    private static final List<String> INVALID_NAMES = Arrays.asList(
            new String[]{"clone","finalize","getClass","hashCode","notify","notifAll","toString","wait"});
    
    public static JynxComponentNode getInstance(JynxClassHdr jclasshdr, Line line, ClassChecker checker) {
        ClassType classtype = checker.getClassType();
        if (classtype != RECORD) {
            LOG(M41);    // "component can only appear in a record"
        }
        String name = line.nextToken().asName();
        String descriptor = line.nextToken().asString();
        FIELD_NAME.validate(name);
        if (INVALID_NAMES.contains(name)) {
            LOG(M47,name);   // "Invalid component name - %s"
        }
        FIELD_DESC.validate(descriptor);
        String signature = line.optAfter(res_signature);
        if (signature != null) {
            FIELD_SIGNATURE.validate(signature);
        }
        JynxComponentNode jcn = new JynxComponentNode(jclasshdr,line,name, descriptor, signature);
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
                visitCommonDirective(dir, linex, js,unique_attributes);
                break;
        }
    }
    
    private String getSignature(Context context) {
        assert endVisited;
        if (signature == null) {
            return null;
        }
        String signaturex = signature;
        if (context == Context.METHOD) {
            signaturex = "()" + signaturex;
        }
        return signaturex;
    }
    
    public String getName() {
        return name;
    }
    
    public String getMethodName() {
        return name + "()" + desc;
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
    public AnnotationVisitor visitTypeAnnotation(int typeref, TypePath tp, String desc, boolean visible) {
        JynxTypeAnnotationNode tan = JynxTypeAnnotationNode.getInstance(typeref, tp, desc,visible);
        annotations.add(tan);
        return tan;
    }

    public void visitEnd() {
        RecordComponentVisitor rcv = jclasshdr.visitRecordComponent(name, desc, signature);
        annotations.stream()
                .forEach(jan -> jan.accept(rcv));
        rcv.visitEnd();
        endVisited = true;
    }
    
    @Override
    public String toString() {
        return String.format("[%s %s %s]",name,desc,signature);
    }
    
}

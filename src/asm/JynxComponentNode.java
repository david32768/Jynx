package asm;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.tree.RecordComponentNode;
import org.objectweb.asm.TypePath;

import static jynx.Global.*;
import static jynx.Message.*;
import static jynx2asm.NameDesc.*;

import jvm.Constants;
import jvm.Context;
import jynx.Directive;
import jynx2asm.handles.LocalFieldHandle;
import jynx2asm.handles.LocalMethodHandle;
import jynx2asm.JynxScanner;
import jynx2asm.Line;

public class JynxComponentNode implements ContextDependent {

    private final RecordComponentNode compnode;
    private final Line line;
    private final LocalFieldHandle compfh;
    private final LocalMethodHandle compmh;
    private boolean endVisited;
   
    private final Map<Directive,Line> unique_directives;

    private JynxComponentNode(Line line, RecordComponentNode compnode, LocalFieldHandle compfh, LocalMethodHandle compmh) {
        this.compnode = compnode;
        this.compfh = compfh;
        this.compmh = compmh;
        this.line = line;
        this.endVisited = false;
        this.unique_directives = new HashMap<>();
    }

    public static JynxComponentNode getInstance(Line line) {
        String name = line.nextToken().asName();
        String descriptor = line.nextToken().asString();
        LocalFieldHandle compfh = LocalFieldHandle.getInstance(name, descriptor);
        LocalMethodHandle compmh = LocalMethodHandle.getInstance(compfh.ond());
        if (Constants.isNameIn(compmh.name(),Constants.INVALID_COMPONENTS)) {
            LOG(M47,compfh.name());   // "Invalid component name - %s"
        }
        RecordComponentNode compnode = new RecordComponentNode(name, descriptor, null);
        return new JynxComponentNode(line, compnode, compfh, compmh);
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
        String signature = compnode.signature;
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
        assert compnode.signature == null; // dir_signature is unique within
        String signature = line.nextToken().asQuoted();
        FIELD_SIGNATURE.validate(signature);
        compnode.signature = signature;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        return compnode.visitAnnotation(desc, visible);
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeref, TypePath tp, String desc, boolean visible) {
        return compnode.visitTypeAnnotation(typeref, tp, desc, visible);
    }

    private boolean hasAnnotations() {
        return compnode.invisibleAnnotations != null || compnode.invisibleTypeAnnotations != null
                || compnode.visibleAnnotations != null || compnode.visibleTypeAnnotations != null;
    }
    
    public RecordComponentNode visitEnd(Directive dir) {
        if (dir ==  null && hasAnnotations()) {
            LOG(M270, Directive.end_component); // "%s directive missing but assumed"
        }
        endVisited = true;
        return compnode;
    }
    
    @Override
    public String toString() {
        return String.format("[%s %s %s]",compfh.name(), compfh.desc(),compnode.signature);
    }
    
}

package asm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;

import static jvm.AccessFlag.acc_abstract;
import static jvm.AccessFlag.acc_native;
import static jvm.AccessFlag.acc_private;
import static jvm.AccessFlag.acc_static;
import static jvm.AttributeName.*;
import static jvm.Context.METHOD;
import static jynx.Global.*;
import static jynx.Message.*;

import jvm.Context;
import jynx.Access;
import jynx.Directive;
import jynx2asm.ClassChecker;
import jynx2asm.JynxLabelMap;
import jynx2asm.JynxScanner;
import jynx2asm.Line;
import jynx2asm.MethodDesc;
import jynx2asm.NameDesc;

public class JynxMethodNode implements ContextDependent {

    private MethodNode mnode;
    private final Line methodLine;
    private final int numparms;
    private final Access accessName;
    private final MethodDesc md;
    private String signature;
    private final List<String> exceptions;
    private final JynxLabelMap labelmap;

    private final MethodAnnotationLists annotationLists;
    private final Map<Directive,Line> unique_directives;
    private final Map<String, Line> unique_attributes;

    private final ClassChecker checker;
    
    private JynxMethodNode(Line line, Access accessname, MethodDesc cmd,  ClassChecker checker) {
        this.md = cmd;
        this.accessName = accessname;
        this.numparms = Type.getArgumentTypes(cmd.getDesc()).length;
        this.methodLine = line;
        this.exceptions = new ArrayList<>();
        this.annotationLists = new MethodAnnotationLists(numparms);
        this.signature = null;
        this.labelmap = new JynxLabelMap();
        this.unique_directives = new HashMap<>();
        this.unique_attributes = new HashMap<>();
        this.checker = checker;
    }

    public static JynxMethodNode getInstance(Line line, ClassChecker checker) {
        Access accessname = checker.getAccess(METHOD,line);
        line.noMoreTokens();
        MethodDesc md = MethodDesc.getInstance(accessname.getName());
        accessname.getCheck4Method(md.isInit());
        JynxMethodNode jmn =  new JynxMethodNode(line,accessname,md,checker);
        checker.checkMethod(jmn);
        return jmn;
    }

    public boolean hasAnnotations() {
        return annotationLists.hasAnnotations();
    }
    
    public String getName() {
        return md.getName();
    }
    
    public String getDesc() {
        return md.getDesc();
    }
    
    public Line getLine() {
        return methodLine;
    }

    @Override
    public void visitDirective(Directive dir, JynxScanner js) {
        Line line = js.getLine();
        dir.checkUnique(unique_directives, line);
        switch(dir) {
            case dir_throws:
                setThrow(line);
                break;
            case dir_parameter:
                visitParameter(line);
                break;
            case dir_visible_parameter_count:
                 visitAnnotableCount(line,true);
                 break;
            case dir_invisible_parameter_count:
                visitAnnotableCount(line,false);
                break;
            case dir_default_annotation:
            case dir_invisible_parameter_annotation:
            case dir_visible_parameter_annotation:
                JynxAnnotation.setAnnotation(dir, this, js);
                break;
            default:
                visitCommonDirective(dir, line, js,unique_attributes);
                break;
        }
    }

    private void endHeader() {
      int access = accessName.getAccess();
      mnode = new MethodNode(access, getName(), getDesc(), signature, exceptions.toArray(new String[0]));
      annotationLists.accept(mnode);
    }
    
    public JynxCodeHdr getJynxCodeHdr(JynxScanner js) {
        endHeader();
        if (isAbstractOrNative()) {
            LOG(M155); // "code is not allowed as method is abstract or native"
            return null;
        }
        return JynxCodeHdr.getInstance(mnode, js, md, labelmap,accessName,checker);
    }

    public MethodDesc getMethodDesc() {
        return md;
    }
    
    public boolean isAbstract() {
        return accessName.is(acc_abstract);
    }

    public boolean isNative() {
        return accessName.is(acc_native);
    }

    public boolean isStatic() {
        return accessName.is(acc_static);
    }
    
    public boolean isAbstractOrNative() {
        return isAbstract() || isNative();
    }

    public boolean isPrivate() {
        return accessName.is(acc_private);
    }
    
    @Override
    public Context getContext() {
        return Context.METHOD;
    }

    private void setThrow(Line line) {
        if (accessName.isComponent()) {
            LOG(M128,Directive.dir_throws,getName());   // "% directive not allowed for component method %s"
            return;
        }
        String exception = line.lastToken().asString();
        exceptions.add(exception);
    }
    
    @Override
    public void setSignature(Line line) {
        CHECK_SUPPORTS(Signature);
        String signaturex = line.lastToken().asQuoted();
        NameDesc.METHOD_SIGNATURE.validate(signaturex);
        signature = signaturex;
    }

    private void visitParameter(Line line) {
        Access accessname = checker.getAccess(Context.PARAMETER, line);
        line.noMoreTokens();
        String pname = accessname.getName();
        accessname.getCheck4Parameter();
        int pflags = accessname.getAccess();
        annotationLists.visitParameter(pname, pflags);
    }
    
    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        return annotationLists.visitAnnotation(descriptor,visible);
    }

    @Override
    public AnnotationVisitor visitAnnotationDefault() {
        return annotationLists.visitAnnotationDefault();
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeref, TypePath tp, String descriptor, boolean visible) {
        return annotationLists.visitTypeAnnotation(typeref, tp, descriptor, visible);
    }

    private void visitAnnotableCount(Line line,boolean visible) {
        int count = line.nextToken().asInt();
        line.noMoreTokens();
        CHECK_SUPPORTS(visible?RuntimeVisibleParameterAnnotations:RuntimeInvisibleParameterAnnotations);
        annotationLists.visitAnnotableCount(count, visible);
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(String classdesc,int parameter, boolean visible) {
        return annotationLists.visitParameterAnnotation(parameter, classdesc, visible);
    }

    public MethodNode visitEnd() {
        if (mnode == null) {
            endHeader();
        }
        JynxComponentNode jcn = checker.getComponent4Method(getName(),getDesc());
        if (jcn != null) {
            jcn.checkSignature(signature, METHOD);
        }
        try {
            mnode.visitEnd();
        } catch (Exception ex) {
            LOG(ex);
        }
        return mnode;
    }

}

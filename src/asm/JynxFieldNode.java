package asm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.TypePath;

import static jvm.AccessFlag.acc_final;
import static jvm.AccessFlag.acc_private;
import static jvm.AccessFlag.acc_static;
import static jvm.AttributeName.*;
import static jvm.Context.FIELD;
import static jynx.Global.*;
import static jynx.Message.*;
import static jynx.ReservedWord.*;
import static jynx2asm.NameDesc.*;

import jvm.ConstType;
import jvm.Context;
import jynx.Access;
import jynx.Directive;
import jynx2asm.ClassChecker;
import jynx2asm.JynxScanner;
import jynx2asm.Line;
import jynx2asm.Token;

public class JynxFieldNode implements ContextDependent {

    private final JynxClassHdr jclasshdr;
    private final String name;
    private final String desc;
    private String signature;
    private final Object value;
    private final List<AcceptsVisitor> annotations;
    private final Line line;
    private final Access accessName;
    private final ClassChecker checker;
    
    private final Map<Directive,Line> unique_directives;
    private final Map<String, Line> unique_attributes;

    private JynxFieldNode(JynxClassHdr jclasshdr, Line line, Access accessname,
            String name, String desc, String signature, Object value,ClassChecker checker) {
        this.jclasshdr = jclasshdr;
        this.checker = checker;
        this.accessName = accessname;
        this.line = line;
        this.name = name;
        this.desc = desc;
        this.signature = signature;
        this.value = value;
        this.annotations = new ArrayList<>();
        this.unique_directives = new HashMap<>();
        this.unique_attributes = new HashMap<>();
        if (this.signature != null) {
            unique_directives.put(Directive.dir_signature, this.line);
        }
    }

    public static JynxFieldNode getInstance(JynxClassHdr jclasshdr, Line line, ClassChecker checker) {
        Access accessname = checker.getAccess(FIELD,line);
        String name = accessname.getName();
        String desc = line.nextToken().asString();
        FIELD_DESC.validate(desc);
        String signature = line.optAfter(res_signature);
        Token token = line.nextToken();
        Object value = null;
        if (token != Token.END_TOKEN) {
            token.mustBe(equals_sign);
            token = line.lastToken();
            CHECK_SUPPORTS(ConstantValue);
            ConstType ctf = ConstType.getFromDesc(desc,FIELD);
            value = token.getValue(ctf);    // check range
            value = ctf.toJvmValue(value);
            if (accessname.is(acc_static)) {
                if (!accessname.is(acc_final)) {
                    // 5.5 bullet 6 overriding 4.7.2
                    // however openjdk seems to initialise non_final static fields
                    // although javac uses <clinit> method
                    LOG(M71,name,token); // "as %s is not a final static field, ' = %s' may be silently ignored by JVM (JVMS 5.5 6)"
                }
            } else {
                LOG(M53,name,token); // "as %s is not a static field, ' = %s' is silently ignored by JVM "
            }
            FIELD_NAME.validate(accessname);
        } else {
            FIELD_NAME.validate(accessname.getName());
        }
        if (signature != null) {
            CHECK_SUPPORTS(Signature);
            FIELD_SIGNATURE.validate(signature);
        }
        accessname.check4Field();
        JynxFieldNode jfn = new JynxFieldNode(jclasshdr, line, accessname, name, desc, signature, value, checker);
        checker.checkField(jfn);
        return jfn;
    }
    
    public Line getLine() {
        return line;
    }

    public String getName() {
        return name;
    }
    
    public String getDesc() {
        return desc;
    }
    
    public boolean isInstanceField() {
        return !accessName.is(acc_static);
    }

    public boolean isPrivate() {
        return accessName.is(acc_private);
    }

    @Override
    public Context getContext() {
        return Context.FIELD;
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
    
    @Override
    public void setSignature(Line line) {
        String signaturex = line.nextToken().asQuoted();
        FIELD_SIGNATURE.validate(signaturex);
        this.signature = signaturex;
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

    public void visitEnd() {
        JynxComponentNode jcn = checker.getComponent4Field(name);
        if (jcn != null) {
            jcn.checkSignature(signature, FIELD);
        }
        int access = accessName.getAccess();
        FieldVisitor fv = jclasshdr.visitField(access, name, desc, signature, value);
        annotations.stream()
                .forEach(jan -> jan.accept(fv));
    }
    
    @Override
    public String toString() {
        return String.format("[%s %s %s]",name,desc,signature);
    }
    
}

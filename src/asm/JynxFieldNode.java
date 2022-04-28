package asm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.TypePath;

import static jvm.AccessFlag.acc_final;
import static jvm.AccessFlag.acc_static;
import static jvm.AttributeName.*;
import static jvm.Context.FIELD;
import static jynx.Global.*;
import static jynx.Message.*;
import static jynx.ReservedWord.*;
import static jynx2asm.NameDesc.*;

import jvm.AccessFlag;
import jvm.ConstType;
import jvm.Context;
import jynx.Access;
import jynx.Directive;
import jynx2asm.ClassChecker;
import jynx2asm.JynxScanner;
import jynx2asm.Line;
import jynx2asm.Token;

public class JynxFieldNode implements ContextDependent, HasAccessFlags {

    private final JynxClassHdr jclasshdr;
    private final String name;
    private final String desc;
    private String signature;
    private final Optional<Object> value;
    private final List<AcceptsVisitor> annotations;
    private final Line line;
    private final Access accessName;
    private final ClassChecker checker;
    
    private final Map<Directive,Line> unique_directives;

    private JynxFieldNode(JynxClassHdr jclasshdr, Line line, Access accessname,
            String name, String desc, Optional<Object> value,ClassChecker checker) {
        this.jclasshdr = jclasshdr;
        this.checker = checker;
        this.accessName = accessname;
        this.line = line;
        this.name = name;
        this.desc = desc;
        this.value = value;
        this.annotations = new ArrayList<>();
        this.unique_directives = new HashMap<>();
    }

    public static JynxFieldNode getInstance(JynxClassHdr jclasshdr, Line line, ClassChecker checker) {
        Access accessname = checker.getAccess(FIELD,line);
        String name = accessname.getName();
        String desc = line.nextToken().asString();
        FIELD_DESC.validate(desc);
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
        accessname.check4Field();
        Optional<Object> optvalue = value==null?Optional.empty():Optional.of(value);
        JynxFieldNode jfn = new JynxFieldNode(jclasshdr, line, accessname, name, desc, optvalue, checker);
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
    
    @Override
    public boolean is(AccessFlag flag) {
        assert flag.isValid(FIELD);
        return accessName.is(flag);
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
                visitCommonDirective(dir, linex, js);
                break;
        }
    }
    
    @Override
    public void setSignature(Line line) {
        signature = line.nextToken().asQuoted();
        FIELD_SIGNATURE.validate(signature);
        if (isComponent()) {
            checker.checkSignature4Field(signature, name);
        }
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
        if (signature == null && isComponent()) {
            checker.checkSignature4Field(signature, name);
        }
        int access = accessName.getAccess();
        FieldVisitor fv = jclasshdr.visitField(access, name, desc, signature, value.orElse(null));
        annotations.stream()
                .forEach(jan -> jan.accept(fv));
    }
    
    @Override
    public String toString() {
        return String.format("[%s %s %s]",name,desc,signature);
    }
    
}

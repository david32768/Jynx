package asm;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.TypePath;

import static jvm.AccessFlag.acc_final;
import static jvm.AccessFlag.acc_super;
import static jynx.Global.*;
import static jynx.GlobalOption.VALHALLA;
import static jynx.Message.*;
import static jynx.ReservedWord.*;
import static jynx2asm.NameDesc.*;

import jvm.AccessFlag;
import jvm.Constants;
import jvm.Context;
import jvm.JvmVersion;
import jynx.Access;
import jynx.ClassType;
import jynx.Directive;
import jynx.LogIllegalStateException;
import jynx2asm.handles.EnclosingMethodHandle;
import jynx2asm.handles.HandlePart;
import jynx2asm.JynxScanner;
import jynx2asm.Line;
import jynx2asm.ObjectLine;
import jynx2asm.TokenArray;
import jynx2asm.TypeHints;

public class JynxClassHdr implements ContextDependent, HasAccessFlags {

    private ASMClassHeaderNode hdrnode;

    private final JvmVersion jvmVersion;
    private final ClassType classType;
    private final Access accessName;
    private final String cname;

    private String source;
    private final String defaultSource;
    private StringBuilder debugBuilder;
 
    private String csuper;
    private Line outerLine;

    private final Map<String,ObjectLine<InnerClassNode>> innerClasses;
    
    private final Map<Directive,Line> unique_directives;
    private final TypeHints hints;
    
    private boolean inner;

    private JynxClassHdr(ObjectLine<String> sourcex, String defaultsource, 
            Access accessname, TypeHints hints) {
        this.jvmVersion = accessname.jvmVersion();
        this.cname = accessname.name();
        this.classType = accessname.classType();
        this.hdrnode = new ASMClassHeaderNode(jvmVersion.toASM(), cname, accessname.getAccess());
        this.hints = hints;
        this.unique_directives = new HashMap<>();
        if (sourcex == null) {
            this.source = null;
        } else {
            this.source = sourcex.object();
            Directive.dir_source.checkUnique(unique_directives, sourcex.line());
        }
        this.defaultSource = defaultsource;
        this.accessName = accessname;
        this.innerClasses = new LinkedHashMap<>();
        this.csuper = null;
        this.outerLine = null;
        this.inner = false;
        LOGGER().pushContext();
    }
    
    public static JynxClassHdr getInstance(Access accessname, ObjectLine<String> source,
            String defaultsource, TypeHints hints) {
        return new JynxClassHdr(source, defaultsource, accessname, hints);
    }

    @Override
    public void visitDirective(Directive dir, JynxScanner js) {
        Line line = js.getLine();
        dir.checkUnique(unique_directives, line);
        if (Constants.isObjectClass(cname)) {
            switch(dir) {
                case dir_debug:
                case dir_source:
                    break;
                default:
                    line.skipTokens();
                    // "Directive %s is not valid for %s"
                    LOG(M100,dir,cname);
                    return;
            }
        }
        switch(dir) {
            case dir_debug:
                setDebug(line);
                break;
            case dir_super:
                setSuper(line);
                break;
            case dir_implements:
                setImplements(line);
                break;
            case dir_inner_class:
            case dir_inner_enum:
            case dir_inner_interface:
            case dir_inner_define_annotation:
            case dir_inner_record:
                setInnerClass(dir,line);
                break;
            case dir_enclosing_method:
            case dir_outer_class:
                setOuterClass(dir,line);
                break;
            case dir_nesthost:
                setHostClass(line);
                break;
            case dir_nestmember:
                setMemberClass(line);
                break;
            case dir_permittedSubclass:
                setPermittedSubclass(line);
                break;
            case dir_hints:
                setHints(js,line);
                break;
            default:
                visitCommonDirective(dir, line, js);
                break;
        }
    }
    
    @Override
    public boolean is(AccessFlag flag) {
        assert flag.isValid(Context.CLASS);
        return accessName.is(flag);
    }    

    @Override
    public Context getContext() {
        return Context.CLASS;
    }

    private void setDebug(Line line) {
        if (this.debugBuilder == null) {
            this.debugBuilder = new StringBuilder();
        }
        TokenArray.debugString(debugBuilder, line);
    }

    public String getClassName() {
        return cname;
    }
    
    @Override
    public void setSignature(Line line) {
        String signature = line.lastToken().asQuoted();
        CLASS_SIGNATURE.validate(signature);
        hdrnode.signature = signature;
    }

    @Override
    public void setSource(Line line) {
        source = line.lastToken().asQuoted();
    }

    private void setSuper(Line line) {
        String csuperx = line.lastToken().asString();
        csuper = classType.checkSuper(csuperx);
    }

    private void setImplements(Line line) {
        hdrnode.interfaces = TokenArray.listString(Directive.dir_implements, line, CLASS_NAME);
    }

    private void setInnerClass(Directive dir,Line line) {
        EnumSet<AccessFlag> accflags = line.getAccFlags();
        ClassType classtype;
        switch (dir) {
            case dir_inner_class:
                classtype = ClassType.BASIC;
                if (OPTION(VALHALLA) && !accflags.contains(acc_super)) {
                    classtype = ClassType.VALUE_CLASS;
                }
                break;
            case dir_inner_enum:
                classtype = ClassType.ENUM;
                break;
            case dir_inner_record:
                classtype = ClassType.RECORD;
                break;
            case dir_inner_interface:
                classtype = ClassType.INTERFACE;
                break;
            case dir_inner_define_annotation:
                classtype = ClassType.ANNOTATION_CLASS;
                break;
            default:
                throw new EnumConstantNotPresentException(dir.getClass(), dir.name());
        }
        String innerclass = line.nextToken().asName();
        Optional<String> outerclass = line.optAfter(res_outer);
        Optional<String> innername = line.optAfter(res_innername);
        line.noMoreTokens();
        accflags.addAll(classtype.getMustHave4Inner(jvmVersion));
        Access accessname = Access.getInstance(accflags, jvmVersion, innerclass,classtype);
        accessname.check4InnerClass();
        int flags = accessname.getAccess();
        CLASS_NAME.validate(innerclass);
        if (jvmVersion.compareTo(JvmVersion.V1_7) >= 0 && innername.isEmpty() && outerclass.isPresent()) {
            //"outer class must be absent if inner name is absent but is %s"
            LOG(M181,outerclass.orElseThrow());
            outerclass = Optional.empty();
        }
        innername.ifPresent(iname -> {
                INNER_CLASS_NAME.validate(iname);
                if (innerclass.equals(iname)) {
                    LOG(M247,innerclass,res_innername,iname); // "inner class %s must be different from %s %s"
                }
            });
        outerclass.ifPresent(CLASS_NAME::validate);
        InnerClassNode in = new InnerClassNode(innerclass,
                outerclass.orElse(null),
                innername.orElse(null),
                flags);
        ObjectLine<InnerClassNode> inline = new ObjectLine<>(in,line);
        ObjectLine<InnerClassNode> previous = innerClasses.putIfAbsent(innerclass, inline);
        if (previous == null) {
            in.accept(hdrnode);
        } else {
            LOG(M233,innerclass,dir,previous.line().getLinect()); // "Duplicate entry %s in %s: previous entry at line %d"
        }
    }

    private void setOuterClass(Directive dir, Line line) {
        String mspec = line.nextToken().asString();
        line.noMoreTokens();
        inner = true;
        switch(dir) {
            case dir_outer_class:
                if (!cname.startsWith(mspec)) {    // jls 13.1
                    // "enclosing class name(%s) is not a prefix of class name(%s)"
                    LOG(M261,mspec,cname);
                }
                if (outerLine == null) {
                    outerLine = line;
                    String outerClass = mspec;
                    hdrnode.visitOuterClass(outerClass,null,null);
                } else {
                    // "enclosing instance has already been defined%n   %s"
                    LOG(M268,outerLine);
                }
                break;
            case dir_enclosing_method:
                if (outerLine == null) {
                    EnclosingMethodHandle encloseMethod = EnclosingMethodHandle.getInstance(mspec);
                    outerLine = line;
                    hdrnode.visitOuterClass(encloseMethod.owner(), encloseMethod.name(), encloseMethod.desc());
                } else {
                    // "enclosing instance has already been defined%n   %s"
                    LOG(M268,outerLine);
                }
                break;
            default:
                throw new EnumConstantNotPresentException(dir.getClass(), dir.name());
        }
    }

    private boolean sameOwnerAsClass(String token) {
        boolean ok = CLASS_NAME.validate(token);
        if (ok && !HandlePart.isSamePackage(cname, token)) {
            LOG(M306,cname,token); // "nested class have different owners; class = %s token = %s",
            ok = false;
        }
        return ok;
    }
    
    private void setHostClass(Line line) {
        String host = line.lastToken().asName();
        sameOwnerAsClass(host);
        Line hostline = unique_directives.get(Directive.dir_nestmember);
        if (hostline == null) {
            hdrnode.nestHostClass = host;
            inner = true;
        } else {
            LOG(M304, Directive.dir_nestmember, hostline); // "%s has already been defined%n  %s"
        }
    }

    private void setMemberClass(Line line) {
        Line hostline = unique_directives.get(Directive.dir_nesthost);
        if (hostline == null) {
            TokenArray.listString(Directive.dir_nestmember, line, this::sameOwnerAsClass)
                    .stream()
                    .forEach(hdrnode::visitNestMember);
        } else {
            LOG(M304, Directive.dir_nesthost, hostline); // "%s has already been defined%n  %s"
        }
    }

    private void setPermittedSubclass(Line line) {
        if (accessName.is(acc_final)) {
            LOG(M313,Directive.dir_permittedSubclass); // "final class cannot have %s"
            return;
        }
        TokenArray.listString(Directive.dir_permittedSubclass, line, CLASS_NAME)
                .stream()
                .forEach(hdrnode::visitPermittedSubclass);
    }
    
    private void setHints(JynxScanner js, Line line) {
        try (TokenArray dotarray = line.getTokenArray()) {
            hints.setHints(dotarray);
        }
    }
    
    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        return hdrnode.visitAnnotation(desc, visible);
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeref, TypePath tp, String desc, boolean visible) {
        if (classType == ClassType.MODULE_CLASS) {
            throw new LogIllegalStateException(M370); // "Type annotations not allowed for Module"
        }
        return hdrnode.visitTypeAnnotation(typeref, tp, desc, visible);
    }

    public ASMClassHeaderNode endHeader() {
        if (hdrnode == null) {
            throw new IllegalStateException();
        }
        if (csuper == null && !Constants.isObjectClass(cname)) {
            csuper = classType.defaultSuper();
        }
        hdrnode.superName = csuper;
        try {
            visitHeader();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            LOG(M394,ex.toString()); // "END OF CLASS HEADER - SHOULD NOT APPEAR!; %s"
            throw new AssertionError();
        }
        ASMClassHeaderNode result = hdrnode;
        hdrnode = null;
        return result;
    }
    
    private void visitHeader() {
        if (source == null && defaultSource != null) {
            LOG(M143,Directive.dir_source,defaultSource); // "%s %s assumed"
            source = defaultSource;
        }
        if (source != null || debugBuilder != null) {
            hdrnode.visitSource(source, debugBuilder == null? null : debugBuilder.toString());
        }
        for (ObjectLine<InnerClassNode> inline:innerClasses.values()) {
            InnerClassNode in = inline.object();
            if (in.name.equals(cname)) {
                inner = true;
            }
        }
        if (cname.contains("$") && !inner) {
            LOG("; .end_header",M52); // "class name contains '$' but is not an internal class"
        }
    }

}
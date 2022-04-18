package asm;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassTooLargeException;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodTooLargeException;
import org.objectweb.asm.RecordComponentVisitor;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.BasicVerifier;
import org.objectweb.asm.tree.analysis.Interpreter;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.util.CheckClassAdapter;

import static jvm.AccessFlag.acc_final;
import static jvm.AttributeName.StackMapTable;
import static jynx.Global.*;
import static jynx.Message.*;
import static jynx.ReservedWord.*;
import static jynx2asm.NameDesc.*;

import jvm.AccessFlag;
import jvm.Constants;
import jvm.Context;
import jvm.Feature;
import jvm.JvmVersion;
import jynx.Access;
import jynx.ClassType;
import jynx.Directive;
import jynx.GlobalOption;
import jynx.LogIllegalStateException;
import jynx2asm.ClassChecker;
import jynx2asm.JynxScanner;
import jynx2asm.Line;
import jynx2asm.NameDesc;
import jynx2asm.ObjectLine;
import jynx2asm.ONDRecord;
import jynx2asm.OwnerNameDesc;
import jynx2asm.TokenArray;
import jynx2asm.TypeHints;

public class JynxClassHdr implements ContextDependent {

    private final ClassVisitor cv;
    private final JynxClassWriter cw;

    private boolean hasBeenVisited;

    private final JvmVersion jvmVersion;
    private final ClassType classType;
    private final Access accessName;
    private final String cname;

    private final String source;
    private String debugStr;
 
    private final ClassChecker checker;

    private String csignature;
    private String csuper;
    private ObjectLine<OwnerNameDesc> outer;
    private String host;

    private final List<String> cimplements;
    private final List<InnerClassNode> innerClasses;
    private final List<String> members;

    private final List<AcceptsVisitor> annotations;

    private final List<String> permittedSubclasses;
    
    private VerifierFactory verifierFactory;
    
    private final Map<Directive,Line> unique_directives;
    private final Map<String, Line> unique_attributes;
    private final TypeHints hints;

    private JynxClassHdr(int cwflags, JvmVersion version, String source,
            String cname, Access accessname, ClassType classtype) {
        this.hints = new TypeHints();
        this.cw = new JynxClassWriter(cwflags,hints);
        this.cv = new CheckClassAdapter(cw, false);
        this.jvmVersion = version;
        this.source = source;
        this.cname = cname;
        this.accessName = accessname;
        this.cimplements = new ArrayList<>();
        this.innerClasses = new ArrayList<>();
        this.classType = classtype;
        this.checker = ClassChecker.getInstance(cname,accessname,classtype,version);
        this.members = new ArrayList<>();
        this.permittedSubclasses = new ArrayList<>();
        this.annotations = new ArrayList<>();
        this.csignature = null;
        this.csuper = null;
        this.outer = null;
        this.host = null;
        this.unique_directives = new HashMap<>();
        this.unique_attributes = new HashMap<>();
        LOGGER().pushContext();
    }
    
    public static JynxClassHdr getInstance(
            JvmVersion jvmversion, String source, Line line, ClassType classtype) {
        String cname;
        EnumSet<AccessFlag> flags;
        switch (classtype) {
            case MODULE_CLASS:
                flags = EnumSet.noneOf(AccessFlag.class);
                cname = Constants.MODULE_CLASS_NAME.toString();
                break;
            case PACKAGE:
                flags = line.getAccFlags();
                cname = line.nextToken().asName();
                CLASS_NAME.validate(cname);
                cname += "/" + Constants.PACKAGE_INFO_NAME.toString();
                jvmversion.checkSupports(Feature.package_info);
                break;
            default:
                flags = line.getAccFlags();
                cname = line.nextToken().asName();
                CLASS_NAME.validate(cname);
                break;
        }
        flags.addAll(classtype.getMustHave4Class(jvmversion)); 
        Access accessname = Access.getInstance(flags, jvmversion, cname,classtype);
        accessname.check4Class();
        boolean usestack = OPTION(GlobalOption.USE_STACK_MAP);
        int cwflags;
        if (jvmversion.supports(StackMapTable)) {
            cwflags = usestack?0:ClassWriter.COMPUTE_FRAMES;
        } else {
            cwflags = usestack?0:ClassWriter.COMPUTE_MAXS;
        }
        return new JynxClassHdr(cwflags,jvmversion, source, cname, accessname,classtype);
    }

    @Override
    public void visitDirective(Directive dir, JynxScanner js) {
        Line line = js.getLine();
        dir.checkUnique(unique_directives, line);
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
            case dir_enclosing_class:
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
                visitCommonDirective(dir, line, js,unique_attributes);
                break;
        }
    }
    
    public byte[] toByteArray() {
        byte[] ba = null;
        try {
            ba = cw.toByteArray();
        } catch (ClassTooLargeException | MethodTooLargeException ex) {
            LOG(ex);
        }
        return ba;
    }

    public boolean isFinal() {
        return accessName.is(acc_final);
    }    

    @Override
    public Context getContext() {
        return Context.CLASS;
    }

    private void setDebug(Line line) {
        String debugx = line.lastToken().asQuoted();
        if (this.debugStr == null) this.debugStr = "";
        this.debugStr += debugx;
    }

    public String getClassName() {
        if (cname == null) {
            throw new IllegalStateException();
        }
        return cname;
    }
    
    @Override
    public void setSignature(Line line) {
        String signaturex = line.lastToken().asQuoted();
        CLASS_SIGNATURE.validate(signaturex);
        csignature = signaturex;
    }

    private void setSuper(Line line) {
        String csuperx = line.lastToken().asString();
        CLASS_NAME.validate(csuperx);
        switch(classType) {
            case MODULE_CLASS:
                LOG(M181,Directive.dir_super,csuperx);   // "%s directive is invalid for MODULE - value specifued was %s"
                break;
            case PACKAGE:
                if (!Constants.OBJECT_CLASS.equalString(csuperx)) {
                    // "%s directive for %s must be %s but is %s"
                    LOG(M186,Directive.dir_super,ClassType.PACKAGE,NameDesc.OBJECT_NAME,csuperx);
                    csuperx = Constants.RECORD_SUPER.toString();
                }
                break;
            case RECORD:
                if (!Constants.RECORD_SUPER.equalString(csuperx)) {
                    // "%s directive for %s must be %s but is %s"
                    LOG(M186,Directive.dir_super,ClassType.RECORD,Constants.RECORD_SUPER,csuperx);
                    csuperx = Constants.RECORD_SUPER.toString();
                }
                break;
        }
        csuper = csuperx;
    }

    public void setImplements(Line line) {
        String token = line.lastToken().asString();
        CLASS_NAME.validate(token);
        this.cimplements.add(token);
    }

    private void setInnerClass(Directive dir,Line line) {
        ClassType classtype;
        switch (dir) {
            case dir_inner_class:
                classtype = ClassType.BASIC;
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
        EnumSet<AccessFlag> accflags = line.getAccFlags();
        String innerclass;
        String innername;
        innerclass = line.nextToken().asName();
        innername = line.optAfter(res_innername);
        accflags.addAll(classtype.getMustHave4Inner(jvmVersion));
        Access accessname = Access.getInstance(accflags, jvmVersion, innerclass,classtype);
        accessname.check4InnerClass();
        int flags = accessname.getAccess();
        String outerclass = line.optAfter(res_outer);
        line.noMoreTokens();
        CLASS_NAME.validate(innerclass);
        if (innername != null) {
            INNER_CLASS_NAME.validate(innername);
            if (innerclass.equals(innername)) {
                LOG(M247,innerclass,res_innername,innername); // "inner class %s must be different from %s %s"
            }
        }
        if (outerclass != null) CLASS_NAME.validate(outerclass);
        InnerClassNode in = new InnerClassNode(innerclass, outerclass,innername, flags);
        innerClasses.add(in);
    }

    private void setOuterClass(Directive dir, Line line) {
        String mspec = line.nextToken().asString();
        line.noMoreTokens();
        OwnerNameDesc cmd;
        switch(dir) {
            case dir_enclosing_class:
                if (!cname.startsWith(mspec)) {    // jls 13.1
                    // "enclosing class name(%s) is not a prefix of class name(%s)"
                    LOG(M261,mspec,cname);
                }
                cmd = OwnerNameDesc.getClass(mspec);
                break;
            case dir_enclosing_method:
                cmd = OwnerNameDesc.getOwnerMethodDesc(mspec);
                break;
            default:
                throw new EnumConstantNotPresentException(dir.getClass(), dir.name());
        }
        if (outer == null) {
            outer = new ObjectLine<>(cmd,line);
        } else {
            // "enclosing instance has already been defined"
            LOG(M268);
        }
    }

    private void sameOwnerAsClass(String token) {
        String classOwner = ONDRecord.packageNameOf(cname);
        String tokenOwner = ONDRecord.packageNameOf(token);
        if (!classOwner.equals(tokenOwner)) {
            LOG(M306,classOwner,tokenOwner); // "nested class have different owners; class = %s token = %s",
        }
    }
    
    private void setHostClass(Line line) {
        String hostx = line.nextToken().asName();
        line.noMoreTokens();
        sameOwnerAsClass(hostx);
        if (!members.isEmpty()) {
            LOG(M289);   // "A nest member has already been defined"
            return;
        }
        host = hostx;
    }

    private void setMemberClass(Line line) {
        String member = line.lastToken().asName();
        sameOwnerAsClass(member);
        Line hostline = unique_directives.get(Directive.dir_nesthost);
        if (hostline != null) {
            LOG(M304,hostline); // "Nest host already defined%n  %s"
            return;
        }
        members.add(member);
    }

    private void setPermittedSubclass(Line line) {
        if (accessName.is(acc_final)) {
            LOG(M313,Directive.dir_permittedSubclass); // "final class cannot have %s"
            return;
        }
        String subclass = line.lastToken().asString();
        CLASS_NAME.validate(subclass);
        permittedSubclasses.add(subclass);
    }
    
    private void setHints(JynxScanner js, Line line) {
        TokenArray dotarray = TokenArray.getInstance(js, line);
        hints.setHints(dotarray);
    }
    
    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        JynxAnnotationNode jan = JynxAnnotationNode.getInstance(desc,visible);
        annotations.add(jan);
        return jan;
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeref, TypePath tp, String desc, boolean visible) {
        if (classType == ClassType.MODULE_CLASS) {
            throw new LogIllegalStateException(M370); // "Type annotations not allowed for Module"
        }
        JynxTypeAnnotationNode tan = JynxTypeAnnotationNode.getInstance(typeref, tp, desc,visible);
        annotations.add(tan);
        return tan;
    }

    private void visit() {
        if (hasBeenVisited) {
            throw new IllegalStateException();
        }
        if (csuper == null && !Constants.OBJECT_CLASS.equalString(cname)) {
            switch(classType) {
                case MODULE_CLASS:
                    break;
                case RECORD:
                    csuper = Constants.RECORD_SUPER.toString();
                    break;
                case ENUM:
                    csuper = Constants.ENUM_SUPER.toString();
                    break;
                default:
                    csuper = Constants.OBJECT_CLASS.toString();
                    break;
            }
            if (csuper != null) {
                LOG(M327,Directive.dir_super,csuper); // "added: %s %s"
            }
        }
        checker.setSuper(csuper);
        String[] interfaces = cimplements.toArray(new String[0]);
        int cflags = accessName.getAccess();
        cv.visit(jvmVersion.getRelease(), cflags, cname, csignature, csuper, interfaces);
        hasBeenVisited = true;
    }

    public void endHeader() {
        visit();
        try {
            visitHeader();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            LOG(M394,ex.toString()); // "END OF CLASS HEADER - SHOULD NOT APPEAR!; %s"
            throw new AssertionError();
        }
    }
    
    private void visitHeader() {
        if (source != null || debugStr != null) {
            cv.visitSource(source, debugStr);
        }
        boolean inner = false;
        if (host != null) {
            cv.visitNestHost(host);
            inner = true;
        }
        if (outer != null) {
            OwnerNameDesc ond = outer.object();
            cv.visitOuterClass(ond.getOwner(),ond.getName(),ond.getDesc());
            inner = true;
        }
        annotations.stream()
                .forEach(jan -> jan.accept(cv));
        for (String  member:members) {
            cv.visitNestMember(member);
        }
        for (String subclass:permittedSubclasses) {
            cv.visitPermittedSubclass(subclass);
        }
        for (InnerClassNode in:innerClasses) {
            in.accept(cv);
            if (in.name.equals(cname)) {
                inner = true;
            }
        }
        if (cname.contains("$") && !inner) {
            LOG(M52); // "class name contains '$' but is not an internal class"
        }
        verifierFactory = new VerifierFactory(cname, csuper, cimplements, permittedSubclasses, hints);

    }

    public JynxMethodNode getJynxMethodNode(Line line) {
        JynxMethodNode jmn =  JynxMethodNode.getInstance(line,checker);
        return jmn;
    }

    public JynxFieldNode getJynxFieldNode(Line line) {
         JynxFieldNode jfn = JynxFieldNode.getInstance(this,line,checker);
         return jfn;
    }
    
    public JynxComponentNode getJynxComponentNode(Line line) {
        JynxComponentNode jcn = JynxComponentNode.getInstance(this,line,checker);
        return jcn;
    }
    
    public void visitEnd() {
        checker.visitEnd();
        cv.visitEnd();
    }
    
    public void acceptMethod(JynxMethodNode jmethodnode) {
        MethodNode mv = jmethodnode.visitEnd();
        boolean verified = false;
        Interpreter<BasicValue> verifier;
        if (OPTION(GlobalOption.SIMPLE_VERIFIER)) {
            verifier = verifierFactory.getSimpleVerifier(accessName.is(AccessFlag.acc_interface));
        } else {
            verifier = new BasicVerifier();
        }
        Analyzer<BasicValue> analyzer = new Analyzer<>(verifier);
        try {
            analyzer.analyze(cname, mv);
            verified =  true;
        } catch (AnalyzerException e) {
            String emsg = e.getMessage();
            LOG(M75,mv.name,GlobalOption.SIMPLE_VERIFIER,emsg); // "Method %s failed %s check:%n    %s"
        }
        if (verified) {
            mv.accept(cv);
        }
    }
    
    public RecordComponentVisitor visitRecordComponent(String name, String desc, String signature) {
        return cv.visitRecordComponent(name, desc, signature);
    }
    
    public FieldVisitor visitField(int access, String name,String desc,String signature,Object value) {
        return cv.visitField(access, name, desc, signature, value);
    }
    
    public void acceptModule(JynxModule jmodule) {
        jmodule.getModNode().accept(cv);
    }

}

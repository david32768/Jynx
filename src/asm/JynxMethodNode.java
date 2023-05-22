package asm;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;

import static jvm.AttributeName.*;
import static jvm.Context.METHOD;
import static jynx.Global.*;
import static jynx.Message.*;

import jvm.AccessFlag;
import jvm.Context;
import jvm.FrameType;
import jynx.Access;
import jynx.Directive;

import jynx2asm.*;
import jynx2asm.handles.JynxHandle;
import jynx2asm.handles.LocalMethodHandle;
import jynx2asm.ops.JvmOp;
import jynx2asm.ops.JynxOps;

public class JynxMethodNode implements ContextDependent, HasAccessFlags {

    private MethodNode mnode;
    private final Line methodLine;
    private final int numparms;
    private final Access accessName;
    private final LocalMethodHandle lmh;
    private String signature;
    private final Map<String, Line> exceptions;

    private final MethodAnnotationLists annotationLists;
    private final Map<Directive,Line> unique_directives;

    private final ClassChecker checker;
    
    private final int errorsAtStart;
    
    private JynxMethodNode(Line line, Access accessname, LocalMethodHandle lmh,  ClassChecker checker) {
        this.errorsAtStart = LOGGER().numErrors();
        this.lmh = lmh;
        this.accessName = accessname;
        this.numparms = Type.getArgumentTypes(lmh.desc()).length;
        this.methodLine = line;
        this.exceptions = new LinkedHashMap<>();
        this.annotationLists = new MethodAnnotationLists(numparms);
        this.signature = null;
        this.unique_directives = new HashMap<>();
        this.checker = checker;
    }

    public static JynxMethodNode getInstance(Line line, ClassChecker checker) {
        Access accessname = checker.getAccess(METHOD,line);
        line.noMoreTokens();
        LocalMethodHandle lmh = LocalMethodHandle.getInstance(accessname.getName());
        if (lmh.isInit()) {
            accessname.check4InitMethod();
        } else {
            if (checker.isComponent(METHOD, lmh.name(), lmh.desc())) {
                accessname.setComponent();
            }
            accessname.check4Method();
        }
        JynxMethodNode jmn =  new JynxMethodNode(line,accessname,lmh,checker);
        checker.checkMethod(jmn);
        return jmn;
    }

    public boolean hasAnnotations() {
        return annotationLists.hasAnnotations();
    }
    
    public String getName() {
        return lmh.name();
    }
    
    public String getDesc() {
        return lmh.desc();
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
            case dir_parameter_annotation:
                JynxAnnotation.setAnnotation(dir, this, js);
                break;
            default:
                visitCommonDirective(dir, line, js);
                break;
        }
    }

    private void endHeader() {
      int access = accessName.getAccess();
      mnode = new MethodNode(access, getName(), getDesc(), signature, exceptions.keySet().toArray(new String[0]));
      annotationLists.accept(mnode);
    }
    
    public JynxCodeHdr getJynxCodeHdr(JynxScanner js, JynxOps opmap) {
        endHeader();
        if (isAbstractOrNative()) {
            LOG(M155); // "code is not allowed as method is abstract or native"
            return null;
        }
        boolean isStatic = is(AccessFlag.acc_static);
        JynxLabelMap labelmap = new JynxLabelMap();
        String virtclname = isStatic? null: checker.getClassName();
        List<Object> localStack = FrameType.getInitFrame(virtclname, lmh); // classname set non null for virtual methods
        LocalVars lv = LocalVars.getInstance(localStack, mnode.parameters, isStatic, annotationLists.getFinalParms());
        JvmOp returnop = JynxHandle.getReturnOp(lmh);
        OperandStack opstack = OperandStack.getInstance(localStack);
        StackLocals stackLocals = StackLocals.getInstance(lv, opstack, labelmap, returnop);
        String2Insn s2a = String2Insn.getInstance(labelmap, checker, opmap);
        return JynxCodeHdr.getInstance(js, mnode, stackLocals, localStack, s2a);
    }

    public LocalMethodHandle getLocalMethodHandle() {
        return lmh;
    }
    
    @Override
    public boolean is(AccessFlag flag) {
        assert flag.isValid(METHOD);
        return accessName.is(flag);
    }

    @Override
    public Context getContext() {
        return Context.METHOD;
    }

    private void setThrow(Line line) {
        if (isComponent()) {
            LOG(M128,Directive.dir_throws,getName());   // "% directive not allowed for component method %s"
            return;
        }
        TokenArray.arrayString(exceptions, Directive.dir_throws, line, NameDesc.CLASS_NAME);
    }
    
    @Override
    public void setSignature(Line line) {
        CHECK_SUPPORTS(Signature);
        signature = line.lastToken().asQuoted();
        NameDesc.METHOD_SIGNATURE.validate(signature);
        if (isComponent()) {
            checker.checkSignature4Method(signature, getName(), getDesc());
        }
    }

    private void visitParameter(Line line) {
        int parmnum = line.nextToken().asUnsignedInt();
        Access accessname = checker.getAccess(Context.PARAMETER, line);
        line.noMoreTokens();
        accessname.check4Parameter();
        annotationLists.visitParameter(parmnum, accessname);
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
        if (signature == null && isComponent()) {
            checker.checkSignature4Method(signature, getName(), getDesc());
        }
        checker.endMethod(this);
        try {
            mnode.visitEnd();
        } catch (Exception ex) {
            LOG(ex);
        }
        boolean ok = LOGGER().numErrors() == errorsAtStart;
        if (ok) {
            return mnode;
        } else {
            LOG(M296,getName(),getDesc()); // "method %s%s not added as contains errors"
            return null;
        }
    }

}

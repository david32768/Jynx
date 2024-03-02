package asm;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;

import static jvm.StandardAttribute.RuntimeInvisibleParameterAnnotations;
import static jvm.StandardAttribute.RuntimeVisibleParameterAnnotations;
import static jvm.StandardAttribute.Signature;
import static jynx.Global.*;
import static jynx.Message.*;

import jvm.AccessFlag;
import jvm.Context;
import jynx.Access;
import jynx.Directive;

import jynx2asm.*;
import jynx2asm.frame.MethodParameters;
import jynx2asm.handles.LocalMethodHandle;
import jynx2asm.ops.JynxOps;

public class JynxMethodNode implements ContextDependent, HasAccessFlags {

    private final MethodNode mnode;
    private final Line methodLine;
    private final Access accessName;
    private final LocalMethodHandle lmh;

    private final MethodParametersBuilder parametersBuilder;
    private final Map<Directive,Line> unique_directives;

    private final ClassChecker checker;
    
    private final int errorsAtStart;
    
    private JynxMethodNode(Line line, MethodNode mnode, Access accessname, LocalMethodHandle lmh,  ClassChecker checker) {
        this.errorsAtStart = LOGGER().numErrors();
        this.mnode = mnode;
        this.lmh = lmh;
        this.accessName = accessname;
        this.methodLine = line;
        int numparms = Type.getArgumentTypes(lmh.desc()).length;
        this.parametersBuilder = new MethodParametersBuilder(numparms);
        this.unique_directives = new HashMap<>();
        this.checker = checker;
    }

    public static JynxMethodNode getInstance(Line line, ClassChecker checker) {
        Access accessname = checker.getAccess(Context.METHOD,line);
        line.noMoreTokens();
        LocalMethodHandle lmh = LocalMethodHandle.getInstance(accessname.name());
        if (lmh.isInit()) {
            accessname.check4InitMethod();
        } else {
            if (checker.isComponent(Context.METHOD, lmh.name(), lmh.desc())) {
                accessname.setComponent();
            }
            accessname.check4Method();
        }
        MethodNode mnode = new MethodNode(accessname.getAccess(), lmh.name(), lmh.desc(), null, null);
        JynxMethodNode jmn =  new JynxMethodNode(line, mnode, accessname, lmh, checker);
        // signature and exceptions to be set later if required
        checker.checkMethod(jmn);
        return jmn;
    }

    public String getName() {
        return mnode.name;
    }
    
    public String getDesc() {
        return mnode.desc;
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
      parametersBuilder.accept(mnode);
    }
    
    public JynxCodeHdr getJynxCodeHdr(JynxScanner js, JynxOps opmap) {
        endHeader();
        if (isAbstractOrNative()) {
            LOG(M155); // "code is not allowed as method is abstract or native"
            return null;
        }
        boolean isStatic = is(AccessFlag.acc_static);
        String classname = checker.getClassName();
        MethodParameters parameters = parametersBuilder.getMethodParameters(lmh, isStatic, classname);
        String2Insn s2a = String2Insn.getInstance(checker, opmap);
        return JynxCodeHdr.getInstance(js, mnode, parameters, s2a);
    }

    public LocalMethodHandle getLocalMethodHandle() {
        return lmh;
    }
    
    public boolean isInit() {
        return lmh.isInit();
    }
    
    @Override
    public boolean is(AccessFlag flag) {
        assert flag.isValid(Context.METHOD);
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
        mnode.exceptions = TokenArray.listString(Directive.dir_throws, line, NameDesc.CLASS_NAME);
    }
    
    @Override
    public void setSignature(Line line) {
        assert mnode.signature == null; // dir_signature is unique within
        CHECK_SUPPORTS(Signature);
        String signature = line.lastToken().asQuoted();
        NameDesc.METHOD_SIGNATURE.validate(signature);
        if (isComponent()) {
            checker.checkSignature4Method(signature, getName(), getDesc());
        }
        mnode.signature = signature;
    }

    private void visitParameter(Line line) {
        int parmnum = line.nextToken().asUnsignedInt();
        Access accessname = checker.getAccessOptName(Context.PARAMETER, line);
        line.noMoreTokens();
        accessname.check4Parameter();
        parametersBuilder.visitParameter(parmnum, accessname);
    }
    
    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        return mnode.visitAnnotation(descriptor,visible);
    }

    @Override
    public AnnotationVisitor visitAnnotationDefault() {
        return mnode.visitAnnotationDefault();
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeref, TypePath tp, String descriptor, boolean visible) {
        return mnode.visitTypeAnnotation(typeref, tp, descriptor, visible);
    }

    private void visitAnnotableCount(Line line,boolean visible) {
        int count = line.nextToken().asInt();
        line.noMoreTokens();
        CHECK_SUPPORTS(visible?RuntimeVisibleParameterAnnotations:RuntimeInvisibleParameterAnnotations);
        parametersBuilder.visitAnnotableCount(count, visible);
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(String classdesc,int parameter, boolean visible) {
        parametersBuilder.checkParameterAnnotation(parameter, visible);
        return mnode.visitParameterAnnotation(parameter, classdesc, visible);
    }

    public MethodNode visitEnd() {
        if (mnode.instructions == null || mnode.instructions.size() == 0) {
            if (!isAbstractOrNative()) {
                LOG(M338); // "code missing but method is not native or abstract"
                return null;
            }
            endHeader();
        }
        String signature = mnode.signature;
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

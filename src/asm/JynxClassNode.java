package asm;

import java.io.PrintWriter;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.BasicVerifier;
import org.objectweb.asm.tree.analysis.Interpreter;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ModuleNode;
import org.objectweb.asm.tree.RecordComponentNode;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.TraceClassVisitor;

import static jynx.ClassType.RECORD;
import static jynx.Global.*;
import static jynx.GlobalOption.TRACE;
import static jynx.Message.*;

import classfile.ClassFileClassNode;
import jvm.AccessFlag;
import jvm.Feature;
import jynx.Access;
import jynx.ClassType;
import jynx.Directive;
import jynx.GlobalOption;
import jynx2asm.ClassChecker;
import jynx2asm.Line;
import jynx2asm.ObjectLine;
import jynx2asm.TypeHints;

public abstract class JynxClassNode {

    private final ClassVisitor cv;

    private final Access accessName;
 
    private final ClassChecker checker;

    private VerifierFactory verifierFactory;
    
    private final TypeHints hints;
    

    protected JynxClassNode(Access accessname, ClassVisitor basecv, TypeHints hints) {
        this.hints = hints;
        if (OPTION(TRACE)) {
            Printer printer = new ASMifier();
            PrintWriter pw = new PrintWriter(System.out);
            TraceClassVisitor tcv = new TraceClassVisitor(basecv, printer, pw);
            this.cv = new CheckClassAdapter(tcv, false);
        } else {
            this.cv = OPTION(GlobalOption.VALHALLA) && SUPPORTS(Feature.value)?
                    basecv:
                    new CheckClassAdapter(basecv, false);
        }
        this.accessName = accessname;
        this.checker = ClassChecker.getInstance(accessname);
    }
    
    public static JynxClassNode getInstance(Access accessname) {
        boolean usestack = OPTION(GlobalOption.USE_STACK_MAP);
        if (OPTION(GlobalOption.USE_CLASSFILE)) {
            try {
                return ClassFileClassNode.getInstance(accessname, usestack);
            } catch (UnsupportedOperationException ex) {
                LOG(M117, GlobalOption.USE_CLASSFILE); // "%s not supported; ASM ClassWriter used"
            }
        }
        return ASMClassNode.getInstance(accessname, usestack);
    }

    public abstract byte[] toByteArray();

    public String getClassName() {
        return accessName.name();
    }
    
    public JynxClassHdr getJynxClassHdr(ObjectLine<String> source, String defaultsource) {
        return JynxClassHdr.getInstance(accessName, source, defaultsource, hints);
    }
    
    public JynxMethodNode getJynxMethodNode(Line line) {
        return  JynxMethodNode.getInstance(line,checker);
    }

    public JynxFieldNode getJynxFieldNode(Line line) {
         return JynxFieldNode.getInstance(line,checker);
    }
    
    public JynxComponentNode getJynxComponentNode(Line line) {
        ClassType classtype = accessName.classType();
        if (classtype != RECORD) {
            LOG(M41);    // "component can only appear in a record"
        }
        JynxComponentNode jcn = JynxComponentNode.getInstance(line);
        checker.checkComponent(jcn);
        return jcn;
    }
    
    public void visitEnd() {
        checker.visitEnd();
        cv.visitEnd();
    }

    public void acceptClassHdr(JynxClassHdr jclasshdr) {
        ASMClassHeaderNode hdrnode = jclasshdr.endHeader();
        if (hdrnode != null) {
            verifierFactory = new VerifierFactory(hdrnode, hints);
            if (hdrnode.interfaces != null) {
                checker.hasImplements();
            }
            checker.setSuper(hdrnode.superName);
            hdrnode.accept(cv);
        }
    }
    
    public void acceptComponent(JynxComponentNode jcompnode, Directive dir) {
        RecordComponentNode compnode = jcompnode.visitEnd(dir);
        if (compnode == null) {
            return;
        }
        compnode.accept(cv);
    }
    
    public void acceptField(JynxFieldNode jfieldnode, Directive dir) {
        FieldNode fnode = jfieldnode.visitEnd(dir);
        if (fnode == null) {
            return;
        }
        fnode.accept(cv);
    }
    
    public void acceptMethod(JynxMethodNode jmethodnode) {
        MethodNode mnode = jmethodnode.visitEnd();
        if (mnode == null) {
            return;
        }
        boolean verified = false;
        String verifiername;
        Interpreter<BasicValue> verifier;
        if (OPTION(GlobalOption.BASIC_VERIFIER)) {
            verifier = new BasicVerifier();
            verifiername = GlobalOption.BASIC_VERIFIER.name();
        } else {
            verifier = verifierFactory.getSimpleVerifier(accessName.is(AccessFlag.acc_interface));
            verifiername = "SIMPLE_VERIFIER";
        }
        Analyzer<BasicValue> analyzer = new Analyzer<>(verifier);
        try {
            analyzer.analyze(accessName.name(), mnode);
            verified =  true;
        } catch (AnalyzerException | IllegalArgumentException e) {
            String emsg = e.getMessage();
            // "Method %s failed %s check:%n    %s"
            LOG(e, M75, mnode.name, verifiername, emsg);
        }
        if (verified) {
            try {
                mnode.accept(cv);
            } catch (TypeNotPresentException ex) {
                LOG(M411,ex.typeName()); // "type %s not found"
            }
        }
    }
    
    public void acceptModule(JynxModule jmodule) {
        ModuleNode modnode = jmodule.visitEnd();
        if (modnode == null) {
            return;
        }
        modnode.accept(cv);
    }

}

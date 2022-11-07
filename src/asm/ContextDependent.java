package asm;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.TypePath;

import static jynx.Message.*;

import jvm.Context;
import jynx.Directive;
import jynx.LogIllegalStateException;
import jynx2asm.JynxScanner;
import jynx2asm.Line;

public interface ContextDependent {
    
    public void visitDirective(Directive dir, JynxScanner js);

    // exceptions are thrown so tokens are skipped
    
    public default Context getContext() {
        throw new LogIllegalStateException(M42,Directive.dir_signature); // "%s invalid in context"
    }
    
    public default void setSource(Line line) {
        throw new LogIllegalStateException(M42,Directive.dir_signature); // "%s invalid in context"
    }
    
    public default void setSignature(Line line) {
        throw new LogIllegalStateException(M42,Directive.dir_signature); // "%s invalid in context"
    }
    
    public default AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        throw new LogIllegalStateException(M42,Directive.dir_annotation); // "%s invalid in context"
    }
    
    public default AnnotationVisitor visitTypeAnnotation(int typeref, TypePath tp, String desc, boolean visible) {
        throw new LogIllegalStateException(M42,Directive.dir_annotation); // "%s invalid in context"
    }
    
    public default AnnotationVisitor visitAnnotationDefault() {
        throw new LogIllegalStateException(M42,Directive.dir_annotation); // "%s invalid in context"
    }
    
    public default AnnotationVisitor visitParameterAnnotation(String desc, int parameter, boolean visible) {
        throw new LogIllegalStateException(M42,Directive.dir_annotation);  // "%s invalid in context"
    }
    
    public default void visitCommonDirective(Directive dir, Line line, JynxScanner js) {
        switch(dir) {
            case dir_source:
                setSource(line);
                break;
            case dir_signature:
                setSignature(line);
                break;
            default:
                if (dir.isAnotation()) {
                    JynxAnnotation.setAnnotation(dir,this,js);
                } else {
                    throw new EnumConstantNotPresentException(dir.getClass(), dir.name());
                }
        }
    }
    
}

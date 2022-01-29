package asm;

import java.util.Map;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.TypePath;

import static jynx.Message.*;

import jvm.Context;
import jynx.Directive;
import jynx.LogIllegalStateException;
import jynx2asm.JynxScanner;
import jynx2asm.Line;

public interface ContextDependent {
    
    public Context getContext();

    // exceptions are thrown so tokens are skipped
    
    
    public void visitDirective(Directive dir, JynxScanner js);

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
        throw new LogIllegalStateException(M42,Directive.dir_parameter);  // "%s invalid in context"
    }
    
    public default void visitCommonDirective(Directive dir, Line line, JynxScanner js, Map<String, Line> unique_attributes) {
        switch(dir) {
            case dir_signature:
                setSignature(line);
                break;
            case dir_invisible_annotation:
            case dir_visible_annotation:
            case dir_invisible_type_annotation:
            case dir_visible_type_annotation:
                JynxAnnotation.setAnnotation(dir,this,js);
                break;
            default:
                throw new AssertionError();
        }
    }
    
}

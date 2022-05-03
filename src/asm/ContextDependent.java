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
        throw new LogIllegalStateException(M42,Directive.dir_annotation);  // "%s invalid in context"
    }
    
    public default void visitCommonDirective(Directive dir, Line line, JynxScanner js) {
        switch(dir) {
            case dir_signature:
                setSignature(line);
                break;
            case dir_annotation:
            case dir_argmethod_type_annotation:
            case dir_argmethodref_type_annotation:
            case dir_argnew_type_annotation:
            case dir_argnewref_type_annotation:
            case dir_cast_type_annotation:
            case dir_except_type_annotation:
            case dir_extends_type_annotation:
            case dir_field_type_annotation:
            case dir_formal_type_annotation:
            case dir_instanceof_type_annotation:
            case dir_methodref_type_annotation:
            case dir_new_type_annotation:
            case dir_newref_type_annotation:
            case dir_param_bound_type_annotation:
            case dir_param_type_annotation:
            case dir_receiver_type_annotation:
            case dir_resource_type_annotation:
            case dir_return_type_annotation:
            case dir_throws_type_annotation:
            case dir_var_type_annotation:
                JynxAnnotation.setAnnotation(dir,this,js);
                break;
            default:
                throw new EnumConstantNotPresentException(dir.getClass(), dir.name());
        }
    }
    
}

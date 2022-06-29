package asm;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.util.CheckAnnotationAdapter;

import static jvm.Context.ANNOTATION;
import static jynx.Global.*;
import static jynx.Message.*;
import static jynx.ReservedWord.*;

import jvm.ConstType;
import jvm.Context;
import jvm.TypeRef;
import jynx.Directive;
import jynx.LogAssertionError;
import jynx.LogIllegalArgumentException;
import jynx.LogIllegalStateException;
import jynx.ReservedWord;
import jynx2asm.JynxScanner;
import jynx2asm.Line;
import jynx2asm.LinesIterator;
import jynx2asm.NameDesc;
import jynx2asm.Token;
import jynx2asm.TokenArray;

public class JynxAnnotation {

    private final ContextDependent sd;
    private final JynxScanner js;
    private final Directive dir;
    
    private JynxAnnotation(ContextDependent sd, JynxScanner js, Directive dir) {
        this.sd = sd;
        this.js = js;
        this.dir = dir;
    }

    public static void setAnnotation(Directive dir, ContextDependent sd, JynxScanner js) {
        JynxAnnotation ja = new JynxAnnotation(sd,js,dir);
        ja.visitAnnotation();
    }
    
    private void visitAnnotation() {
        AnnotationVisitor av;
        try {
            av = getAnnotationVisitor();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            js.skipTokens();
            LOG(ex);
            av = new AnnotationNode("throw_away"); // syntax check and throw away
        }
        av = new CheckAnnotationAdapter(av);
        visitAnnotationValues(av);
    }
    
    private AnnotationVisitor getAnnotationVisitor() {
        AnnotationVisitor av;
        int paramStart = 0;
        Line line = js.getLine();
        Context acctype = sd.getContext();
        switch (dir) {
            case dir_annotation:
                ReservedWord visibility = line.nextToken().expectOneOf(res_visible, res_invisible);
                String classdesc = line.nextToken().asString();
                NameDesc.CLASS_PARM.validate(classdesc);
                line.noMoreTokens();
                av = sd.visitAnnotation(classdesc, visibility == res_visible);
                break;
            case dir_default_annotation:
                av = sd.visitAnnotationDefault();
                line.noMoreTokens();
                break;
            case dir_parameter_annotation:
                visibility = line.nextToken().expectOneOf(res_visible, res_invisible);
                int parameter = line.nextToken().asInt();
                parameter -= paramStart;
                classdesc = line.nextToken().asString();
                NameDesc.CLASS_PARM.validate(classdesc);
                line.noMoreTokens();
                av = sd.visitParameterAnnotation(classdesc, parameter, visibility == res_visible);
                break;
            case dir_except_type_annotation:
                acctype = Context.CATCH;
                // fallthrough
            case dir_argmethod_type_annotation:
            case dir_argmethodref_type_annotation:
            case dir_argnew_type_annotation:
            case dir_argnewref_type_annotation:
            case dir_cast_type_annotation:
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
                visibility = line.nextToken().expectOneOf(res_visible, res_invisible);
                TypeRef tr = TypeRef.getInstance(dir,acctype);
                int numind = tr.getNumberIndices();
                int[] indices = new int[numind];
                for (int i = 0; i < numind; ++i) {
                    indices[i] = line.nextToken().asInt();
                }
                int typeref = tr.getTypeRef(indices);
                String typepathstr = line.optAfter(res_typepath);
                TypePath typepath = TypePath.fromString(typepathstr);
                String desc = line.nextToken().asString();
                av = sd.visitTypeAnnotation(typeref, typepath, desc, visibility == res_visible);
                break;
            default:
                // "unknown directive %s for context %s"
                throw new LogAssertionError(M907,dir,Context.ANNOTATION);
        }
        return av;
    }

    private void visitAnnotationValues(AnnotationVisitor av) {
        LinesIterator lines = new LinesIterator(js,Directive.end_annotation);
        while(lines.hasNext()) {
            Line line = lines.next();
            String name = line.firstToken().asString();
            String chdesc;
            if (dir == Directive.dir_default_annotation) {
                chdesc = name;
            } else {
                chdesc = line.nextToken().asString();
            }
            boolean array = chdesc.startsWith("[");
            if (array) {
                chdesc = chdesc.substring(1);
            }
            if (chdesc.length() != 1) {
                throw new LogIllegalArgumentException(M96); // "syntax error in annotation field type"
            }
            char typech = chdesc.charAt((0));
            switch (typech) {
                case '@': // case '&':
                    String desc = line.nextToken().asString();
                    line.nextToken().mustBe(equals_sign);
                    Token dot = line.nextToken();
                    if (array) {
                        dot.mustBe(dot_annotation_array);
                        line.noMoreTokens();
                        Directive enddir = Directive.end_annotation_array;
                        visitArrayOfAnnotations(av, name,desc,enddir);
                        break;
                    }
                    dot.mustBe(dot_annotation);
                    line.noMoreTokens();
                    visitAnnotationValues(av.visitAnnotation(name, desc));
                    break;
                case '\n':
                    AnnotationVisitor avnull = av.visitArray(name);
                    avnull.visitEnd();
                    break;
                default:
                    String enumdesc = typech == 'e'?line.nextToken().asString():null;
                    assert typech != '@';
                    ConstType cta = ConstType.getInstance(typech,ANNOTATION);
                    line.nextToken().mustBe(equals_sign);
                    if (array) {
                        AnnotationVisitor avarr = av.visitArray(name);
                        TokenArray tokens = TokenArray.getInstance(js, line);
                        while (true) {
                            Token token = tokens.firstToken();
                            if (token.is(right_array)) {
                                break;
                            }
                            Object value = token.getValue(cta);
                            visitvalue(avarr,name,enumdesc,value);
                            tokens.noMoreTokens();
                        }
                        avarr.visitEnd();
                    } else {
                        Token token = line.lastToken();
                        Object value = token.getValue(cta);
                        visitvalue(av,name,enumdesc,value);
                    }
                    break;
            }
        }
        av.visitEnd();
    }
    
    private void visitvalue(AnnotationVisitor av,String name,String enumdesc,Object value) {
        if (enumdesc == null) {
            av.visit(name, value);
        } else {
            av.visitEnum(name, enumdesc, value.toString());
        }
    }
    
    private void visitArrayOfAnnotations(AnnotationVisitor av, String name,String desc, Directive enddir) {
        AnnotationVisitor avarr = av.visitArray(name);
        while(true) {
            Line line = js.next();
            Token token = line.firstToken();
            Directive dirx = token.asDirective();
            if (dirx == enddir) {
                break;
            }
            if (dirx != Directive.dir_annotation) {
                throw new LogIllegalStateException(M168,dirx); // "unexpected directive(%s) in annotation"
            }
            line.noMoreTokens();
            AnnotationVisitor avarrav = avarr.visitAnnotation(name, desc);
            visitAnnotationValues(avarrav);
        }
        avarr.visitEnd();
    }

}

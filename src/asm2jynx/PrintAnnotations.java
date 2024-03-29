package asm2jynx;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableAnnotationNode;
import org.objectweb.asm.tree.TypeAnnotationNode;
import org.objectweb.asm.TypePath;

import static asm2jynx.Util.*;
import static jvm.Context.ANNOTATION;
import static jynx.Directive.*;
import static jynx.Global.*;
import static jynx.Message.*;
import static jynx.ReservedWord.res_typepath;

import jvm.ConstType;
import jvm.Context;
import jvm.TypeRef;
import jynx.Directive;
import jynx.ReservedWord;

public class PrintAnnotations {

    private final JynxStringBuilder lb;
    private final Object2String o2s;

    public PrintAnnotations(JynxStringBuilder lb) {
        this.lb = lb;
        this.o2s = new Object2String();
    }

    private void printTypeAnnotation(boolean visible,int typeref, TypePath tp, String desc) {
        TypeRef tr = TypeRef.fromASM(typeref);
        Directive dir = tr.getDirective();
        CHECK_SUPPORTS(dir);
        String typepath = tp == null?null:tp.toString();
        String trstr = tr.getTypeRefString(typeref);
        ReservedWord visibility = visible?ReservedWord.res_visible:ReservedWord.res_invisible;
        lb.append(dir)
                .append(visibility);
        if (!trstr.isEmpty()) {
            lb.append(trstr.split(" "));
        }
        lb.append(res_typepath, typepath)
            .append(desc)
            .nl();
    }
    
    public final void printAnnotations(List<AnnotationNode> visible,List<AnnotationNode> invisible) {
        printAnnotations(true,visible);
        printAnnotations(false,invisible);
    }
    
    public final void printDefaultAnnotation(Object obj, String mdesc) {
        if (obj == null) {
            return;
        }
        lb.append(Directive.dir_default_annotation)
                .nl()
                .incrDepth();
            int retind = mdesc.lastIndexOf(')');
            String desc = mdesc.substring(retind + 1);
            printAnnotationValue(null,obj,desc);
        lb.decrDepth()
                .append(Directive.end_annotation).nl();
    }

    private void printAnnotations(boolean visible, List<AnnotationNode> anlist) {
        ReservedWord visibility = visible?ReservedWord.res_visible:ReservedWord.res_invisible;
        for (AnnotationNode an : nonNullList(anlist)) {
            lb.append(dir_annotation)
                    .append(visibility)
                    .append(an.desc)
                    .nl();
            printAnnotation(an);
            lb.append(Directive.end_annotation).nl();
        }
    }

    private void printLength(boolean visible, int count,int min, int max) {
        Directive dirx = visible?dir_visible_parameter_count:dir_invisible_parameter_count;
        if (count < min || count > max) {
            LOG(M92,dirx,count,min,max);  // "%s count(%d) must be in range [%d,%d]"
            count = max;
        }
        boolean notdefault = count < max;
        if (notdefault) {
            lb.append(dirx).append(count).nl();
        }
    }

    private int maxnonnulls(Object[] objs) {
        for (int i = objs.length - 1; i>=0;--i) {
            if (objs[i] != null) {
                return i + 1;
            }
        }
        return 0;
    }
    
    public final void printParamAnnotations(List<AnnotationNode>[] visible,int visibleparms,
            List<AnnotationNode>[] invisible, int invisibleparms) {
            printParamAnnotations(true, visible, visibleparms, 0);
            printParamAnnotations(false, invisible, invisibleparms, 0);
    }
    
    private void printParamAnnotations(boolean visible, List<AnnotationNode>[] anlistarr, int parms, int index) {
        if (anlistarr == null) {
            return;
        }
        int min = maxnonnulls(anlistarr);
        int max = anlistarr.length;
        printLength(visible,parms,min,max);
        ReservedWord visibility = visible?ReservedWord.res_visible:ReservedWord.res_invisible;
        for (List<AnnotationNode> anlist:anlistarr) {
            for (AnnotationNode an : nonNullList(anlist)) {
                lb.append(dir_parameter_annotation)
                        .append(visibility)
                        .append(index)
                        .append(an.desc)
                        .nl();
                printAnnotation(an);
                lb.append(Directive.end_annotation).nl();
            }
            ++index;
        }
    }

    public void printAnnotation(AnnotationNode an) {
        lb.incrDepth();
        List<Object> listvalue = nonNullList(an.values);
        Iterator<Object> valueiter = listvalue.iterator();
        while (valueiter.hasNext()) {
            String name = (String) valueiter.next();
            Object obj = valueiter.next();
            printAnnotationValue(name,obj,null);
        }
        lb.decrDepth();
    }

    private void printAnnotationValue(String name, Object obj,String desc) {
        List<Object> values;
        boolean isArray;
        if (obj instanceof List) {
            @SuppressWarnings("unchecked") List<Object> valuesx = (List<Object>)obj;
            values = valuesx;
            isArray = true;
            if (values.isEmpty()) {
                // a zero array does not need a type, so use [I
                ConstType ct = desc == null? ConstType.ct_int:ConstType.getFromDesc(desc.substring(1), ANNOTATION);
                if (ct == ConstType.ct_annotation) {
                    lb.appendNonNull(name)
                            .append(ct.getJynxDesc(true))
                            .appendNonNull(desc.substring(1))
                            .append(ReservedWord.equals_sign)
                            .append(ReservedWord.dot_annotation_array)
                            .nl()
                            .append(Directive.end_annotation_array)
                            .nl();
                } else {
                    lb.appendNonNull(name)
                            .append(ct.getJynxDesc(true))
                            .append(ReservedWord.equals_sign)
                            .append(ReservedWord.left_array)
                            .append(ReservedWord.right_array)
                            .nl();
                }
                return;
            }
        } else {
            values = new ArrayList<>();
            values.add(obj);
            isArray = false;
        }
        Object objzero = values.get(0);
        for (Object objz:values) {
            if (objz == null) {
                LOG(M162,name); // "some annotation values for %s are null; annotation ignored"
                return;
            }
        }
        ConstType ct = ConstType.getFromASM(objzero,Context.ANNOTATION);

        switch(ct) {
            case ct_enum:
                String[] strings = (String[]) objzero;
                String enumstr = strings[0];
                printValuesEnum(name, ct, isArray, values, enumstr);
                break;
            case ct_annotation:
                AnnotationNode an = (AnnotationNode) objzero;
                printValuesAnnotation(name, ct, isArray, values, an.desc);
                break;
            default:
                printValues(name, ct, isArray, values);
                break;
        }
    }

    private void printValues(String name,ConstType ct, boolean isArray, List<Object> values) {
        String typestr = ct.getJynxDesc(isArray);
        lb.appendNonNull(name)
                .append(typestr)
                .append(ReservedWord.equals_sign);
        if (isArray) {
            lb.append(ReservedWord.dot_array)
                    .nl()
                    .incrDepth();
        }
        for (Object value : values) { // String, Type or numeric
            String strvalue = o2s.stringFrom(ct, value);
            assert strvalue != null; // already reported - M162
            lb.appendNonNull(strvalue);
            if (isArray) {
                lb.nl();
            }
        }
        if (isArray) {
            lb.decrDepth();
            lb.append(Directive.end_array);
        }
        lb.nl();
    }
    
    private void printValuesEnum(String name, ConstType ct, boolean isArray, List<Object> values, String enumstr) {
        String typestr = ct.getJynxDesc(isArray);
        lb.appendNonNull(name).append(typestr).appendNonNull(enumstr).append(ReservedWord.equals_sign);
        if (isArray) {
            lb.append(ReservedWord.dot_array)
                    .nl()
                    .incrDepth();
        }
        for (Object value : values) {
            String[] strings = (String[]) value;
            if (!enumstr.equals(strings[0])) {
                LOG(M205, enumstr, strings[0]); // "enum class changed; was %s now %s"
                break;
            }
            String strvalue = o2s.stringFrom(ConstType.ct_enum, strings[1]);
            assert strvalue != null; // already reported  - M162 
            lb.appendNonNull(strvalue);
            if (strvalue != null){
                lb.nl();
            }
        }
        if (isArray) {
            lb.decrDepth()
                    .append(Directive.end_array);
        }
        lb.nl();
    }
    
    private void printValuesAnnotation(String name, ConstType ct, boolean isArray, List<Object> values, String desc) {
        String typestr = ct.getJynxDesc(isArray);
        lb.appendNonNull(name).append(typestr).appendNonNull(desc).append(ReservedWord.equals_sign);
        if (isArray) {
            lb.append(ReservedWord.dot_annotation_array).nl();
            lb.incrDepth();
            for (Object anobj : values) {
                lb.append(Directive.dir_annotation).nl();
                printAnnotation((AnnotationNode) anobj);
                lb.append(Directive.end_annotation).nl();
            }
            lb.decrDepth()
                    .append(end_annotation_array).nl();
        } else {
            lb.append(ReservedWord.dot_annotation).nl();
            printAnnotation((AnnotationNode) values.get(0));
            lb.append(Directive.end_annotation).nl();
        }
    }
    
    public final void printTypeAnnotations(List<TypeAnnotationNode> visible,List<TypeAnnotationNode> invisible) {
        printTypeAnnotations(true,visible);
        printTypeAnnotations(false,invisible);
    }   
    
    private void printTypeAnnotations(boolean visible, List<TypeAnnotationNode> anlist) {
        for (TypeAnnotationNode tan : nonNullList(anlist)) {
            printTypeAnnotation(visible,tan.typeRef, tan.typePath, tan.desc);
            printAnnotation(tan);
            lb.append(Directive.end_annotation).nl();
        }
    }

    private void printInsnTypeAnnotations(boolean visibility, List<TypeAnnotationNode> anlist) {
        for (TypeAnnotationNode tan : nonNullList(anlist)) {
            printTypeAnnotation(visibility,tan.typeRef, tan.typePath, tan.desc);
            printAnnotation(tan);
            lb.append(Directive.end_annotation).nl();
        }
    }

    public final void printInsnTypeAnnotations(List<TypeAnnotationNode> visible,List<TypeAnnotationNode> invisible) {
        printInsnTypeAnnotations(true,visible);
        printInsnTypeAnnotations(false,invisible);
    }   
    
    private void printLocalVarAnnotations(boolean visible, List<LocalVariableAnnotationNode> anlist) {
        for (LocalVariableAnnotationNode lvan:nonNullList(anlist)) {
            int typeref = lvan.typeRef;
            TypeRef tr = TypeRef.fromASM(typeref);
            Directive dir = tr.getDirective();
            CHECK_SUPPORTS(dir);
            String typepath = lvan.typePath == null?null:lvan.typePath.toString();
            String trstr = tr.getTypeRefString(typeref);
            ReservedWord visibility = visible?ReservedWord.res_visible:ReservedWord.res_invisible;
            lb.append(dir)
                    .append(visibility);
            if (!trstr.isEmpty()) {
                lb.append(trstr.split(" "));
            }
            lb.append(res_typepath, typepath)
                .append(lvan.desc)
                .append(ReservedWord.dot_array)
                .nl()
                    .incrDepth()
                    .incrDepth();
                int entries = lvan.index.size();
                assert entries == lvan.start.size() && entries == lvan.end.size();
                Iterator<Integer> indexiter = lvan.index.iterator();
                Iterator<LabelNode> enditer = lvan.end.iterator();
                for (LabelNode start:lvan.start) {
                    int index = indexiter.next();
                    LabelNode end = enditer.next();
                    lb.append(index)
                            .appendLabelNode(start)
                            .appendLabelNode(end)
                            .nl();
                }
                lb.decrDepth();
            lb.append(Directive.end_array)
                    .nl()
                    .decrDepth();
            printAnnotation(lvan);
            lb.append(Directive.end_annotation).nl();
        }
    }

    public final void printLocalVarAnnotations(List<LocalVariableAnnotationNode> visible,
            List<LocalVariableAnnotationNode> invisible) {
        printLocalVarAnnotations(true,visible);
        printLocalVarAnnotations(false, invisible);
    }
    
}

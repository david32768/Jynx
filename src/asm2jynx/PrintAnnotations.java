package asm2jynx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Function;
import java.util.Iterator;
import java.util.List;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableAnnotationNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.TypeAnnotationNode;
import org.objectweb.asm.TypePath;

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

    private final LineBuilder lb;
    private final Object2String o2s;

    public PrintAnnotations(LineBuilder lb) {
        this.lb = lb;
        this.o2s = new Object2String();
    }

    private <E> List<E> nonNullList(List<E> list) {
        return list == null ? Collections.emptyList() : list;
    }

    private <E> List<E> nonNullList(List<E> list, Directive dir) {
        if (list != null && list.iterator().hasNext() && CHECK_SUPPORTS(dir)) {
            return list;
        } else {
            return Collections.emptyList();
        }
    }
    
    private void printTypeAnnotationDirective(int typeref, TypePath tp, String desc, Directive dir) {
        String typepath = tp == null?null:tp.toString();
        String trstr = TypeRef.getString(typeref);
        lb.appendDirective(dir)
                .append(trstr.split(" "))
                .append(res_typepath, typepath)
                .append(desc)
                .nl();
    }

    
    public final void printAnnotations(List<AnnotationNode> visible,List<AnnotationNode> invisible) {
        printAnnotations(visible, dir_visible_annotation);
        printAnnotations(invisible, dir_invisible_annotation);
    }
    
    public final void printDefaultAnnotation(Object obj, String mdesc) {
        if (obj == null) {
            return;
        }
        lb.appendDirective(Directive.dir_default_annotation).nl();
        lb.incrDepth();
        int retind = mdesc.lastIndexOf(')');
        String desc = mdesc.substring(retind + 1);
        printAnnotationValue(null,obj,desc);
        lb.decrDepth();
        lb.appendDirective(Directive.end_annotation).nl();
    }

    private void printAnnotations(List<AnnotationNode> anlist, Directive dir) {
        for (AnnotationNode an : JynxDisassemble.nonNullList(anlist)) {
            lb.appendDirective(dir).append(an.desc).nl();
            printAnnotation(an);
            lb.appendDirective(Directive.end_annotation).nl();
        }
    }

    private void printLength(int count,int min, int max, Directive dir) {
        boolean visible = !dir.toString().contains("invisible");
        Directive dirx = visible?dir_visible_parameter_count:dir_invisible_parameter_count;
        if (count < min || count > max) {
            LOG(M92,dir,count,min,max);  // "%s count(%d) must be in range [%d,%d]"
            count = max;
        }
        boolean notdefault = count < max;
        if (notdefault) {
            lb.appendDirective(dirx).append(count).nl();
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
            printParamAnnotations(visible, visibleparms, dir_visible_parameter_annotation,0);
            printParamAnnotations(invisible, invisibleparms, dir_invisible_parameter_annotation,0);
    }
    
    private void printParamAnnotations(List<AnnotationNode>[] anlistarr, int parms,Directive dir, int index) {
        if (anlistarr == null) {
            return;
        }
        int min = maxnonnulls(anlistarr);
        int max = anlistarr.length;
        printLength(parms,min,max,dir);

        for (List<AnnotationNode> anlist:anlistarr) {
            for (AnnotationNode an : nonNullList(anlist)) {
                lb.appendDirective(dir).append(index).append(an.desc).nl();
                printAnnotation(an);
                lb.appendDirective(Directive.end_annotation).nl();
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
                ConstType ct = desc == null? ConstType.ct_int:ConstType.getFromDesc(desc.substring(1), ANNOTATION);
                // a zero array does not need a type, so use [I
                lb.appendNonNull(name).append(ct.getJynx_desc(true)).appendNonNull(null).append(ReservedWord.equals_sign);
                lb.append(ReservedWord.left_array).append(ReservedWord.right_array);
                lb.nl();
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
        String typestr = ct.getJynx_desc(isArray);
        lb.appendNonNull(name).append(typestr).appendNonNull(null).append(ReservedWord.equals_sign);
        if (isArray) {
            lb.append(ReservedWord.dot_array).nl();
            lb.incrDepth();
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
        String typestr = ct.getJynx_desc(isArray);
        lb.appendNonNull(name).append(typestr).appendNonNull(enumstr).append(ReservedWord.equals_sign);
        if (isArray) {
            lb.append(ReservedWord.dot_array).nl();
            lb.incrDepth();
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
            lb.decrDepth();
            lb.append(Directive.end_array);
        }
        lb.nl();
    }
    
    private void printValuesAnnotation(String name, ConstType ct, boolean isArray, List<Object> values, String desc) {
        String typestr = ct.getJynx_desc(isArray);
        lb.appendNonNull(name).append(typestr).appendNonNull(desc).append(ReservedWord.equals_sign);
        if (isArray) {
            lb.append(ReservedWord.dot_annotation_array).nl();
            lb.incrDepth();
            for (Object anobj : values) {
                lb.appendDirective(Directive.dir_annotation).nl();
                printAnnotation((AnnotationNode) anobj);
                lb.appendDirective(Directive.end_annotation).nl();
            }
            lb.decrDepth();
            lb.appendDirective(end_annotation_array).nl();
        } else {
            lb.append(ReservedWord.dot_annotation).nl();
            printAnnotation((AnnotationNode) values.get(0));
            lb.appendDirective(Directive.end_annotation).nl();
        }
    }
    
    private void printTypeAnnotations(List<TypeAnnotationNode> anlist, Directive dir) {
        for (TypeAnnotationNode tan : nonNullList(anlist,dir)) {
            printTypeAnnotationDirective(tan.typeRef, tan.typePath, tan.desc,dir);
            printAnnotation(tan);
            lb.appendDirective(Directive.end_annotation).nl();
        }
    }

    public final void printTypeAnnotations(List<TypeAnnotationNode> visible,List<TypeAnnotationNode> invisible) {
        printTypeAnnotations(visible, dir_visible_type_annotation);
        printTypeAnnotations(invisible, dir_invisible_type_annotation);
    }   
    
    private void printInsnTypeAnnotations(List<TypeAnnotationNode> anlist, Directive dir) {
        for (TypeAnnotationNode tan : nonNullList(anlist,dir)) {
            printTypeAnnotationDirective(tan.typeRef, tan.typePath, tan.desc,dir);
            printAnnotation(tan);
            lb.appendDirective(Directive.end_annotation).nl();
        }
    }

    public final void printInsnTypeAnnotations(List<TypeAnnotationNode> visible,List<TypeAnnotationNode> invisible) {
        printInsnTypeAnnotations(visible, dir_visible_type_annotation);
        printInsnTypeAnnotations(invisible, dir_invisible_type_annotation);
    }   
    
    private void printLocalVarAnnotations(List<LocalVariableAnnotationNode> anlist, Directive dir,
                Function<LabelNode,String> lab2strfn,List<LocalVariableNode> lvnlist) {
        for (LocalVariableAnnotationNode lvan:nonNullList(anlist,dir)) {
            String typepath = lvan.typePath == null?null:lvan.typePath.toString();
            String trstr = TypeRef.getString(lvan.typeRef);
            lb.appendDirective(dir)
                    .append(trstr.split(" "))
                    .append(res_typepath, typepath)
                    .append(lvan.desc)
                    .append(ReservedWord.dot_array)
                    .nl();
            lb.incrDepth();
            int entries = lvan.index.size();
            assert entries == lvan.start.size() && entries == lvan.start.size();
            Iterator<Integer> indexiter = lvan.index.iterator();
            Iterator<LabelNode> enditer = lvan.end.iterator();
            for (LabelNode start:lvan.start) {
                int index = indexiter.next();
                LabelNode end = enditer.next();
                String startname = lab2strfn.apply(start);
                String endname = lab2strfn.apply(end);
                lb.append(index)
                        .append(startname)
                        .append(endname)
                        .nl();
            }
            lb.decrDepth();
            lb.appendDirective(Directive.end_array).nl();
            printAnnotation(lvan);
            lb.appendDirective(Directive.end_annotation).nl();
        }
    }

    public final void printLocalVarAnnotations(List<LocalVariableAnnotationNode> visible,
            List<LocalVariableAnnotationNode> invisible,
            Function<LabelNode,String> lab2strfn,List<LocalVariableNode> lvnlist) {
        printLocalVarAnnotations(visible, dir_visible_type_annotation, lab2strfn, lvnlist);
        printLocalVarAnnotations(invisible, dir_invisible_type_annotation, lab2strfn, lvnlist);
    }
    
}

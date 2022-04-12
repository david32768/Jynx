package asm2jynx;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ModuleExportNode;
import org.objectweb.asm.tree.ModuleNode;
import org.objectweb.asm.tree.ModuleOpenNode;
import org.objectweb.asm.tree.ModuleProvideNode;
import org.objectweb.asm.tree.ModuleRequireNode;
import org.objectweb.asm.tree.RecordComponentNode;

import static jvm.AccessFlag.*;
import static jvm.AttributeName.*;
import static jvm.Context.*;
import static jynx.Directive.*;
import static jynx.Global.*;
import static jynx.Message.*;
import static jynx.ReservedWord.*;

import asm.JynxClassReader;
import jvm.AccessFlag;
import jvm.Constants;
import jvm.ConstType;
import jvm.JvmVersion;
import jynx.ClassType;
import jynx.Directive;
import jynx.Global;
import jynx.GlobalOption;

public class JynxDisassemble {

    private final ClassNode cn;

    private final PrintWriter pw;
    
    private final JvmVersion jvmVersion;
    private final Object2String o2s;
    private final LineBuilder jp;
    private final PrintAnnotations annotator;
    private final JynxMethodPrinter jmp;

    private JynxDisassemble(ClassNode cn, PrintWriter pw, JvmVersion jvmversion) {
        this.pw = pw;
        this.o2s = new Object2String();
        this.jp = new LineBuilder(pw);
        this.jvmVersion = jvmversion;
        this.annotator = new PrintAnnotations(jp);
        this.cn = cn;
        this.jmp = JynxMethodPrinter.getInstance(cn.name, jvmVersion, jp, annotator);
    }

    public static JynxDisassemble getInstance(ClassReader cr, PrintWriter pw) {
        int poolsz = cr.getItemCount();
        if (poolsz >= 256) {
            LOG(M67,poolsz); // "poolsz = %d"
        }
        ClassNode cn = new ClassNode();
        cr.accept(cn, ClassReader.EXPAND_FRAMES);
        JvmVersion jvmversion = JvmVersion.getInstance(cn.version);
        if (jvmversion == JvmVersion.V1_6JSR && OPTION(GlobalOption.USE_STACK_MAP)) {
            jvmversion = JvmVersion.V1_6;
        }
        Global.setJvmVersion(jvmversion);
        return new JynxDisassemble(cn, pw, jvmversion);
    }

   public static <E> List<E> nonNullList(List<E> list) {
        return list == null ? Collections.emptyList() : list;
    }

    public void close() {
        pw.close();
    }

    private  static <E> boolean isAbsent(Iterable<E> list) {
        return list == null || !list.iterator().hasNext();
    }
    
    private  static <E> boolean isPresent(Iterable<E> list) {
        return !isAbsent(list);
    }

    private void printPackage() {
        String cname = cn.name;
        EnumSet<AccessFlag> accflags = AccessFlag.getEnumSet(cn.access,CLASS,jvmVersion);
        ClassType classtype = ClassType.PACKAGE;
        int index = cname.lastIndexOf('/');
        cname = cname.substring(0, index);
        accflags.removeAll(classtype.getMustHave(jvmVersion,false));
        Directive dir = classtype.getDir();
        jp.append(dir)
                .append(accflags,cname).nl();
        jp.incrDepth();
        jp.printDirective(dir_super, cn.superName);
        annotator.printAnnotations(cn.visibleAnnotations,cn.invisibleAnnotations);
        jp.decrDepth();
    }
    
    public boolean print() {
        printVersionSource();
        if (cn.module != null) {
            LOGGER().setLine("module " + cn.name);
            LOGGER().pushContext();
            printModuleHeader();
        } else if (cn.name.endsWith("/" + Constants.PACKAGE_INFO_NAME.toString())) {
            LOGGER().setLine("package " + cn.name);
            LOGGER().pushContext();
            printPackage();
        } else {
            LOGGER().setLine("class " + cn.name);
            LOGGER().pushContext();
            printClassHeader();
            for (FieldNode fn : nonNullList(cn.fields)) {
                printField(fn);
            }
            for (MethodNode mn : nonNullList(cn.methods)) {
                jmp.printMethod(mn);
            }
        }
        close();
        boolean success = END_MESSAGES(cn.name);
        return success;
    }
    
    private void printEnclosing(String outerClass, String outerMethod, String outerMethodDesc) {
        if (outerMethod != null || outerMethodDesc != null) {
            String cmdesc = outerMethodDesc == null?outerMethod:outerMethod + outerMethodDesc;
            if (outerClass != null) {
                cmdesc = outerClass + "/" + cmdesc;
            }
            jp.appendDirective(dir_enclosing_method)
                    .append(cmdesc)
                    .nl();
            
        } else if (outerClass != null) {
            jp.appendDirective(dir_enclosing_class)
                    .append(outerClass)
                    .nl();
        }
    }

    private void printClassHeader() {
        String cname = cn.name;
        EnumSet<AccessFlag> accflags = AccessFlag.getEnumSet(cn.access,CLASS,jvmVersion);
        ClassType classtype = ClassType.from(accflags);
        accflags.removeAll(classtype.getMustHave(jvmVersion,false));
        Directive dir = classtype.getDir();
        jp.append(dir)
                .append(accflags,cname).nl();
        jp.incrDepth();
        jp.printDirective(dir_super, cn.superName);
        for (String itf : nonNullList(cn.interfaces)) {
            jp.printDirective(dir_implements, itf);
        }
        jp.printDirective(dir_signature,cn.signature);
        jp.printDirective(dir_debug, cn.sourceDebug);
        printEnclosing(cn.outerClass,cn.outerMethod,cn.outerMethodDesc);
        jp.printDirective(dir_nesthost, cn.nestHostClass);
        annotator.printAnnotations(cn.visibleAnnotations,cn.invisibleAnnotations);
        annotator.printTypeAnnotations(cn.visibleTypeAnnotations, cn.invisibleTypeAnnotations);
        printInner();
        for (String nested: nonNullList(cn.nestMembers)) {
            jp.printDirective(dir_nestmember,nested);
        }
        for (String subtype: nonNullList(cn.permittedSubclasses)) {
            jp.printDirective(dir_permittedSubclass,subtype);
        }
        jp.decrDepth();
        printComponents();
    }

    private void printComponents() {
        List<RecordComponentNode> components = nonNullList(cn.recordComponents);
        for (RecordComponentNode rcn :components) {
            jp.append(dir_component)
                    .append(rcn.name)
                    .append(rcn.descriptor)
                    .append(res_signature,rcn.signature)
                    .nl();
            jp.incrDepth();
            if(isPresent(rcn.invisibleAnnotations)
                    || isPresent(rcn.visibleAnnotations)
                    || isPresent(rcn.visibleTypeAnnotations)
                    || isPresent(rcn.invisibleTypeAnnotations)
                    || isPresent(rcn.attrs)
                    ) {
                jp.incrDepth();
                annotator.printAnnotations(rcn.visibleAnnotations,rcn.invisibleAnnotations);
                annotator.printTypeAnnotations(rcn.visibleTypeAnnotations, rcn.invisibleTypeAnnotations);
                jp.appendDirective(end_component).nl();
                jp.decrDepth();
            }
        }
    }

    private void printInner() {
        for (InnerClassNode icn : nonNullList(cn.innerClasses)) {
            EnumSet<AccessFlag> inneraccflags = AccessFlag.getEnumSet(icn.access,INNER_CLASS,jvmVersion);
            ClassType classtype = ClassType.from(inneraccflags);
            inneraccflags.removeAll(classtype.getMustHave(jvmVersion,true));
            Directive inner = classtype.getInnerDir();
            jp.appendDirective(inner)
                    .append(inneraccflags, icn.name)
                    .append(res_innername, icn.innerName)
                    .append(res_outer, icn.outerName)
                    .nl();
        }
    }
    
    private EnumSet<Directive> getModuleAbsent() {
        EnumSet<Directive> result = EnumSet.noneOf(Directive.class);
        if (cn.superName != null) result.add(dir_super);
        if (isPresent(cn.interfaces)) result.add(dir_implements);
        if (isPresent(cn.methods)) result.add(dir_method);
        if (isPresent(cn.fields)) result.add(dir_field);
        
        if (cn.signature != null) result.add(dir_signature);
        if (cn.outerMethod != null || cn.outerMethodDesc != null) {
            result.add(dir_enclosing_method);
        } else if (cn.outerClass != null) {
            result.add(dir_enclosing_class);
        }
        if (cn.nestHostClass != null) result.add(dir_nesthost);
        if (isPresent(cn.nestMembers)) result.add(dir_nestmember);
        if (isPresent(cn.permittedSubclasses)) result.add(dir_permittedSubclass);
        return result;
    }
    
    private void printModuleHeader() {
        // specified at end of jvms 4.1
        assert Constants.MODULE_CLASS_NAME.equalString(cn.name);
        assert getModuleAbsent().isEmpty();
        assert isAbsent(cn.invisibleTypeAnnotations);
        assert isAbsent(cn.visibleTypeAnnotations);
        EnumSet<AccessFlag>  accflags = AccessFlag.getEnumSet(cn.access, CLASS,jvmVersion);
        assert accflags.contains(acc_module) && accflags.size() == 1;
        
        ModuleNode module = cn.module;
        accflags = AccessFlag.getEnumSet(module.access, MODULE,jvmVersion);
        jp.appendDirective(dir_module)
                .append(accflags,module.name)
                .append(res_main,module.mainClass)
                .append(module.version)
                .nl();

        jp.incrDepth();
        jp.printDirective(dir_debug, cn.sourceDebug);
        annotator.printAnnotations(cn.visibleAnnotations, cn.invisibleAnnotations);
        printInner();
        jp.decrDepth();
        printModuleInfo(module);
    }

    private void printField(FieldNode fn) {
        jp.blankline();
        LOGGER().setLine("field " + fn.name);
        EnumSet<AccessFlag> accflags = AccessFlag.getEnumSet(fn.access, FIELD,jvmVersion);
        jp.appendDirective(dir_field)
                .append(accflags, fn.name)
                .appendNonNull(fn.desc);
        boolean annotated = fn.visibleAnnotations != null || fn.invisibleAnnotations != null
                || fn.visibleTypeAnnotations != null || fn.invisibleTypeAnnotations != null;
        boolean endrequired = annotated;
        if (endrequired) {
            jp.incrDepth();
        } else {
            jp.append(res_signature, fn.signature);
        }
        if (fn.value != null) {
            jvmVersion.checkSupports(ConstantValue);
            ConstType ct = ConstType.getFromDesc(fn.desc,FIELD);
            jp.append(equals_sign, o2s.stringFrom(ct,fn.value));
        }
        String line = jp.nlstr();
        LOGGER().setLine(line);
        LOGGER().pushContext();
        if (annotated) {
            jp.printDirective(dir_signature,fn.signature);
        }
        annotator.printAnnotations(fn.visibleAnnotations, fn.invisibleAnnotations);
        annotator.printTypeAnnotations(fn.visibleTypeAnnotations, fn.invisibleTypeAnnotations);
        if (endrequired) {
            jp.decrDepth();
            jp.appendDirective(end_field).nl();
        }
        LOGGER().popContext();
    }

    private void printArray(List<String> strings) {
        jp.append(dot_array)
                .nl();
        jp.incrDepth();
        for (String mod:strings) {
            jp.append(mod).nl();
        }
        jp.decrDepth();
        jp.appendDirective(end_array).nl();
    }
    
    private void printModuleInfo(ModuleNode module) {
        EnumSet<AccessFlag> accflags;
        if (module.packages != null) {
            jp.appendDirective(dir_packages);
            printArray(module.packages);
        }
        for (String use:nonNullList(module.uses)) {
            jp.appendDirective(dir_uses).append(use).nl();
        }
        for (ModuleExportNode men: nonNullList(module.exports)) {
            accflags = AccessFlag.getEnumSet(men.access, MODULE,jvmVersion);
            jp.appendDirective(dir_exports)
                    .append(accflags, men.packaze);
            if(men.modules == null || men.modules.isEmpty()) {
                jp.nl();
            } else {
                jp.append(res_to);
                printArray(men.modules);
            }
        }
        for (ModuleOpenNode mon: nonNullList(module.opens)) {
            accflags = AccessFlag.getEnumSet(mon.access, MODULE,jvmVersion);
            jp.appendDirective(dir_opens)
                    .append(accflags, mon.packaze);
            if(mon.modules == null || mon.modules.isEmpty()) {
                jp.nl();
            } else {
                jp.append(res_to);
                printArray(mon.modules);
            }
        }
        for (ModuleRequireNode mrn: nonNullList(module.requires)) {
            accflags = AccessFlag.getEnumSet(mrn.access, REQUIRE,jvmVersion);
            jp.appendDirective(dir_requires)
                    .append(accflags, mrn.module)
                    .appendNonNull(mrn.version)
                    .nl();
        }
        for (ModuleProvideNode mpn: nonNullList(module.provides)) {
            jp.appendDirective(dir_provides)
                    .append(mpn.service)
                    .append(res_with);
            printArray(mpn.providers);
        }
    }

    private void printDotVersion() {
        jp.appendDirective(dir_version)
                .append(jvmVersion.asJava());
        jp.nl();
    }
    
    private void printVersionSource() {
        jp.appendComment("options = " + OPTIONS().toString()).nl();
        printDotVersion();
        jp.printDirective(dir_source, cn.sourceFile);
    }
    
    public static boolean a2jpw(PrintWriter pw, String fname) {
        Optional<ClassReader> optrdr = JynxClassReader.getClassReader(fname);
        if (optrdr.isPresent()) {
            try {
                JynxDisassemble a2j = JynxDisassemble.getInstance(optrdr.get(),pw);
                return a2j.print();
            } catch (Exception ex) {
                LOG(M237,ex); // "error accepting class file: %s"
                return false;
            }
        }
        return false;
    }

}

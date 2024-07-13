package asm2jynx;

import java.io.PrintWriter;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ModuleExportNode;
import org.objectweb.asm.tree.ModuleNode;
import org.objectweb.asm.tree.ModuleOpenNode;
import org.objectweb.asm.tree.ModuleProvideNode;
import org.objectweb.asm.tree.ModuleRequireNode;
import org.objectweb.asm.tree.RecordComponentNode;

import static asm2jynx.Util.*;
import static jvm.AccessFlag.*;
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
import jvm.StandardAttribute;
import jynx.ClassType;
import jynx.Directive;
import jynx.Global;
import jynx.GlobalOption;
import jynx.MainOption;
import jynx2asm.handles.HandlePart;

public class JynxDisassemble {

    private final ClassNode cn;

    private final PrintWriter pw;
    
    private final JvmVersion jvmVersion;
    private final Object2String o2s;
    private final JynxStringBuilder jp;
    private final PrintAnnotations annotator;
    private final JynxMethodPrinter jmp;

    private JynxDisassemble(ClassNode cn, PrintWriter pw, JvmVersion jvmversion) {
        this.pw = pw;
        this.o2s = new Object2String();
        this.jp = new JynxStringBuilder(pw::print);
        if (OPTION(GlobalOption.SKIP_ANNOTATIONS)) {
            this.annotator = new PrintAnnotations(new JynxStringBuilder(str->{}));
        } else {
            this.annotator = new PrintAnnotations(jp);
        }
        this.jvmVersion = jvmversion;
        this.cn = cn;
        this.jmp = JynxMethodPrinter.getInstance(cn.name, jvmVersion, jp, annotator);
    }

    private static JynxDisassemble getInstance(ClassReader cr, PrintWriter pw) {
        int poolsz = cr.getItemCount();
        if (poolsz >= 256) {
            LOG(M67,poolsz); // "poolsz = %d"
        }
        ClassNode cn = new ClassNode();
        int crflag = ClassReader.EXPAND_FRAMES;
        if (OPTION(GlobalOption.SKIP_CODE)) {
            crflag |= ClassReader.SKIP_CODE;
        }
        if (OPTION(GlobalOption.SKIP_DEBUG)) {
            crflag |= ClassReader.SKIP_DEBUG;
        }
        if (OPTION(GlobalOption.SKIP_FRAMES)) {
            crflag |= ClassReader.SKIP_FRAMES;
        }
        cr.accept(cn, crflag);
        JvmVersion jvmversion = JvmVersion.fromASM(cn.version);
        jvmversion.checkSupported();
        if (jvmversion == JvmVersion.V1_6JSR && hasFrames(cn)) {
            jvmversion = JvmVersion.V1_6;
        }
        Global.setJvmVersion(jvmversion);
        return new JynxDisassemble(cn, pw, jvmversion);
    }

    private static boolean hasFrames(ClassNode cn) {
        if (cn.methods == null || OPTION(GlobalOption.SKIP_FRAMES)) {
            return false;
        }
        return cn.methods.stream()
                .map(m->m.instructions)
                .filter(insnlist-> insnlist != null)
                .flatMap(insnlist->Stream.of(insnlist.toArray()))
                .filter(insn-> insn instanceof FrameNode)
                .findAny()
                .isPresent();
    }
    
    public void close() {
        jp.close();
        pw.close();
    }

    private void printPackage() {
        String cname = cn.name;
        EnumSet<AccessFlag> accflags = AccessFlag.getEnumSet(cn.access,CLASS,jvmVersion);
        ClassType classtype = ClassType.PACKAGE;
        int index = cname.lastIndexOf('/');
        cname = cname.substring(0, index);
        accflags.removeAll(classtype.getMustHave4Class(jvmVersion));
        Directive dir = classtype.getDir();
        jp.append(dir)
                .appendFlags(accflags)
                .appendName(cname)
                .nl()
                .incrDepth()
                .appendDir(dir_super, cn.superName);
        annotator.printAnnotations(cn.visibleAnnotations,cn.invisibleAnnotations);
        printInner();
        jp.decrDepth();
    }
    
    private boolean print() {
        Global.setClassName(cn.name);
        printVersionSource();
        if (cn.module != null) {
            LOGGER().setLine("module " + cn.name);
            LOGGER().pushContext();
            printModuleHeader();
        } else if (cn.name.endsWith("/" + Constants.PACKAGE_INFO_NAME.stringValue())) {
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
                cmdesc = HandlePart.ownerName(outerClass,cmdesc);
            }
            jp.append(dir_enclosing_method)
                    .append(cmdesc)
                    .nl();
            
        } else if (outerClass != null) {
            jp.append(dir_outer_class)
                    .append(outerClass)
                    .nl();
        }
    }

    private void printClassHeader() {
        String cname = cn.name;
        EnumSet<AccessFlag> accflags = AccessFlag.getEnumSet(cn.access,CLASS,jvmVersion);
        ClassType classtype = ClassType.from(accflags);
        accflags.removeAll(classtype.getMustHave4Class(jvmVersion));
        Directive dir = classtype.getDir();
        jp.append(dir)
                .appendFlags(accflags)
                .appendName(cname)
                .nl()
                .incrDepth()
                .appendDir(dir_super, cn.superName)
                .appendDirArray(dir_implements, cn.interfaces)
                .appendDir(dir_signature,cn.signature)
                .appendDir(dir_debug, cn.sourceDebug);
        printEnclosing(cn.outerClass,cn.outerMethod,cn.outerMethodDesc);
        jp.appendDir(dir_nesthost, cn.nestHostClass);
        annotator.printAnnotations(cn.visibleAnnotations,cn.invisibleAnnotations);
        annotator.printTypeAnnotations(cn.visibleTypeAnnotations, cn.invisibleTypeAnnotations);
        printInner();
        jp.appendDirArray(dir_nestmember,cn.nestMembers)
                .appendDirArray(dir_permittedSubclass,cn.permittedSubclasses)
                .decrDepth();
        printComponents();
    }

    private void printComponents() {
        List<RecordComponentNode> components = nonNullList(cn.recordComponents);
        for (RecordComponentNode rcn :components) {
            jp.append(dir_component)
                    .append(rcn.name)
                    .append(rcn.descriptor)
                    .nl();
            if (rcn.signature != null || isAnyPresent(
                    rcn.invisibleAnnotations,
                    rcn.visibleAnnotations,
                    rcn.visibleTypeAnnotations,
                    rcn.invisibleTypeAnnotations
                    )) {
                jp.incrDepth()
                        .appendDir(dir_signature, rcn.signature);
                    annotator.printAnnotations(rcn.visibleAnnotations,rcn.invisibleAnnotations);
                    annotator.printTypeAnnotations(rcn.visibleTypeAnnotations, rcn.invisibleTypeAnnotations);
                jp.decrDepth()
                        .append(end_component).nl();
            }
        }
    }

    private void printInner() {
        for (InnerClassNode icn : nonNullList(cn.innerClasses)) {
            EnumSet<AccessFlag> inneraccflags = AccessFlag.getEnumSet(icn.access,INNER_CLASS,jvmVersion);
            ClassType classtype = ClassType.from(inneraccflags);
            inneraccflags.removeAll(classtype.getMustHave4Inner(jvmVersion));
            Directive inner = classtype.getInnerDir();
            jp.append(inner)
                    .appendFlags(inneraccflags)
                    .appendName(icn.name)
                    .append(res_outer, icn.outerName)
                    .append(res_innername, icn.innerName)
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
            result.add(dir_outer_class);
        }
        if (cn.nestHostClass != null) result.add(dir_nesthost);
        if (isPresent(cn.nestMembers)) result.add(dir_nestmember);
        if (isPresent(cn.permittedSubclasses)) result.add(dir_permittedSubclass);
        return result;
    }
    
    private void printModuleHeader() {
        // specified at end of jvms 4.1
        assert Constants.MODULE_CLASS_NAME.equalsString(cn.name);
        assert getModuleAbsent().isEmpty();
        assert isAbsent(cn.invisibleTypeAnnotations);
        assert isAbsent(cn.visibleTypeAnnotations);
        EnumSet<AccessFlag>  accflags = AccessFlag.getEnumSet(cn.access, CLASS,jvmVersion);
        assert accflags.contains(acc_module) && accflags.size() == 1;
        
        jp.append(dir_define_module)
                .nl()
                .incrDepth()
                .appendDir(dir_debug, cn.sourceDebug);
        annotator.printAnnotations(cn.visibleAnnotations, cn.invisibleAnnotations);
        printInner();
        jp.decrDepth();
        printModuleInfo(cn.module);
    }

    private void printField(FieldNode fn) {
        jp.blankline();
        LOGGER().setLine("field " + fn.name);
        boolean annotated = isAnyPresent(
                fn.visibleAnnotations,
                fn.invisibleAnnotations,
                fn.visibleTypeAnnotations,
                fn.invisibleTypeAnnotations);
        boolean endrequired = annotated || fn.signature != null;

        EnumSet<AccessFlag> accflags = AccessFlag.getEnumSet(fn.access, FIELD,jvmVersion);
        jp.append(dir_field)
                .appendFlags(accflags)
                .appendName(fn.name)
                .appendNonNull(fn.desc);
        if (fn.value != null) {
            jvmVersion.checkSupports(StandardAttribute.ConstantValue);
            ConstType ct = ConstType.getFromDesc(fn.desc,FIELD);
            jp.append(equals_sign, o2s.stringFrom(ct,fn.value));
        }
        String line = jp.line();
        jp.nl();
        LOGGER().setLine(line);

        if (endrequired) {
            LOGGER().pushContext();
            jp.incrDepth()
                    .appendDir(dir_signature,fn.signature);
                annotator.printAnnotations(fn.visibleAnnotations, fn.invisibleAnnotations);
                annotator.printTypeAnnotations(fn.visibleTypeAnnotations, fn.invisibleTypeAnnotations);
            jp.decrDepth();
            jp.append(end_field).nl();
            LOGGER().popContext();
        }
    }

    private void printModuleInfo(ModuleNode module) {
        EnumSet<AccessFlag> accflags = AccessFlag.getEnumSet(module.access, MODULE,jvmVersion);
        jp.append(dir_module)
                .appendFlags(accflags)
                .appendName(module.name)
                .appendNonNull(module.version)
                .nl()
                .incrDepth();
        jp.appendDir(dir_main,module.mainClass);
        for (ModuleRequireNode mrn: nonNullList(module.requires)) {
            accflags = AccessFlag.getEnumSet(mrn.access, REQUIRE,jvmVersion);
            jp.append(dir_requires)
                    .appendFlags(accflags)
                    .appendName(mrn.module)
                    .appendNonNull(mrn.version)
                    .nl();
        }
        for (ModuleExportNode men: nonNullList(module.exports)) {
            accflags = AccessFlag.getEnumSet(men.access, MODULE,jvmVersion);
            jp.append(dir_exports)
                    .appendFlags(accflags)
                    .appendName(men.packaze)
                    .appendRWArray(res_to, men.modules);
        }
        for (ModuleOpenNode mon: nonNullList(module.opens)) {
            accflags = AccessFlag.getEnumSet(mon.access, MODULE,jvmVersion);
            jp.append(dir_opens)
                    .appendFlags(accflags)
                    .appendName(mon.packaze)
                    .appendRWArray(res_to, mon.modules);
        }
        for (String use:nonNullList(module.uses)) {
            jp.append(dir_uses).append(use).nl();
        }
        for (ModuleProvideNode mpn: nonNullList(module.provides)) {
            jp.append(dir_provides)
                    .append(mpn.service)
                    .appendRWArray(res_with, mpn.providers);
        }

        if (isPresent(module.packages)) {
            jp.appendDirArray(dir_packages, module.packages);
        }
        jp.decrDepth().append(end_module).nl();
    }

    private void printVersionSource() {
        // "options = %s"
        LOG(M88, OPTIONS());
        jp.appendComment("options = " + OPTIONS().toString())
                .nl()
                .comment()
                .append(MainOption.DISASSEMBLY.version())
                .nl()
                .append(dir_version)
                .append(jvmVersion.asJava());
        OPTIONS().stream()
                .filter(GlobalOption::isExternal)
                .filter(opt -> MainOption.ASSEMBLY.usesOption(opt))
                .filter(opt-> opt != GlobalOption.SYSIN)
                .forEach(jp::append);
        jp.nl()
                .appendDir(dir_source, cn.sourceFile);
    }
    
    public static boolean a2jpw(PrintWriter pw, String fname) {
        Optional<ClassReader> optrdr = JynxClassReader.getClassReader(fname);
        if (optrdr.isPresent()) {
            try {
                JynxDisassemble a2j = JynxDisassemble.getInstance(optrdr.get(),pw);
                return a2j.print();
            } catch (Exception ex) {
                LOG(ex);
                LOG(M237,fname); // "error accepting class file: %s"
                return false;
            }
        }
        return false;
    }

}

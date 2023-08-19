package textifier;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;

import static jvm.Context.CLASS;
import static jvm.Context.FIELD;
import static jvm.Context.INNER_CLASS;
import static jynx.Directive.dir_enclosing_method;
import static jynx.Directive.dir_field;
import static jynx.Directive.dir_implements;
import static jynx.Directive.dir_outer_class;
import static jynx.Directive.dir_signature;
import static jynx.Directive.dir_super;
import static jynx.Directive.dir_version;
import static jynx.Directive.end_field;
import static jynx.Global.OPTION;
import static jynx.ReservedWord.equals_sign;
import static jynx.ReservedWord.res_innername;
import static jynx.ReservedWord.res_outer;

import asm.JynxClassReader;
import asm2jynx.JynxStringBuilder;
import asm2jynx.Object2String;
import jvm.AccessFlag;
import jvm.Constants;
import jvm.ConstType;
import jvm.JvmVersion;
import jvm.StandardAttribute;
import jynx.ClassType;
import jynx.Directive;
import jynx.Global;
import jynx.GlobalOption;
import jynx2asm.handles.HandlePart;

public class JynxText extends Textifier {

    private final Object2String o2s;
    private final JynxStringBuilder jsb;
    private final List<String> packages;

    private JvmVersion jvmVersion;
    private boolean endRequired;

    public JynxText() {
        this(JvmVersion.DEFAULT_VERSION, false);
    }

    protected JynxText(JvmVersion jvmVersion, boolean endRequired) {
        super(Opcodes.ASM9);
        this.o2s = new Object2String();
        this.jsb = new JynxStringBuilder(text::add);
        this.jvmVersion = jvmVersion;
        this.endRequired = endRequired;
        this.packages = new ArrayList<>();
    }

    public static void jynxify(final byte[] ba, final PrintWriter pw) {
        ClassReader cr = JynxClassReader.getClassReader(ba);
        Printer printer = new JynxText();
        TraceClassVisitor tcv = new TraceClassVisitor(null, printer, pw);
        cr.accept(tcv, 0);
    }

    // textifier for method
    @Override
    protected Textifier createTextifier() {
        return new JynxText(jvmVersion, false);
    }

    private Textifier createTextifier(boolean endrequired) {
        return new JynxText(jvmVersion, endrequired);
    }

    @Override
    public void visit(
            final int version,
            final int access,
            final String name,
            final String signature,
            final String superName,
            final String[] interfaces) {
        JvmVersion jvmversion = JvmVersion.fromASM(version);
        jvmversion.checkSupported();
        if (jvmversion == JvmVersion.V1_6JSR && !OPTION(GlobalOption.SKIP_FRAMES)) {
            jvmversion = JvmVersion.V1_6;
        }
        jvmVersion = jvmversion;
        Global.setJvmVersion(jvmversion);

        EnumSet<AccessFlag> accflags = AccessFlag.getEnumSet(access, CLASS, jvmVersion);
        ClassType classtype = ClassType.from(accflags);
        accflags.removeAll(classtype.getMustHave4Class(jvmVersion));
        Directive dir = classtype.getDir();
        jsb.start(0)
                .append(dir_version)
                .append(jvmversion.asJava())
                .nl()
                .appendDir(Directive.dir_macrolib, "ASMTextOps")
                .append(dir);
        if (classtype == ClassType.MODULE_CLASS) {
            assert Constants.MODULE_CLASS_NAME.equalsString(name);
        } else {
            jsb.appendFlags(accflags)
                .append(name);
        }
        jsb.nl()
                .incrDepth()
                .appendDir(dir_super, superName)
                .appendDirArray(dir_implements, interfaces)
                .appendDir(dir_signature, signature);
    }

    @Override
    public Printer visitRecordComponent(String name, String descriptor, String signature) {
        jsb.start(2)
                .append(Directive.dir_component)
                .append(name)
                .append(descriptor)
                .nl()
                .incrDepth()
                .appendDir(dir_signature, signature);
        endRequired = signature != null;
        return this;
    }

    @Override
    public Textifier visitRecordComponentAnnotation(String descriptor, boolean visible) {
        endRequired = true;
        return visitAnnotation(descriptor, visible);
    }

    @Override
    public void visitRecordComponentEnd() {
        if (endRequired) {
            jsb.start(2)
                    .append(Directive.end_component)
                    .nl();
        }
    }

    @Override
    public Printer visitRecordComponentTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
        endRequired = true;
        return visitTypeAnnotation(typeRef, typePath, descriptor, visible);
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        EnumSet<AccessFlag> inneraccflags = AccessFlag.getEnumSet(access, INNER_CLASS, jvmVersion);
        ClassType classtype = ClassType.from(inneraccflags);
        inneraccflags.removeAll(classtype.getMustHave4Inner(jvmVersion));
        Directive inner = classtype.getInnerDir();
        jsb.start(1)
                .append(inner)
                .appendFlags(inneraccflags)
                .append(name)
                .append(res_outer, outerName)
                .append(res_innername, innerName)
                .nl();
    }

    private void addDirective(Directive dir, Object value) {
        if (value != null) {
            jsb.start(1)
                    .appendDir(dir, value);
        }
    }

    @Override
    public void visitNestHost(String nestHost) {
        addDirective(Directive.dir_nesthost, nestHost);
    }

    @Override
    public void visitNestMember(String nestMember) {
        addDirective(Directive.dir_nestmember, nestMember);
    }

    @Override
    public void visitOuterClass(final String owner, final String name, final String descriptor) {
        if (name != null || descriptor != null) {
            String cmdesc = descriptor == null ? name : name + descriptor;
            if (owner != null) {
                cmdesc = HandlePart.ownerName(owner, cmdesc);
            }
            addDirective(dir_enclosing_method, cmdesc);
        } else if (owner != null) {
            addDirective(dir_outer_class, owner);
        }
    }

    @Override
    public void visitPermittedSubclass(String permittedSubclass) {
        addDirective(Directive.dir_permittedSubclass, permittedSubclass);
    }

    @Override
    public void visitClassEnd() {
        jsb.close();
    }

    @Override
    public Textifier visitField(
            final int access,
            final String name,
            final String descriptor,
            final String signature,
            final Object value) {

        EnumSet<AccessFlag> accflags = AccessFlag.getEnumSet(access, FIELD, jvmVersion);
        jsb.start(0)
                .blankline()
                .append(dir_field)
                .appendFlags(accflags)
                .append(name)
                .append(descriptor);
        if (value != null) {
            jvmVersion.checkSupports(StandardAttribute.ConstantValue);
            ConstType ct = ConstType.getFromDesc(descriptor, FIELD);
            jsb.append(equals_sign)
                    .append(o2s.stringFrom(ct, value));
        }
        jsb.nl()
                .incrDepth()
                .appendDir(dir_signature, signature);
        endRequired = signature != null;

        Textifier textifier = createTextifier(true);
        text.add(textifier.getText());
        return textifier;
    }

    @Override
    public void visitFieldEnd() {
        if (endRequired) {
            jsb.start(0)
                    .append(end_field)
                    .nl();
        }
    }

    @Override
    public Textifier visitMethod(
            final int access,
            final String name,
            final String descriptor,
            final String signature,
            final String[] exceptions) {

        Textifier textifier = new JynxTextMethod(jvmVersion, true);
        text.add(textifier.getText());
        return textifier.visitMethod(access, name, descriptor, signature, exceptions);
    }

    // skip-debug: skipped directives
    @Override
    public void visitSource(String file, String debug) {
        jsb.start(1)
                .appendDir(Directive.dir_source, file)
                .appendDir(Directive.dir_debug, debug);
    }

    // annotations
    @Override
    public Textifier visitAnnotation(String descriptor, boolean visible) {
        Textifier textifier = new JynxTextAnnotation(jvmVersion);
        text.add(textifier.getText());
        return textifier.visitAnnotation(descriptor, visible);
    }

    @Override
    public Textifier visitAnnotation(String name, String descriptor) {
        Textifier textifier = new JynxTextAnnotation(jvmVersion);
        text.add(textifier.getText());
        return textifier.visitAnnotation(name, descriptor);
    }

    @Override
    public Textifier visitArray(final String name) {
        // TODO
        Textifier textifier = new JynxTextAnnotation(jvmVersion);
        text.add(textifier.getText());
        return textifier.visitArray(name);
    }

    @Override
    public Textifier visitFieldAnnotation(String descriptor, boolean visible) {
        endRequired = true;
        return super.visitFieldAnnotation(descriptor, visible); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void visitAnnotationEnd() {
        jsb.start(1)
                .append(Directive.end_annotation)
                .nl();
    }

    // type annotations
    @Override
    public Textifier visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
        Textifier textifier = new JynxTextAnnotation(jvmVersion);
        text.add(textifier.getText());
        return textifier.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
    }

    @Override
    public Printer visitFieldTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
        endRequired = true;
        return super.visitFieldTypeAnnotation(typeRef, typePath, descriptor, visible); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void visitAttribute(Attribute attribute) {
        text.add(".comment\n");
        super.visitAttribute(attribute);
        text.add(".end_comment\n");
    }

    @Override
    public Printer visitModule(String name, int access, String version) {
        JynxTextModule jmt = new JynxTextModule(jvmVersion, jsb);
        jmt.visitModule(name, access, version);
        return jmt;
    }

}

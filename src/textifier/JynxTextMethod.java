package textifier;

import java.util.Arrays;
import java.util.EnumSet;

import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;

import static jvm.Context.METHOD;
import static jvm.Context.PARAMETER;
import static jynx.Directive.dir_catch;
import static jynx.Directive.dir_method;
import static jynx.Directive.dir_parameter;
import static jynx.Directive.dir_signature;
import static jynx.Directive.dir_throws;
import static jynx.Directive.dir_var;
import static jynx.Directive.end_method;
import static jynx.ReservedWord.dot_array;
import static jynx.ReservedWord.res_all;
import static jynx.ReservedWord.res_default;
import static jynx.ReservedWord.res_from;
import static jynx.ReservedWord.res_is;
import static jynx.ReservedWord.res_signature;
import static jynx.ReservedWord.res_to;
import static jynx.ReservedWord.res_typepath;
import static jynx.ReservedWord.res_using;
import static jynx.ReservedWord.right_arrow;

import asm2jynx.FrameTypeValue;
import asm2jynx.JynxStringBuilder;
import asm2jynx.Object2String;
import jvm.AccessFlag;
import jvm.JvmVersion;
import jvm.TypeRef;
import jynx.Directive;
import jynx.ReservedWord;
import jynx2asm.handles.HandlePart;
import jynx2asm.ops.JvmOp;

public class JynxTextMethod extends JynxText {

    private final Object2String o2s;
    private final JynxStringBuilder jsb;

    private JvmVersion jvmVersion;
    private boolean endRequired;
    private int parmnum;

    public JynxTextMethod() {
        this(JvmVersion.DEFAULT_VERSION, false);
    }

    public JynxTextMethod(JvmVersion jvmVersion, boolean endRequired) {
        super();
        this.o2s = new Object2String();
        this.jsb = new JynxStringBuilder(text::add, this::getLabelName);
        this.jvmVersion = jvmVersion;
        this.endRequired = endRequired;
        this.parmnum = 0;
    }

    // textifier for method
    @Override
    protected Textifier createTextifier() {
        return new JynxTextMethod(jvmVersion, false);
    }

    private String getLabelName(Label label) {
        stringBuilder.setLength(0);
        appendLabel(label);
        String labname = stringBuilder.toString();
        stringBuilder.setLength(0);
        return labname;
    }

    public String getLabelName(LabelNode labnode) {
        Label label = labnode.getLabel();
        return getLabelName(label);
    }

    // so same order as Textifier
    private void defineLabels(final Label dflt, final Label[] labels) {
        stringBuilder.setLength(0);
        for (int i = 0; i < labels.length; ++i) {
            appendLabel(labels[i]);
        }
        appendLabel(dflt);
    }

    @Override
    public Textifier visitMethod(
            final int access,
            final String name,
            final String descriptor,
            final String signature,
            final String[] exceptions) {

        EnumSet<AccessFlag> accflags = AccessFlag.getEnumSet(access, METHOD, jvmVersion);
        jsb.start(0)
                .blankline()
                .incrDepth()
                .append(dir_method)
                .appendFlags(accflags)
                .append(name + descriptor)
                .nl()
                .appendDir(dir_signature, signature)
                .appendDir(dir_throws, exceptions);
        return this;
    }

    @Override
    public Textifier visitAnnotableParameterCount(int parameterCount, boolean visible) {
        Directive dir = visible ? Directive.dir_visible_parameter_count : Directive.dir_invisible_parameter_count;
        jsb.start(1)
                .appendDir(dir, parameterCount);
        return this;
    }

    @Override
    public Textifier visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
        Textifier textifier = new JynxTextAnnotation(jvmVersion);
        text.add(textifier.getText());
        return textifier.visitParameterAnnotation(parameter, descriptor, visible);
    }

    @Override
    public void visitParameter(String name, int access) {
        EnumSet<AccessFlag> pnaccflags = AccessFlag.getEnumSet(access, PARAMETER, jvmVersion);
        jsb.start(1)
                .append(dir_parameter)
                .append(parmnum)
                .appendFlags(pnaccflags)
                .appendName(name)
                .nl();
        ++parmnum;
    }

    @Override
    public Textifier visitMethodAnnotation(String descriptor, boolean visible) {
        Textifier textifier = new JynxTextAnnotation(jvmVersion);
        text.add(textifier.getText());
        return textifier.visitAnnotation(descriptor, visible);
    }

    @Override
    public Textifier visitAnnotationDefault() {
        Textifier textifier = new JynxTextAnnotation(jvmVersion);
        text.add(textifier.getText());
        return textifier.visitAnnotationDefault();
    }

    @Override
    public void visitMethodEnd() {
        jsb.start(1)
                .append(end_method)
                .nl();
    }

    @Override
    public void visitTryCatchBlock(
            final Label start, final Label end, final Label handler, final String type) {
        String exception = type == null ? res_all.toString() : type;
        jsb.start(2)
                .append(dir_catch)
                .append(exception)
                .append(res_from)
                .appendLabel(start)
                .append(res_to)
                .appendLabel(end)
                .append(res_using)
                .appendLabel(handler)
                .nl();
    }

    @Override
    public void visitInvokeDynamicInsn(
            final String name,
            final String descriptor,
            final Handle bootstrapMethodHandle,
            final Object... bootstrapMethodArguments) {
        ConstantDynamic cd = new ConstantDynamic(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
        jsb.start(2)
                .append(JvmOp.asm_invokedynamic)
                .append(o2s.constDynamic2String(cd))
                .nl();
    }

    @Override
    public void visitLdcInsn(final Object value) {
        String cststr = o2s.asm2String(value);
        jsb.start(2)
                .append("LDC")
                .append(cststr)
                .nl();
    }

    @Override
    public void visitLookupSwitchInsn(final Label dflt, final int[] keys, final Label[] labels) {
        defineLabels(dflt, labels);
        jsb.start(2)
                .append(JvmOp.asm_lookupswitch)
                .append(res_default)
                .appendLabel(dflt)
                .append(dot_array)
                .nl()
                .incrDepth();
        for (int i = 0; i < labels.length; ++i) {
            jsb.append(keys[i])
                    .append(right_arrow)
                    .appendLabel(labels[i])
                    .nl();
        }
        jsb.decrDepth()
                .append(Directive.end_array)
                .nl();
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        String ownerx = isInterface ? HandlePart.INTERFACE_PREFIX + owner : owner;
        super.visitMethodInsn(opcode, ownerx, name, descriptor, false);
    }

    @Override
    public void visitTableSwitchInsn(
            final int min, final int max, final Label dflt, final Label... labels) {

        defineLabels(dflt, labels);
        jsb.start(2)
                .append(JvmOp.asm_tableswitch)
                .append(min)
                .append(res_default)
                .appendLabel(dflt)
                .append(dot_array)
                .nl()
                .incrDepth();
        for (int i = 0; i < labels.length; ++i) {
            jsb.appendLabel(labels[i])
                    .comment()
                    .append(min + i)
                    .nl();
        }
        jsb.decrDepth()
                .append(Directive.end_array)
                .nl();
    }

    @Override
    public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
        assert type == Opcodes.F_NEW;
        FrameTypeValue[] locals = FrameTypeValue.from(Arrays.stream(local, 0, numLocal), this::getLabelName);
        FrameTypeValue[] stacks = FrameTypeValue.from(Arrays.stream(stack, 0, numStack), this::getLabelName);
        jsb.start(2)
                .append(Directive.dir_stack)
                .nl()
                .incrDepth();
        for (FrameTypeValue ftv : locals) {
            jsb.append(ReservedWord.res_locals)
                    .append(ftv.ft())
                    .appendNonNull(ftv.value())
                    .nl();
        }
        for (FrameTypeValue ftv : stacks) {
            jsb.append(ReservedWord.res_stack)
                    .append(ftv.ft())
                    .appendNonNull(ftv.value())
                    .nl();
        }
        jsb.decrDepth()
                .append(Directive.end_stack)
                .nl();
    }

    // skip-debug: skipped directives
    @Override
    public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
        jsb.start(2)
                .append(dir_var)
                .append(index)
                .append(res_is)
                .appendName(name)
                .append(descriptor)
                .append(res_signature, signature)
                .append(res_from, start)
                .append(res_to, end)
                .nl();
    }

    @Override
    public Textifier visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
        Textifier textifier = new JynxTextAnnotation(jvmVersion);
        text.add(textifier.getText());
        return textifier.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
    }

    @Override
    public Printer visitTryCatchAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
        return visitTypeAnnotation(typeRef, typePath, descriptor, visible);
    }

    @Override
    public Printer visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index, String descriptor, boolean visible) {
        TypeRef tr = TypeRef.getInstance(typeRef);
        Directive dir = tr.getDirective();
        String typepath = typePath == null ? null : typePath.toString();
        String trstr = tr.getTypeRefString(typeRef);
        ReservedWord visibility = visible ? ReservedWord.res_visible : ReservedWord.res_invisible;
        jsb.start(2)
                .append(dir)
                .append(visibility);
        if (!trstr.isEmpty()) {
            jsb.append(trstr.split(" "));
        }
        jsb.append(res_typepath, typepath)
                .append(descriptor)
                .append(ReservedWord.dot_array)
                .nl()
                .incrDepth();
        int entries = index.length;
        assert entries == start.length && entries == end.length;
        for (int i = 0; i < entries; ++i) {
            Label startlab = start[i];
            Label endlab = end[i];
            jsb.append(index[i])
                    .appendLabel(startlab)
                    .appendLabel(endlab)
                    .nl();
        }
        jsb.decrDepth()
                .append(Directive.end_array)
                .nl();
        Textifier textifier = new JynxTextAnnotation(jvmVersion);
        text.add(textifier.getText());
        return textifier;
    }

}

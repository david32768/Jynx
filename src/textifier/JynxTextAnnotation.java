package textifier;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.util.Textifier;

import static jynx.Directive.dir_parameter_annotation;
import static jynx.ReservedWord.res_typepath;

import asm2jynx.JynxStringBuilder;
import asm2jynx.Object2String;
import jvm.ConstType;
import jvm.Context;
import jvm.JvmVersion;
import jvm.TypeRef;
import jynx.Directive;
import jynx.ReservedWord;

public class JynxTextAnnotation extends Textifier {

    private final Object2String o2s;
    private final JynxStringBuilder jsb;

    private final JvmVersion jvmVersion;

    public JynxTextAnnotation() {
        this(JvmVersion.DEFAULT_VERSION);
    }

    public JynxTextAnnotation(JvmVersion jvmVersion) {
        super(Opcodes.ASM9);
        this.o2s = new Object2String();
        this.jsb = new JynxStringBuilder(text::add);
        this.jvmVersion = jvmVersion;
    }

    // textifier for method
    @Override
    protected Textifier createTextifier() {
        return new JynxTextAnnotation(jvmVersion);
    }

    // annotations
    @Override
    public Textifier visitAnnotation(String descriptor, boolean visible) {
        ReservedWord visibility = visible ? ReservedWord.res_visible : ReservedWord.res_invisible;
        jsb.start(1)
                .append(Directive.dir_annotation)
                .append(visibility)
                .append(descriptor)
                .nl();
        return this;
    }

    private void visitPrimitiveArray(final String name, final Object objs) {
        Textifier textifier = visitArray(name);
        if (objs instanceof boolean[]) {
            for (boolean x : (boolean[]) objs) {
                textifier.visit(null, x);
            }
        } else if (objs instanceof byte[]) {
            for (byte x : (byte[]) objs) {
                textifier.visit(null, x);
            }
        } else if (objs instanceof char[]) {
            for (char x : (char[]) objs) {
                textifier.visit(null, x);
            }
        } else if (objs instanceof short[]) {
            for (short x : (short[]) objs) {
                textifier.visit(null, x);
            }
        } else if (objs instanceof int[]) {
            for (int x : (int[]) objs) {
                textifier.visit(null, x);
            }
        } else if (objs instanceof long[]) {
            for (long x : (long[]) objs) {
                textifier.visit(null, x);
            }
        } else if (objs instanceof float[]) {
            for (float x : (float[]) objs) {
                textifier.visit(null, x);
            }
        } else if (objs instanceof double[]) {
            for (double x : (double[]) objs) {
                textifier.visit(null, x);
            }
        } else {
            throw new AssertionError();
        }
        textifier.visitAnnotationEnd();
    }

    @Override
    public void visit(final String name, final Object value) {
        Class<?> klass = value.getClass();
        if (klass.isArray()) {
            visitPrimitiveArray(name, value);
            return;
        }
        ConstType ct = ConstType.getFromASM(value, Context.ANNOTATION);
        String typestr = ct.getJynxDesc(false);
        String strvalue = o2s.stringFrom(ct, value);
        assert strvalue != null;
        jsb.start(2)
                .appendNonNull(name)
                .append(typestr)
                .append(ReservedWord.equals_sign)
                .append(strvalue)
                .nl();
    }

    @Override
    public void visitEnum(final String name, final String descriptor, final String value) {
        ConstType ct = ConstType.ct_enum;
        String typestr = ct.getJynxDesc(false);
        String strvalue = o2s.stringFrom(ConstType.ct_enum, value);
        assert strvalue != null;
        jsb.start(2)
                .appendNonNull(name)
                .append(typestr)
                .appendNonNull(descriptor)
                .append(ReservedWord.equals_sign)
                .append(strvalue)
                .nl();
    }

    @Override
    public Textifier visitAnnotation(String name, String descriptor) {
        ConstType ct = ConstType.ct_annotation;
        String typestr = ct.getJynxDesc(false);
        jsb.start(2)
                .appendNonNull(name)
                .append(typestr)
                .appendNonNull(descriptor)
                .append(ReservedWord.equals_sign)
                .append(ReservedWord.dot_annotation)
                .nl();
        return this;
    }

    @Override
    public Textifier visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
        ReservedWord visibility = visible ? ReservedWord.res_visible : ReservedWord.res_invisible;
        jsb.start(0)
                .append(dir_parameter_annotation)
                .append(visibility)
                .append(parameter)
                .append(descriptor)
                .nl();
        return this;
    }

    @Override
    public Textifier visitArray(final String name) {
        Textifier textifier = new JynxTextAnnotationArray(jvmVersion, name);
        text.add(textifier.getText());
        return textifier;
    }

    @Override
    public Textifier visitAnnotationDefault() {
        jsb.start(1)
                .append(Directive.dir_default_annotation)
                .nl();

        return this;
    }

    @Override
    public void visitAnnotationEnd() {
        jsb.start(1)
                .append(Directive.end_annotation)
                .nl();
        jsb.close();
    }

    @Override
    public Textifier visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
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
                .nl();
        return this;
    }

}

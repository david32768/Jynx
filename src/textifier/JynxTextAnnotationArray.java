package textifier;

import org.objectweb.asm.util.Textifier;

import asm2jynx.JynxStringBuilder;
import asm2jynx.Object2String;
import jvm.ConstType;
import jvm.Context;
import jvm.JvmVersion;
import jynx.Directive;
import jynx.ReservedWord;

public class JynxTextAnnotationArray extends JynxTextAnnotation {

    private final Object2String o2s;
    private final JynxStringBuilder jsb;
    private final String name;

    private final JvmVersion jvmVersion;
    private int valct;
    private ConstType ct;

    public JynxTextAnnotationArray(JvmVersion jvmVersion, String name) {
        super(jvmVersion);
        this.o2s = new Object2String();
        this.jsb = new JynxStringBuilder(text::add);
        this.jvmVersion = jvmVersion;
        this.name = name;
        this.ct = null;
    }

    @Override
    public void visit(final String nullname, final Object value) {
        assert nullname == null;
        jsb.start(2);
        if (valct++ == 0) {
            ct = ConstType.getFromASM(value, Context.ANNOTATION);
            String typestr = ct.getJynx_desc(true);
            jsb.appendNonNull(name)
                    .append(typestr)
                    .append(ReservedWord.equals_sign)
                    .append(ReservedWord.dot_array)
                    .nl();
        }
        String strvalue = o2s.stringFrom(ct, value);
        assert strvalue != null;
        jsb.incrDepth()
                .append(strvalue)
                .nl();
    }

    @Override
    public void visitEnum(final String nullname, final String descriptor, final String value) {
        assert nullname == null;
        jsb.start(2);
        if (valct++ == 0) {
            ct = ConstType.ct_enum;
            String typestr = ct.getJynx_desc(true);
            jsb.appendNonNull(name)
                    .append(typestr)
                    .appendNonNull(descriptor)
                    .append(ReservedWord.equals_sign)
                    .append(ReservedWord.dot_array)
                    .nl();
        }
        String strvalue = o2s.stringFrom(ConstType.ct_enum, value);
        assert strvalue != null;
        jsb.incrDepth()
                .append(strvalue)
                .nl();
    }

    @Override
    public Textifier visitAnnotation(String nullname, String descriptor) {
        ct = ConstType.ct_annotation;
        String typestr = ct.getJynx_desc(true);
        jsb.start(2)
                .appendNonNull(name)
                .append(typestr)
                .appendNonNull(descriptor)
                .append(ReservedWord.equals_sign)
                .append(ReservedWord.dot_annotation_array)
                .nl();
        return this;
    }

    @Override
    public void visitAnnotationEnd() {
        Directive dir;
        jsb.start(2)
                .incrDepth();
        if (ct == ConstType.ct_annotation) {
            if (valct == 0) {
                jsb.append(Directive.dir_annotation)
                        .nl()
                        .append(Directive.end_annotation)
                        .nl();
                ++valct;
            }
            if (valct > 0) {
                valct = -1;
                return;
            }
            dir = Directive.end_annotation_array;
        } else {
            if (valct == 0) {
                ct = ConstType.ct_int;
                String typestr = ct.getJynx_desc(true);
                jsb.appendNonNull(name)
                        .append(typestr)
                        .append(ReservedWord.equals_sign)
                        .append(ReservedWord.dot_array)
                        .nl();
            }
            dir = Directive.end_array;
        }
        jsb.decrDepth()
                .append(dir)
                .nl();
        jsb.close();
    }

}

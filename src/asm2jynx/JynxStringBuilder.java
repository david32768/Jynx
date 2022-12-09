package asm2jynx;

import java.util.EnumSet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.List;

import org.objectweb.asm.Label;
import org.objectweb.asm.tree.LabelNode;

import static jynx.Directive.end_array;
import static jynx.Global.LOG;
import static jynx.Message.M142;
import static jynx.Message.M81;
import static jynx.Message.M910;
import static jynx.ReservedWord.dot_array;

import jvm.AccessFlag;
import jynx.Directive;
import jynx.LogAssertionError;
import jynx.LogIllegalStateException;
import jynx.ReservedWord;
import jynx.StringUtil;
import jynx2asm.ops.IndentType;

public class JynxStringBuilder {

    public static final char TOKEN_SEPARATOR = ' ';

    protected final StringBuilder sb;
    private final Consumer<String> consumer;
    private Function<LabelNode, String> labelNamer;

    private int depth;

    public JynxStringBuilder(Consumer<String> consumer) {
        this(consumer,null);
    }
        
    public JynxStringBuilder(Consumer<String> consumer, Function<LabelNode, String> labelnamer) {
        this.sb = new StringBuilder();
        this.consumer = consumer;
        this.labelNamer = labelnamer;
    }

    public void setLabelNamer(Function<LabelNode, String> labelNamer) {
        this.labelNamer = labelNamer;
    }

    private boolean isStartOfLine() {
        int length = sb.length();
        return length == 0 || sb.charAt(length - 1) == '\n';
    }
    
    private void sep() {
        if (isStartOfLine()) {
            int indent = IndentType.BEGIN.after() * depth;
            for (int i = 0; i < indent;++i) {
                sb.append(TOKEN_SEPARATOR);
            }
        } else if (sb.charAt(sb.length() - 1) != ' ') {
            sb.append(TOKEN_SEPARATOR);
        }
    }

    public JynxStringBuilder start(int level) {
        sb.setLength(0);
        this.depth = level;
        return this;
    }

    public JynxStringBuilder incrDepth() {
        depth += 1;
        return this;
    }
    
    public JynxStringBuilder decrDepth() {
        depth -= 1;
        if (depth < 0) {
            throw new LogAssertionError(M910); // "indent depth is now negative"
        }
        return this;
    }
    
    public JynxStringBuilder append(Object object) {
        sep();
        sb.append(StringUtil.visible(object.toString()));
        return this;
    }

    public JynxStringBuilder append(Object[] objects) {
        for (Object obj : objects) {
            append(obj);
        }
        return this;
    }

    public JynxStringBuilder appendQuoted(String str) {
        return append(StringUtil.QuoteEscape(str));
    }
    
    public JynxStringBuilder appendNonNull(Object object) {
        if (object == null) {
            return this;
        }
        return append(object);
    }

    public JynxStringBuilder appendLabel(Label label) {
        return appendLabelNode(new LabelNode(label));
    }

    public JynxStringBuilder appendLabelNode(LabelNode labelnode) {
        return append(labelNamer.apply(labelnode));
    }

    public JynxStringBuilder appendDir(Directive dir, Object value) {
        if (!isStartOfLine()) {
            // "Directive must be first token but line = %s"
            throw new LogIllegalStateException(M81,sb);
        }
        if (value != null) {
            if (dir.hasQuotedArg()) {
                append(dir)
                        .appendQuoted(value.toString())
                        .nl();
            } else {
                append(dir)
                        .append(value)
                        .nl();
            }
        }
        return this;
    }

    private String stringify(ReservedWord rw, Object obj) {
        String string = obj.toString();
        switch(rw.rwtype()) {
            case NAME:
                return StringUtil.escapeName(string);
            case QUOTED:
                return StringUtil.QuoteEscape(string);
            case LABEL:
                if (obj instanceof Label) {
                    return labelNamer.apply(new LabelNode((Label)obj));
                } else if (obj instanceof LabelNode) {
                    return labelNamer.apply((LabelNode)obj);
                } else {
                    throw new AssertionError();
                }
            case TOKEN:
                return StringUtil.visible(string);
            default:
                throw new EnumConstantNotPresentException(rw.rwtype().getClass(), rw.rwtype().name());
        }
    }
    
    public JynxStringBuilder append(ReservedWord res, Object value) {
        if (value != null) {
            append(res).append(stringify(res,value));
        }
        return this;
    }

    public JynxStringBuilder appendName(String name) {
        return append(StringUtil.escapeName(name));
    }

    public JynxStringBuilder appendDir(Directive dir, Object[] values) {
        if (values != null) {
            for (int i = 0; i < values.length; ++i) {
                append(dir).append(values[i]).nl();
            }
        }
        return this;
    }

    public JynxStringBuilder appendDirs(Directive dir, List<String> strs) {
        if (strs != null) {
            for (Object str:strs) {
                appendDir(dir,str);
            }
        }
        return this;
    }
    
    
    public JynxStringBuilder appendFlags(EnumSet<AccessFlag> flags) {
        flags.stream().forEach(this::append);
        return this;
    }

    public JynxStringBuilder nl() {
        if (!isStartOfLine()) {
            sb.append('\n');
            consumer.accept(sb.toString());
            sb.setLength(0);
        }
        return this;
    }

    public String line() {
        return  toString();
    }

    public JynxStringBuilder comment() {
        sep();
        sb.append(';');
        return this;
    }

    public JynxStringBuilder appendComment(Object obj) {
        append("; ");
        String str = obj.toString();
        if (!StringUtil.isPrintableAscii(str)) {
            str = StringUtil.highlightUnprintablesAscii(str);
            LOG(M142,str); // "comment contains unprintable characters (replaced by '?'); comment = '%s'"
        }
        append(str);
        return this;
    }

    public JynxStringBuilder appendNonNull(String str) {
        if (str != null) {
            append(str);
        }
        return this;
    }

    public JynxStringBuilder appendRaw(String str) {
        append(StringUtil.printable(str));
        return this;
    }

    public JynxStringBuilder blankline() {
        sb.append('\n');
        consumer.accept(sb.toString());
        sb.setLength(0);
        return this;
    }
    
    public JynxStringBuilder  appendDotArray(List<String> strings) {
        return appendDotArray(strings.toArray(new String[0]));
    }
    
    public JynxStringBuilder  appendDotArray(String[] strings) {
        append(dot_array)
                .nl()
                .incrDepth();
        for (String mod:strings) {
            append(mod).nl();
        }
        decrDepth()
                .append(end_array)
                .nl();
        return this;
    }
    
    public void close() {
        if (sb.length() != 0) {
            String msg = "extraneous text: " + sb.toString();
            throw new AssertionError(msg);
        }
    }
}

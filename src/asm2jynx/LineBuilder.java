package asm2jynx;

import java.io.PrintWriter;
import java.util.EnumSet;

import static jynx.Global.*;
import static jynx.Message.M142;
import static jynx.Message.M81;
import static jynx.Message.M910;

import jvm.AccessFlag;
import jynx.Directive;
import jynx.LogAssertionError;
import jynx.LogIllegalStateException;
import jynx.ReservedWord;
import jynx.StringUtil;
import jynx2asm.Line;

public class LineBuilder {

    private final StringBuilder sb;
    private final PrintWriter pw;

    private int depth;
    
    public LineBuilder(PrintWriter pw) {
        this.sb = new StringBuilder();
        this.pw = pw;
        this.depth = 0;
    }
    
    public void incrDepth() {
        depth += 1;
    }
    
    public void decrDepth() {
        depth -= 1;
        if (depth < 0) {
            throw new LogAssertionError(M910); // "indent depth is now negative"
        }
    }
    
    public boolean isEmpty() {
        return sb.length() == 0;
    }
    
    public void clear() {
        sb.setLength(0);
    }
    
    public String nlstr() {
        String line = toString();
        nl();
        return line;
    }

    public void nl() {
        if (!isEmpty()) {
            pw.println(this);
            pw.flush();
            clear();
        }
    }

    public LineBuilder appendBlank() {
        sb.append(' ');
        return this;
    }
    
    private void addSep() {
        if (isEmpty()) {
            for (int i = 0; i < depth;++i) {
                sb.append("  ");
            }
        } else if (sb.charAt(sb.length() -1) != ' ') {
            sb.append(' ');
        }
    }
    
    public LineBuilder append(String str) {
        addSep();
        sb.append(StringUtil.visible(str));
        return this;
    }
    
    public LineBuilder append(String[] strarr) {
        for (String str:strarr) {
            append(str);
        }
        return this;
    }
    
    public LineBuilder appendRaw(String str) {
        sb.append(' ').append(StringUtil.printable(str));
        return this;
    }
    
    public LineBuilder append(Object obj) {
        return append(obj.toString());
    }
    
    public LineBuilder appendQuoted(String str) {
        addSep();
        sb.append(StringUtil.QuoteEscape(str));
        return this;
    }
    
    public LineBuilder appendNonNull(Directive dir, Object obj) {
        if (sb.length() != 0) {
            // "Directive must be first token but line = %s"
            throw new LogIllegalStateException(M81,sb);
        }
        if (obj != null) {
            appendDirective(dir);
            String str = obj.toString();
            return dir.hasQuotedArg()?appendQuoted(str):append(str);
        }
        return this;
    }

    public LineBuilder appendDirective(Directive dir) {
        CHECK_SUPPORTS(dir);
        addSep();
        sb.append(dir);
        return this;
    }

    public LineBuilder appendNonNull(String str) {
        if (str != null) {
            append(str);
        }
        return this;
    }

    public LineBuilder appendNonNullName(String str) {
        if (str != null) {
            addSep();
            sb.append(StringUtil.escapeName(str));
        }
        return this;
    }

    public LineBuilder append(ReservedWord rw, Object obj) {
        if (obj != null) {
            append(rw.toString());
            append(rw.stringify(obj.toString()));
        }
        return this;
    }
    
    public LineBuilder append(ReservedWord rw, boolean required) {
        if (required) {
            append(rw.toString());
        }
        return this;
    }
    
    public LineBuilder appendComment(Object obj) {
        addSep();
        sb.append("; ");
        String str = obj.toString();
        if (!StringUtil.isPrintableAscii(str)) {
            str = StringUtil.highlightUnprintablesAscii(str);
            LOG(M142,str); // "comment contains unprintable characters (replaced by '?'); comment = '%s'"
        }
        sb.append(str);
        return this;
    }

    public LineBuilder append(EnumSet<AccessFlag> result, String name) {
        String[] flagstr = AccessFlag.stringArrayOf(result);
        appendBlank().append(flagstr)
                .appendNonNullName(name);   // enclose with '\'' as may be accflag which is not a reserved word
                                            // parameter name may be null

        return this;
    }

    public void blankline() {
        pw.println();
    }
    
    public void printDirective(Directive dir, Object obj) {
        appendNonNull(dir, obj).nl();
    }
    
    private String quoted(String str) {
        if (str == null) {
            return null;
        }
        return StringUtil.QuoteEscape(str);
    }
    
    
    public final void printline(String line) {
        String trim = line.trim();
        if (!trim.isEmpty() && trim.charAt(0) == Line.DIRECTIVE_INICATOR) {
            pw.println(quoted(line));
        } else {
            pw.println(StringUtil.printable(line));
        }
    }

    @Override
    public String toString() {
        return sb.toString();
    }

    
}

package jvm;

import java.util.Objects;

import static jvm.JvmVersion.MIN_VERSION;
import static jvm.JvmVersion.NEVER;
import static jynx.Message.M62;

import jynx.LogAssertionError;

public class JvmVersionRange {

    public static final JvmVersionRange UNLIMITED = new JvmVersionRange(MIN_VERSION, MIN_VERSION, NEVER, NEVER);

    private final JvmVersion preview;
    private final JvmVersion start;
    private final JvmVersion deprecate;
    private final JvmVersion end;
    private final int level;
    
    public JvmVersionRange (JvmVersion preview, JvmVersion start, JvmVersion deprecate, JvmVersion end) {
        this(0,preview,start,deprecate,end);
    }
   
    private JvmVersionRange (int level, JvmVersion preview, JvmVersion start, JvmVersion deprecate, JvmVersion end) {
        Objects.nonNull(preview);
        Objects.nonNull(start);
        Objects.nonNull(deprecate);
        Objects.nonNull(end);
        assert start.compareTo(end) <= 0;
        assert preview.compareTo(start) < 0 && preview.isPreview() || preview.equals(start);
        assert deprecate.compareTo(start) >= 0 && deprecate.compareTo(end) <= 0;
        this.preview = preview;
        this.start = start;
        this.deprecate = deprecate;
        this.end = end;
        this.level = level;
        checkLevel(level);
    }
    
    private static final int MAXIMUM_LEVEL = 16;

    public static void checkLevel(int level) {
        if (level > MAXIMUM_LEVEL) {
           // "macro nest level exceeds %d"
            throw new LogAssertionError(M62,MAXIMUM_LEVEL);
        }
   }

    public boolean isSupportedBy(JvmVersion jvmversion) {
        return jvmversion.compareTo(start) >= 0 && jvmversion.compareTo(end) < 0
                || jvmversion.isPreview() && jvmversion.compareTo(preview) >= 0;
    }

    public boolean isDeprecated(JvmVersion jvmversion) {
        assert isSupportedBy(jvmversion);
        return deprecate.compareTo(end) < 0 && jvmversion.compareTo(deprecate) >= 0;
    }
    
    public JvmVersionRange intersect(JvmVersionRange other) {
        int newlevel = Math.max(level,other. level) + 1;
        return new JvmVersionRange(newlevel,
                max(preview,other.preview),
                max(start,other.start),
                min(deprecate,other.deprecate),
                min(end,other.end));
    }
    
    private JvmVersion min(JvmVersion v1, JvmVersion v2) {
        return v1.compareTo(v2) <= 0?v1:v2;
    }
    
    private JvmVersion max(JvmVersion v1, JvmVersion v2) {
        return v1.compareTo(v2) >= 0?v1:v2;
    }
    
    @Override
    public String toString() {
        String limitsmsg = "";
        if (start == NEVER) {
            return "preview from " + preview.toString();
        }
        if (start != JvmVersion.MIN_VERSION) limitsmsg += "from " + start.toString();
        if (start != JvmVersion.MIN_VERSION && end != JvmVersion.NEVER) limitsmsg += " to ";
        if (end != JvmVersion.NEVER) limitsmsg += " before " + end.toString();
        if (limitsmsg.isEmpty()) {
            return "UNLIMITED";
        }
        return limitsmsg;
    }
}

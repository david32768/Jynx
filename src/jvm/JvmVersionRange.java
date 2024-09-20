package jvm;

import java.util.EnumSet;
import java.util.Objects;

import static jvm.JvmVersion.MIN_VERSION;
import static jvm.JvmVersion.NEVER;
import static jynx.Message.M62;

import jynx.LogAssertionError;

public class JvmVersionRange {

    public static final JvmVersionRange UNLIMITED = 
            new JvmVersionRange(EnumSet.noneOf(JvmVersion.class), MIN_VERSION, NEVER, NEVER);

    private final EnumSet<JvmVersion> preview;
    private final JvmVersion start;
    private final JvmVersion deprecate;
    private final JvmVersion end;
    private final int level;
    
    public JvmVersionRange (EnumSet<JvmVersion> preview, JvmVersion start, JvmVersion deprecate, JvmVersion end) {
        this(0,preview,start,deprecate,end);
    }
   
    private JvmVersionRange (int level, EnumSet<JvmVersion> preview,
            JvmVersion start, JvmVersion deprecate, JvmVersion end) {
        Objects.nonNull(preview);
        Objects.nonNull(start);
        Objects.nonNull(deprecate);
        Objects.nonNull(end);

        assert !start.isPreview(): "start = " + start;
        assert !deprecate.isPreview(): "deprecate = " + deprecate;
        assert !end.isPreview(): "end = " + end;
        assert preview.stream().allMatch(v -> v.isPreview() && v.compareTo(start) < 0);

        assert start.compareTo(end) <= 0;
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
        if (level < 0 || level > MAXIMUM_LEVEL) {
           // "macro nest level is negative or exceeds %d"
            throw new LogAssertionError(M62,MAXIMUM_LEVEL);
        }
   }

    public boolean isSupportedBy(JvmVersion jvmversion) {
        return jvmversion.compareTo(start) >= 0 && jvmversion.compareTo(end) < 0
                || preview.contains(jvmversion);
    }

    public boolean isDeprecated(JvmVersion jvmversion) {
        assert isSupportedBy(jvmversion);
        return deprecate.compareTo(end) < 0 && jvmversion.compareTo(deprecate) >= 0;
    }
    
    public JvmVersionRange intersect(JvmVersionRange other) {
        int newlevel = Math.max(level,other. level) + 1;
        EnumSet<JvmVersion> npreview = preview.clone();
        npreview.retainAll(other.preview);
        JvmVersion nstart = max(start,other.start);
        JvmVersion ndeprecate = min(deprecate,other.deprecate);
        JvmVersion nend = min(end,other.end);

        ndeprecate = max(nstart,ndeprecate);
        nstart = min(nstart, nend);

        return new JvmVersionRange(newlevel,
                npreview,
                nstart,
                ndeprecate,
                nend);
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

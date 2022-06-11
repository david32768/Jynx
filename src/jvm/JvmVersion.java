package jvm;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.Opcodes;

import static jynx.Global.LOG;
import static jynx.Message.*;

public enum JvmVersion {

    // MUST BE IN RELEASE ORDER for compareTo
    V1_0_2(45), // 45.0 to 45.3
    
    V1_1(Opcodes.V1_1), // 45.3 to 45.65535

    V1_2(Opcodes.V1_2),
    V1_3(Opcodes.V1_3),
    V1_4(Opcodes.V1_4),
    V1_5(Opcodes.V1_5),
    V1_6JSR(Opcodes.V1_6), // may contain nuts
    V1_6(Opcodes.V1_6),
    V1_7(Opcodes.V1_7),
    V1_8(Opcodes.V1_8),
    V9(Opcodes.V9),
    V10(Opcodes.V10),
    V11(Opcodes.V11),
    V12(Opcodes.V12),
    V12_PREVIEW(Opcodes.V12 | Opcodes.V_PREVIEW),
    V13(Opcodes.V13),
    V13_PREVIEW(Opcodes.V13 | Opcodes.V_PREVIEW),
    V14(Opcodes.V14),
    V14_PREVIEW(Opcodes.V14 | Opcodes.V_PREVIEW),
    V15(Opcodes.V15),
    V15_PREVIEW(Opcodes.V15 | Opcodes.V_PREVIEW),
    V16(Opcodes.V16),
    V16_PREVIEW(Opcodes.V16 | Opcodes.V_PREVIEW),
    V17(Opcodes.V17),
    V17_PREVIEW(Opcodes.V17 | Opcodes.V_PREVIEW),
    V18(Opcodes.V18),
    V18_PREVIEW(Opcodes.V18 | Opcodes.V_PREVIEW),
    V19(63), // Opcodes.V19
    V19_PREVIEW(63 | Opcodes.V_PREVIEW),  // Opcodes.V19
    
    NEVER(-1); // must be last 0xffff ffff
    
    private final int major;
    private final int minor;
    
    private JvmVersion(int relver) {
        this.major = relver & 0xffff;
        this.minor = relver >>> 16;
        assert major == checkMajor(major);
        assert minor == checkMinor(major,minor);
    }

    private static final int MAJOR_BASE = 44;
    private static final int PREVIEW = 0xffff;
    
    private static int checkMinor(int major, int minor) {
        if (major >= 56 && minor != 0 && minor != PREVIEW || minor < 0 || minor > 0xffff) {
            LOG(M21, minor); // "invalid minor version(%d) - spec table 4.1A"
            minor = 0;
        }
        return minor;
    }
    
    private static int checkMajor(int major) {
        int result = Math.max(major,MAJOR_BASE + 1);
        result = Math.min(result,0xffff);
        if (result != major) {
            LOG(M20,major); // "invalid major version(%d)"
        }
        return result;
    }
    
    public int getRelease() {
        return (minor << 16) | major;
    }

    public boolean isPreview() {
        return minor == PREVIEW && compareTo(V12) >= 0;
    }

    private static final Map<String,JvmVersion> PARSE_MAP;
    
    public final static JvmVersion MIN_VERSION = V1_0_2;
    public final static JvmVersion DEFAULT_VERSION = V18;
    public final static JvmVersion SUPPORTED_VERSION = V18;
    public final static JvmVersion MAX_VERSION = V19_PREVIEW;

    static {
        PARSE_MAP = new HashMap<>();
        JvmVersion last = null;
        for (JvmVersion version:values()) {
            assert last == null
                    || version == V1_6  && last == V1_6JSR
                    || last.major < version.major
                    || last.major == version.major && last.minor < version.minor
                    :String.format("last = %s this = %s",last,version);
            PARSE_MAP.put(version.asJava(), version);
            last = version;
        }
        assert PREVIEW == Opcodes.V_PREVIEW >>> 16;
    }

    public static JvmVersion getVersionInstance(String verstr) {
        JvmVersion version = PARSE_MAP.get(verstr.toUpperCase());
         if (version == null) {
            version = DEFAULT_VERSION;
            LOG(M147,verstr, version);   // "unknown Java version %s - %s used"
        }
        JvmVersion maxversion = MAX_VERSION;
        if (version.compareTo(MIN_VERSION) < 0) {
            LOG(M171,version,MIN_VERSION,maxversion,MIN_VERSION);  // "version %s outside range [%s,%s] - %s used"
            version = MIN_VERSION;
        } else if (version.compareTo(maxversion) > 0) {
            LOG(M171,version,MIN_VERSION,maxversion, maxversion);  // "version %s outside range [%s,%s] - %s used"
            version = maxversion;
        }
        version.checkSupported();
        return version;
    }
    
    public static JvmVersion getInstance(int release) {
        JvmVersion version = V1_0_2;
        int major = release & 0xffff;
        int minor = release >>>16;
        for (JvmVersion jversion:values()) {
            if (major < jversion.major || major == jversion.minor && minor < jversion.minor) {
                LOG(M200,release,version); // "unknown release (%d): used %s"
                break;
            }
            version = jversion;
            if (major == jversion.major && minor == jversion.minor) {
                break;
            }
        }
        version.checkSupported();
        return version;
    }
    
    public void checkSupported() {
        if (isPreview() || compareTo(SUPPORTED_VERSION) > 0) {
            LOG(M72,this); // "version %s may not be fully supported"
        }
    }
    
    public String asJvm() {
        return String.format("%d.%d",major, minor);
    }
    
    public String asJava() {
        return name();
    }
    
    public boolean supports(JvmVersioned versioned) {
        return versioned.range().isSupportedBy(this);
    }
    
    public boolean checkSupports(JvmVersioned versioned) {
        boolean supported = supports(versioned);
        if (supported) {
            if (versioned.range().isDeprecated(this)) {
                //"%s is deprecated in version %s"
                LOG(M263,versioned,this);
            }
        } else {
            LOG(M57,this,versioned,versioned.range());    // "Version %s does not support %s (supported %s)"
        }
        return supported;
    }

    public boolean checkCanLoad(ConstantPoolType cp) {
        return cp.isLoadableBy(this);
    }

    @Override
    public String toString() {
        return String.format("%s(%s)",asJava(),asJvm());
    }
    
}

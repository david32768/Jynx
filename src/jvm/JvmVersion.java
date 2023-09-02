package jvm;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.Opcodes;

import static jynx.Global.LOG;
import static jynx.Message.*;

import asm.CheckOpcodes;

public enum JvmVersion {

    // MUST BE IN RELEASE ORDER for compareTo
    V1_0_2(45, 0), // 45.0 to 45.3
    
    V1_1(Opcodes.V1_1), // 45.3 to 45.65535

    V1_2(Opcodes.V1_2),
    V1_3(Opcodes.V1_3),
    V1_4(Opcodes.V1_4),
    V1_5(Opcodes.V1_5),
    V1_6JSR(Opcodes.V1_6), // may contain opcodes jsr, ret
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
    V18(62),    // Opcode.V18
    V18_PREVIEW(62 | Opcodes.V_PREVIEW),    // Opcode.V18
    V19(63), // Opcodes.V19
    V19_PREVIEW(63 | Opcodes.V_PREVIEW),  // Opcodes.V19
    V20(64), // Opcodes.V20
    V20_PREVIEW(64 | Opcodes.V_PREVIEW),  // Opcodes.V20
    V21(65), // Opcodes.V21
    V21_PREVIEW(65 | Opcodes.V_PREVIEW),  // Opcodes.V21
    V22(66), // Opcodes.V22
    V22_PREVIEW(66 | Opcodes.V_PREVIEW),  // Opcodes.V22
    
    NEVER(-1); // must be last 0xffff ffff
    
    private final long release; // 0x00000000 major minor
    private final int major;
    private final int minor;
    
    private JvmVersion(int release4ASM) {
        this(release4ASM & 0xffff, release4ASM >>> 16);
    }

    private JvmVersion(int major, int minor) {
        this.major = major;
        this.minor = minor;
        this.release = Integer.toUnsignedLong((major << 16) | minor);
        // "invalid major version - %s"
        assert isUnsignedShort(major) && major > MAJOR_BASE:M20.format(this);
        // "invalid minor version - %s"
        assert isUnsignedShort(minor):M21.format(this);
        // "invalid minor version for major version (spec table 4.1A) - %s"
        assert major < MAJOR_PREVIEW || minor == 0 || minor == PREVIEW:M91.format(this);
    }
    
    private static boolean isUnsignedShort(int ushort) {
        return ushort == Short.toUnsignedInt((short)ushort);
    }
    
    private static final int MAJOR_BASE = 44;
    private static final int MAJOR_PREVIEW = 56;
    private static final int PREVIEW = 0xffff;
        
    public int toASM() {
        return minor << 16 | major;
    }

    public boolean isPreview() {
        return minor == PREVIEW && compareTo(V12) >= 0;
    }

    public String asJvm() {
        return String.format("%d.%d",major, minor);
    }
    
    public String asJava() {
        return name();
    }
    
    @Override
    public String toString() {
        return String.format("%s(%s)",asJava(),asJvm());
    }
    
    private static final Map<String,JvmVersion> PARSE_MAP;
    
    public final static JvmVersion MIN_VERSION = V1_0_2;
    public final static JvmVersion DEFAULT_VERSION = V17;
    public final static JvmVersion SUPPORTED_VERSION = V20;
    public final static JvmVersion MAX_VERSION;

    static {
        PARSE_MAP = new HashMap<>();
        JvmVersion last = null;
        for (JvmVersion version:values()) {
            assert last == null
                    || version == V1_6  && last == V1_6JSR
                    || last.release < version.release
                    // "incorret order: last = %s this = %s"
                    :M94.format(last,version);
            PARSE_MAP.put(version.asJava(), version);
            last = version;
        }
        assert PREVIEW == Opcodes.V_PREVIEW >>> 16;
        int maxasm = CheckOpcodes.getMaxJavaVersion();
        JvmVersion asmversion = PARSE_MAP.get(String.format("V%d_PREVIEW", maxasm));
        if (asmversion == null) {
            asmversion = SUPPORTED_VERSION;
        }
        MAX_VERSION = asmversion;
    }

    public static JvmVersion getVersionInstance(String verstr) {
        JvmVersion version = PARSE_MAP.get(verstr.toUpperCase());
         if (version == null) {
            version = DEFAULT_VERSION;
            LOG(M147,verstr, version);   // "unknown Java version %s - %s used"
        }
        if (version.compareTo(MIN_VERSION) < 0) {
            LOG(M171,version,MIN_VERSION,MAX_VERSION,MIN_VERSION);  // "version %s outside range [%s,%s] - %s used"
            version = MIN_VERSION;
        } else if (version.compareTo(MAX_VERSION) > 0) {
            LOG(M171,version,MIN_VERSION,MAX_VERSION, MAX_VERSION);  // "version %s outside range [%s,%s] - %s used"
            version = MAX_VERSION;
        }
        version.checkSupported();
        return version;
    }
    
    public static JvmVersion fromASM(int release) {
        int major = release & 0xffff;
        int minor = release >>>16;
        return from((major << 16) | minor);
    }
    
    public static JvmVersion from(int major, int minor) {
        if (!isUnsignedShort(major)) {
            // "invalid major version - %s"
            LOG(M20,major);
        }
        if (!isUnsignedShort(minor)) {
            // "invalid minor version - %s"
            LOG(M21,minor);
        }
        
        return from((major << 16) | minor);
    }
    
    private static JvmVersion from(int majmin) {
        long release = Integer.toUnsignedLong(majmin);
        JvmVersion last = values()[0];
        for (JvmVersion version:values()) {
            if (release < version.release) {
                // "unknown release (major = %d, minor = %d): used %s"
                LOG(M200, release >>> 16, release & 0xffff, last);
                return last;
            }
            if (release == version.release) {
                return version;
            }
            last = version;
        }
        throw new AssertionError();
    }
    
    public void checkSupported() {
        if (isPreview() || compareTo(SUPPORTED_VERSION) > 0) {
            LOG(M72,this); // "version %s may not be fully supported"
        }
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

}

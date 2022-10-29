package jynx2asm.ops;

import java.util.Arrays;
import java.util.stream.Stream;

import jvm.Feature;

public enum JavaCallOps implements MacroOp {

    inv_bu2i(Byte.class,"toUnsignedInt","(B)I",Feature.unsigned),
    inv_bu2l(Byte.class,"toUnsignedLong","(B)J",Feature.unsigned),

    inv_su2i(Short.class,"toUnsignedInt","(S)I",Feature.unsigned),
    inv_su2l(Short.class,"toUnsignedLong","(S)J",Feature.unsigned),

    inv_iclz(Integer.class,"numberOfLeadingZeros","(I)I",Feature.bitops),
    inv_ictz(Integer.class,"numberOfTrailingZeros","(I)I",Feature.bitops),
    inv_ipopct(Integer.class,"bitCount","(I)I",Feature.bitops),
    inv_iudiv(Integer.class,"divideUnsigned","(II)I",Feature.unsigned),
    inv_iurem(Integer.class,"remainderUnsigned","(II)I",Feature.unsigned),
    inv_irotl(Integer.class,"rotateLeft","(II)I",Feature.bitops),
    inv_irotr(Integer.class,"rotateRight","(II)I",Feature.bitops),
    inv_iu2l(Integer.class,"toUnsignedLong","(I)J",Feature.unsigned),
    inv_ibox(Integer.class,"valueOf","(I)Ljava/lang/Integer;"),
        
    inv_lclz(Long.class,"numberOfLeadingZeros","(J)I",Feature.bitops),
    inv_lctz(Long.class,"numberOfTrailingZeros","(J)I",Feature.bitops),
    inv_lpopct(Long.class,"bitCount","(J)I",Feature.bitops),
    inv_ludiv(Long.class,"divideUnsigned","(JJ)J",Feature.unsigned),
    inv_lurem(Long.class,"remainderUnsigned","(JJ)J",Feature.unsigned),
    inv_lrotl(Long.class,"rotateLeft","(JI)J",Feature.bitops),
    inv_lrotr(Long.class,"rotateRight","(JI)J",Feature.bitops),
    inv_lbox(Long.class,"valueOf","(J)Ljava/lang/Long;"),

    // NB compare method spec does NOT say it returns -1,0,1 (m101)
    // so requires inv_isignum after if m101 required
        inv_icompare(Integer.class,"compare","(II)I",Feature.V7methods),
        inv_iucompare(Integer.class,"compareUnsigned","(II)I",Feature.unsigned),
//          inv_isignum(Integer.class,"signum","(I)I",Feature.V5methods), // non call op (ext_isignum) has size 3 as well.
//          inv_lcompare(Long.class,"compare","(JJ)J",Feature.V7methods), // NB asm_lcmp returns m101 and has size 1
        inv_lucompare(Long.class,"compareUnsigned","(JJ)I",Feature.unsigned),
    //
    
    inv_iasf(Float.class,"intBitsToFloat","(I)F"),
    inv_fasi(Float.class,"floatToRawIntBits","(F)I",Feature.V3methods),
    inv_fbox(Float.class,"valueOf","(F)Ljava/lang/Float;"),

    inv_lasd(Double.class,"longBitsToDouble","(J)D"),
    inv_dasl(Double.class,"doubleToRawLongBits","(D)J",Feature.V3methods),
    inv_dbox(Double.class,"valueOf","(D)Ljava/lang/Double;"),

    
    inv_iabs(Math.class,"abs","(I)I"),
    inv_imin(Math.class,"min","(II)I"),
    inv_imax(Math.class,"max","(II)I"),

    inv_labs(Math.class,"abs","(J)J"),
    inv_lmin(Math.class,"min","(JJ)J"),
    inv_lmax(Math.class,"max","(JJ)J"),

    inv_fabs(Math.class,"abs","(F)F"),
    inv_fmin(Math.class,"min","(FF)F"),
    inv_fmax(Math.class,"max","(FF)F"),

    inv_fsignum(Math.class,"signum","(F)F",Feature.V5methods),
    inv_fcopysign(Math.class,"copySign","(FF)F",Feature.V6methods),
    
    inv_dabs(Math.class,"abs","(D)D"),
    inv_dmin(Math.class,"min","(DD)D"),
    inv_dmax(Math.class,"max","(DD)D"),

    inv_dsignum(Math.class,"signum","(D)D",Feature.V5methods),
    inv_dcopysign(Math.class,"copySign","(DD)D",Feature.V6methods),

    // no float variants
    inv_dceil(Math.class,"ceil","(D)D"),
    inv_dfloor(Math.class,"floor","(D)D"),
    inv_dsqrt(Math.class,"sqrt","(D)D"),
    inv_drint(Math.class,"rint","(D)D"),
    ;


    private final Class<?> klass;
    private final String methodName;
    private final String desc;
    private final Feature feature;
    private final JynxOp[] jynxops;

    private JavaCallOps(Class<?> klass,String method,String type) {
        this(klass,method,type,Feature.unlimited);
    }

    private JavaCallOps(Class<?> klass, String methodName, String desc, Feature feature) {
        this.klass = klass;
        this.methodName = methodName;
        this.desc = desc;
        this.feature = feature;
        this.jynxops = new JynxOp[2];
        String classname = klass.getName().replace('.', '/');
        jynxops[0] = LineOps.insertMethod(classname,methodName,desc);
        jynxops[1] = JvmOp.asm_invokestatic;
    }

    public Feature feature() {
        return feature;
    }

    @Override
    public boolean isExternal() {
        return name().startsWith("inv_");
    }

    @Override
    public String toString() {
        return name().substring(4);
    }
    
    public static Stream<JavaCallOps> streamExternal() {
        return Arrays.stream(values())
            .filter(JavaCallOps::isExternal);
    }

    @Override
    public JynxOp[] getJynxOps() {
        return jynxops;
    }
    
}

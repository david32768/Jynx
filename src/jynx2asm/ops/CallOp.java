package jynx2asm.ops;

import java.lang.invoke.MethodType;
import jvm.Feature;
import jvm.JvmVersionRange;

public class CallOp implements MacroOp {
    
    private final JynxOp[] jynxops;
    private final Feature feature;

    private CallOp(JynxOp[] jynxops, Feature feature) {
        this.jynxops = jynxops;
        this.feature = feature;
    }

    @Override
    public JynxOp[] getJynxOps() {
        return jynxops;
    }

    @Override
    public JvmVersionRange range() {
        return feature.range();
    }
    
    public static CallOp of(String classname, String methodname, String desc) {
        return of(classname, methodname, desc, Feature.unlimited);
    }

    public static CallOp of(Class<?> klass, String methodname, String desc) {
        return of(className(klass), methodname, desc, Feature.unlimited);
    }

    public static CallOp of(Class<?> klass, String methodname, String desc, Feature feature) {
        return of(className(klass), methodname, desc, feature);
    }

    public static CallOp of(String classname, String methodname, String desc, Feature feature) {
        JynxOp[] jynxops = new JynxOp[2];
        jynxops[0] = AdjustToken.insertMethod(classname, methodname, desc);
        jynxops[1] = JvmOp.asm_invokestatic;
        return new CallOp(jynxops, feature);
    }

    public static String className(Class<?> klass) {
        if (klass.isPrimitive()) {
            return parmName(klass);
        }
        return klass.getName().replace('.', '/');
    }
    
    public static String parmName(Class<?> klass) {
        return MethodType.methodType(klass).toMethodDescriptorString().substring(2);
    }
    
    public static String descFrom(Class<?> rtype, Class<?>... ptypes) {
        return MethodType.methodType(rtype, ptypes).toMethodDescriptorString();
    }
    
}

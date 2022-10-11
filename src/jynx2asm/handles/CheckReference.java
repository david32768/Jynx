package jynx2asm.handles;

import java.lang.invoke.MethodType;
import java.lang.reflect.AccessibleObject;

import static jynx.Global.CLASS_NAME;
import static jynx.Global.LOG;
import static jynx.Message.M400;
import static jynx.Message.M407;

import jvm.Context;
import jvm.HandleType;

public class CheckReference {

    private final String ondstr;
    private final String owner;
    private final String name;
    private final String desc;
    private final HandleType ht;
    private final Context context;

    public CheckReference(JynxHandle jh) {
        this.ondstr = jh.ond();
        this.owner = jh.owner();
        this.name = jh.name();
        this.ht = jh.ht();
        if (ht.isField()) {
            this.desc = "()" + jh.desc();
            this.context = Context.FIELD;
        } else {
            this.desc = jh.desc();
            this.context = Context.METHOD;
        }
    }

    private void checkDeprecated(AccessibleObject mfc) {
        boolean has = mfc.isAnnotationPresent(Deprecated.class);
        if (has) {
            LOG(M407,context,ondstr); // "%s %s is deprecated"
        }
    }
    
    void check() {
        try {
            MethodType mt = MethodType.fromMethodDescriptorString(desc, null);
            Class<?> klass = Class.forName(owner.replace('/', '.'),false,
                    ClassLoader.getSystemClassLoader());
            Class<?>[] parms = mt.parameterArray();
            AccessibleObject mfc;
            switch (ht) {
                case REF_invokeStatic:
                case REF_invokeInterface:
                case REF_invokeVirtual:
                case REF_invokeSpecial:
                    mfc = klass.getMethod(name, parms);
                    break;
                case REF_newInvokeSpecial:
                    mfc = klass.getConstructor(parms);
                    break;
                case REF_getStatic:
                case REF_putStatic:
                case REF_getField:
                case REF_putField:
                    mfc = klass.getField(name);
                    break;
                default: 
                    throw new EnumConstantNotPresentException(ht.getClass(), ht.name());
            }
            checkDeprecated(mfc);
        } catch (ClassNotFoundException
                | IllegalArgumentException
                | NoSuchFieldException
                | NoSuchMethodException ex) {
             // "unable to find %s %s because of %s"
            LOG(M400,context,ondstr,ex.getClass().getSimpleName());
        } catch (SecurityException iaex) {
            if (ht == HandleType.REF_invokeSpecial) { // maybe protected
                return;
            } else if (HandlePart.isSamePackage(CLASS_NAME(),owner)) { // maybe package-private
                return;
            }
             // "unable to find %s %s because of %s"
            LOG(M400,context,ondstr,iaex.getClass().getSimpleName());
        } catch (TypeNotPresentException typex) {
            String typename = typex.typeName().replace(".","/");
            if (typename.equals(CLASS_NAME()))  {
                return;
            }
            String cause = typex.getClass().getSimpleName() + " " + typename;
             // "unable to find %s %s because of %s"
            LOG(M400,context,ondstr,cause);
        }
    }
    
}

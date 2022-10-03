package jynx2asm;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static jvm.HandleType.REF_invokeSpecial;
import static jynx.Global.CLASS_NAME;
import static jynx.Global.LOG;
import static jynx.Message.M400;

import jvm.Context;
import jvm.HandleType;

public class CheckPresent {

    private CheckPresent(){}
    
    private final static MethodHandles.Lookup LOOKUP = MethodHandles.publicLookup();
    
    public static void check(Context context, OwnerNameDesc ond, HandleType ht) {
        String owner = ond.getOwner();
        String name = ond.getName();
        String desc = ond.getHandleDesc(); // adds () for field
        try {
            MethodType mt = MethodType.fromMethodDescriptorString(desc, null);
            Class<?> klass = Class.forName(owner.replace('/', '.'),false,
                    ClassLoader.getSystemClassLoader());
            Class<?> typeklass = mt.returnType();
            switch (ht) {
                case REF_invokeStatic:
                    LOOKUP.findStatic(klass, name, mt);
                    break;
                case REF_invokeSpecial:
                case REF_invokeInterface:
                case REF_invokeVirtual:
                    LOOKUP.findVirtual(klass, name, mt);
                    break;
                case REF_newInvokeSpecial:
                    LOOKUP.findConstructor(klass, mt);
                    break;
                case REF_getStatic:
                    LOOKUP.findStaticGetter(klass, name, typeklass);
                    break;
                case REF_putStatic:
                    LOOKUP.findStaticSetter(klass, name, typeklass);
                    break;
                case REF_getField:
                    LOOKUP.findGetter(klass, name, typeklass);
                    break;
                case REF_putField:
                    LOOKUP.findSetter(klass, name, typeklass);
                    break;
                default: 
                    throw new EnumConstantNotPresentException(ht.getClass(), ht.name());
            }
        } catch (ClassNotFoundException
                | IllegalArgumentException
                | NoSuchFieldException
                | NoSuchMethodException ex) {
             // "unable to find %s %s because of %s"
            LOG(M400,context,ond.toJynx(),ex.getClass().getSimpleName());
        } catch (IllegalAccessException iaex) {
            if (ht == REF_invokeSpecial) { // maybe protected
                return;
            } else if (ond.isSamePackage(CLASS_NAME())) { // maybe package-private
                return;
            }
             // "unable to find %s %s because of %s"
            LOG(M400,context,ond.toJynx(),iaex.getClass().getSimpleName());
        } catch (TypeNotPresentException typex) {
            String typename = typex.typeName().replace(".","/");
            if (typename.equals(CLASS_NAME()))  {
                return;
            }
            String cause = typex.getClass().getSimpleName() + " " + typename;
             // "unable to find %s %s because of %s"
            LOG(M400,context,ond.toJynx(),cause);
        }
    }
    
    public static void method(OwnerNameDesc ond, HandleType ht) {
        assert !ht.isField();
        String owner = ond.getOwner();
        assert !owner.equals(CLASS_NAME());
        check(Context.METHOD,ond,ht);
    }
    
    public static void field(OwnerNameDesc ond, HandleType ht) {
        assert ht.isField();
        String owner = ond.getOwner();
        assert !owner.equals(CLASS_NAME());
        check(Context.FIELD,ond,ht);
    }
    
}

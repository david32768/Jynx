package jynx2asm;

import org.objectweb.asm.tree.MethodNode;

import static jynx.Global.CHECK_SUPPORTS;
import static jynx.Global.LOG;
import static jynx.Message.M135;
import static jynx.Message.M139;
import static jynx.Message.M145;
import static jynx.Message.M242;
import static jynx2asm.NameDesc.ARRAY_METHOD_NAME_DESC;
import static jynx2asm.NameDesc.CLASS_NAME;
import static jynx2asm.NameDesc.INTERFACE_METHOD_NAME;
import static jynx2asm.NameDesc.INTERFACE_METHOD_NAME_DESC;
import static jynx2asm.NameDesc.METHOD_NAME;
import static jynx2asm.NameDesc.METHOD_NAME_DESC;

import jvm.Feature;
import jynx.LogIllegalArgumentException;
import jynx2asm.ops.JvmOp;

public class MethodDesc extends OwnerNameDesc {

    private MethodDesc(ONDRecord ond) {
        super(ond);
    }

   
    public static MethodDesc of(MethodNode mn) {
        return getLocalInstance(mn.name + mn.desc);
    }
    
    public static MethodDesc getLocalInstance(String mspec) {
        ONDRecord ond = ONDRecord.getInstance(mspec);
        if (ond.isInterface()
                || ond.owner() != null
                || ond.name() == null || ond.name().isEmpty() 
                || ond.desc() == null || !ond.hasParameters()
                ) {
            // "Invalid method description %s"
            throw new LogIllegalArgumentException(M145,mspec);
        }
        boolean ok = METHOD_NAME.validate(ond.name());
        if (ok) {
            METHOD_NAME_DESC.validate(ond.nameDesc());
        }
        return new MethodDesc(ond);
    }
    
    private static ONDRecord checkInterface(JvmOp op, ONDRecord ond) {
        boolean ownerInterface = ond.isInterface();
        switch(op) {
            case asm_invokeinterface:
                if (!ownerInterface){
                    LOG(M135,ONDRecord.INTERFACE_PREFIX,op);   // "for consistency add %s prefix to method name for %s"
                }
                ownerInterface = true;
                break;
            case asm_invokevirtual:
                if (ownerInterface) {
                    LOG(M139,ONDRecord.INTERFACE_PREFIX,op);   // "%s prefix is invalid for %s"
                }
                ownerInterface = false;
                break;
            case asm_invokespecial:
                if (ownerInterface) {
                    ownerInterface = CHECK_SUPPORTS(Feature.invokespecial_interface);
                }
                break;
            case asm_invokestatic:
                if (ownerInterface) {
                    ownerInterface = CHECK_SUPPORTS(Feature.invokestatic_interface);
                }
                break;
            default:
                if (ownerInterface) {
                    LOG(M139,ONDRecord.INTERFACE_PREFIX,op);   // "%s prefix is invalid for %s"
                }
                ownerInterface = false;
                break;
        }
        return ond.setInterface(ownerInterface);
    }
    
    public static OwnerNameDesc getInstance(String mspec, JvmOp op) {
        ONDRecord ond = ONDRecord.getInstance(mspec);
        ond = checkInterface(op, ond);
        ond = ond.addClassName(op);
        if (ond.isStaticInit() || ond.isInit() && op != JvmOp.asm_invokespecial) {
            // "either init method %s is static or op  is not %s"
            throw new LogIllegalArgumentException(M242,ond.toJynx(),JvmOp.asm_invokespecial);
        }
        return getInstanceOfObjectMethod(ond);
    }
    
    public static OwnerNameDesc getInstance(String mspec) {
        ONDRecord ond = ONDRecord.getInstance(mspec);
        return getInstanceOfObjectMethod(ond);
    }

    private static OwnerNameDesc getInstanceOfObjectMethod(ONDRecord ond) {
        if (ond.desc() == null || ond.owner() == null) {
            // "Invalid method description %s"
            throw new LogIllegalArgumentException(M145,ond.toJynx());
        }
        if (ond.isArray()) {
            if (ond.isInterface()) {
                // "Invalid method description %s"
                throw new LogIllegalArgumentException(M145,ond.toJynx());
            }
            ARRAY_METHOD_NAME_DESC.validate(ond.nameDesc());
        } else {
            CLASS_NAME.validate(ond.owner());
            if (ond.isInterface()) {
                boolean ok = INTERFACE_METHOD_NAME.validate(ond.name());
                if (ok) {
                    INTERFACE_METHOD_NAME_DESC.validate(ond.nameDesc());
                }
            } else {
                boolean ok = METHOD_NAME.validate(ond.name());
                if (ok) {
                    METHOD_NAME_DESC.validate(ond.nameDesc());
                }
            }
        }
        return new OwnerNameDesc(ond);
    }
    
}

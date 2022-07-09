package jynx2asm;

import java.util.Objects;

import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.MethodInsnNode;

import static jynx.Global.*;
import static jynx.Message.*;
import static jynx2asm.NameDesc.*;

import jvm.Feature;
import jvm.HandleType;
import jynx.LogIllegalArgumentException;
import jynx2asm.ops.JvmOp;

public class OwnerNameDesc implements Comparable<OwnerNameDesc> {

    private final ONDRecord ond;

    protected OwnerNameDesc(ONDRecord ond) {
        this.ond = ond;
    }

    public static OwnerNameDesc of(Handle handle) {
        return new OwnerNameDesc(ONDRecord.of(handle));
    }
    
    public static OwnerNameDesc of(MethodInsnNode min) {
        return new OwnerNameDesc(ONDRecord.of(min));
    }
    
    public String getOwner() {
        return ond.owner();
    }

    public String getDesc() {
        return ond.desc();
    }

    public String getName() {
        return ond.name();
    }
    
    public String getNameDesc() {
        return ond.nameDesc();
    }
    
    public boolean isOwnerInterface() {
        return ond.isInterface();
    }

    public boolean isInit() {
        return ond.isInit();
    }
    
    public boolean isStaticInit() {
        return ond.isStaticInit();
    }

    public String getPackageName() {
        return ond.packageName();
    }
    
    public boolean isSamePackage(String other) {
        return ond.isSamePackage(other);
    }
    
    public String toJynx() {
        return ond.toJynx();
    }

    @Override
    public int compareTo(OwnerNameDesc other) {
        return this.toJynx().compareTo(other.toJynx());
    }

    @Override
    public String toString() {
        return ond.toString();
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
    
    public static OwnerNameDesc getClass(String mspec) {
        CLASS_NAME.validate(mspec);
        return new OwnerNameDesc(ONDRecord.classInstance(mspec));
    }

    public static OwnerNameDesc getOwnerMethodDesc(String mspec) {
        ONDRecord ond = ONDRecord.getInstance(mspec);
        return getInstanceOfObjectMethod(ond);
    }

    public static OwnerNameDesc getOwnerMethodDescAndCheck(String mspec, HandleType ht) {
        if (ht.isField()) {
            return FieldDesc.getInstance(mspec, ht);
        }
        return getOwnerMethodDescAndCheck(mspec, ht.op());
    }

    public static OwnerNameDesc getOwnerMethodDescAndCheck(String mspec, JvmOp op) {
        ONDRecord ond = ONDRecord.getInstance(mspec);
        ond = checkInterface(op, ond);
        ond = ond.addClassName(op);
        if (ond.isStaticInit() || ond.isInit() && op != JvmOp.asm_invokespecial) {
            // "either init method %s is static or op  is not %s"
            throw new LogIllegalArgumentException(M242,ond.toJynx(),JvmOp.asm_invokespecial);
        }
        return getInstanceOfObjectMethod(ond);
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
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof OwnerNameDesc) {
            OwnerNameDesc other = (OwnerNameDesc)obj;
            return Objects.equals(ond,other.ond);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return ond.hashCode();
    }

}


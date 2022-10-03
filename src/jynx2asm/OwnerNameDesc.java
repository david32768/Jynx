package jynx2asm;

import java.util.Objects;

import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.MethodInsnNode;

import static jynx2asm.NameDesc.*;

import jvm.HandleType;
import jynx.Global;

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

    public String getHandleDesc() {
        return ond.handleDesc();
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

    public static OwnerNameDesc getClass(String mspec) {
        String mspecx = Global.TRANSLATE_OWNER(mspec);
        CLASS_NAME.validate(mspecx);
        return new OwnerNameDesc(ONDRecord.classInstance(mspecx));
    }

    public static OwnerNameDesc getInstance(String mspec, HandleType ht) {
        if (ht.isField()) {
            return FieldDesc.getInstance(mspec, ht);
        }
        return MethodDesc.getInstance(mspec, ht.op());
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


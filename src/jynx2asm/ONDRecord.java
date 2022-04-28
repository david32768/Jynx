package jynx2asm;

import java.util.Objects;

import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import static jynx2asm.NameDesc.PACKAGE_NAME;
import jvm.Constants;

public class ONDRecord {
    
    private final String owner;
    private final String name;
    private final String desc;
    private final boolean ownerInterface;

    private ONDRecord(String owner, String name, String desc, boolean ownerInterface) {
        this.owner = owner;
        this.name = name;
        this.desc = desc;
        this.ownerInterface = ownerInterface;
    }

    public static ONDRecord of(Handle handle) {
        return new  ONDRecord(handle.getOwner(),handle.getName(),handle.getDesc(),handle.isInterface());
    }
    
    public static ONDRecord of(MethodInsnNode min) {
        return new ONDRecord(min.owner,min.name,min.desc,min.itf);
    }
    
    public static ONDRecord of(MethodNode mn) {
        return new ONDRecord(null,mn.name,mn.desc,false);
    }
    
    public static ONDRecord classInstance(String klass) {
        return new ONDRecord (klass,null,null,false);
    }
    
    protected static final char INTERFACE_PREFIX = '@';
    private static final char LEFT_BRACKET = '(';
    private static final char ARRAY_MARKER = '[';
    private static final char DOT = '.';
    private static final char FORWARD_SLASH = '/';

    private static final String EMPTY_PARM = "()";
    private static final String VOID_METHOD = ")V";

    
    public static ONDRecord getInstance(String spec) {
        boolean itf = spec.charAt(0) == INTERFACE_PREFIX;
        if (itf) {
            spec = spec.substring(1);
        }
        int lbindex = spec.indexOf(LEFT_BRACKET);
        String mname = spec;
        String mdesc = null;
        if (lbindex >= 0) {
            mname = spec.substring(0,lbindex);
            mdesc = spec.substring(lbindex);
        }
        int slindex = mname.lastIndexOf(FORWARD_SLASH);
        String mclass = null;
        if (slindex >= 0) {
            mclass = mname.substring(0,slindex);
            mname = mname.substring(slindex+1);
        }
        return new ONDRecord(mclass, mname, mdesc, itf);
    }

    public String desc() {
        return desc;
    }

    public String name() {
        return name;
    }

    public String owner() {
        return owner;
    }

    public boolean isInterface() {
        return ownerInterface;
    }

    public boolean isArray() {
        return owner != null && owner.charAt(0) == ARRAY_MARKER;
    }
    
    public boolean hasParameters() {
        return desc != null && !desc.isEmpty() && desc.charAt(0) == LEFT_BRACKET;
    }
    
    public String nameDesc() {
        assert desc != null && desc.charAt(0) == '(';
        return name + desc;
    }
    
    public boolean isInit() {
        return Constants.CLASS_INIT_NAME.equalString(name) && desc.endsWith(VOID_METHOD);
    }
    
    public boolean isStaticInit() {
        return Constants.STATIC_INIT.equalString(nameDesc());
    }
    
    public String packageName() {
        Objects.nonNull(owner);
        return packageNameOf(owner);
    }
    
    public boolean isSamePackage(String other) {
        return packageName().equals(packageNameOf(other));
    }
    
    public static String packageNameOf(String classname) {
        int slindex = classname.lastIndexOf(FORWARD_SLASH);
        if (slindex <= 0) {
            return "";
        }
        String pkgname = classname.substring(0, slindex);
        PACKAGE_NAME.validate(pkgname);
        return pkgname;
    }
    
    public ONDRecord changeOwner(String newowner) {
        assert owner == null;
        return new ONDRecord(newowner, name, desc, ownerInterface);
    }

    public ONDRecord changeDesc(String newdesc) {
        assert desc == null;
        return new ONDRecord(owner, name, newdesc, ownerInterface);
    }

    public ONDRecord setInterface(boolean itf) {
        return new ONDRecord(owner, name, desc, itf);
    }

    public String toJynx() {
        StringBuilder sb = new StringBuilder();
        if (owner != null) {
            if (ownerInterface) {
                sb.append(INTERFACE_PREFIX);
            }
            sb.append(owner);
        }
        if (name != null) {
            if (owner != null) {
                sb.append(FORWARD_SLASH);
            }
            sb.append(name);
        }
        if (desc != null) {
            if (desc.charAt(0) != LEFT_BRACKET) {    // handle for field access
                sb.append(EMPTY_PARM);
            }
            sb.append(desc);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("owner = %s name = %s desc = %s",owner,name,desc);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ONDRecord) {
            ONDRecord other = (ONDRecord)obj;
            return Objects.equals(owner,other.owner)
                    && Objects.equals(name,other.name)
                    && Objects.equals(desc,other.desc)
                    && ownerInterface == other.ownerInterface;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(owner,name,desc,ownerInterface);
    }

}

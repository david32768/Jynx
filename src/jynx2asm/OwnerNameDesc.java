package jynx2asm;

import java.util.Objects;

import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import static jynx.Global.*;
import static jynx.Message.*;
import static jynx2asm.NameDesc.*;

import jvm.AsmOp;
import jvm.Constants;
import jvm.Feature;
import jvm.HandleType;
import jynx.LogIllegalArgumentException;

public class OwnerNameDesc implements Comparable<OwnerNameDesc> {

    private final String owner;
    private final String name;
    private final String desc;
    private final boolean ownerInterface;

    private OwnerNameDesc(String owner, String name, String desc, boolean ownerInterface) {
        this.owner = owner;
        this.name = name;
        this.desc = desc;
        this.ownerInterface = ownerInterface;
    }

    public static OwnerNameDesc of(Handle handle) {
        return new  OwnerNameDesc(handle.getOwner(),handle.getName(),handle.getDesc(),handle.isInterface());
    }
    
    public static OwnerNameDesc of(MethodInsnNode min) {
        return new OwnerNameDesc(min.owner,min.name,min.desc,min.itf);
    }
    
    public static OwnerNameDesc of(MethodNode mn) {
        return new OwnerNameDesc(null,mn.name,mn.desc,false);
    }
    
    public String getOwner() {
        return owner;
    }

    public String getDesc() {
        return desc;
    }

    public String getName() {
        return name;
    }

    
    public String getNameDesc() {
        return name + desc;
    }
    
    public boolean isOwnerInterface() {
        return ownerInterface;
    }

    public boolean isInit() {
        return Constants.CLASS_INIT_NAME.equalString(name);
    }
    
    public boolean isStaticInit() {
        return Constants.STATIC_INIT_NAME.equalString(name);
    }
    
    private static final char INTERFACE_PREFIX = '@';
    private static final char LEFT_BRACKET = '(';
    private static final char DOT = '.';
    private static final char FORWARD_SLASH = '/';

    private static final String EMPTY_PARM = "()";

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
    public int compareTo(OwnerNameDesc other) {
        return this.toJynx().compareTo(other.toJynx());
    }

    @Override
    public String toString() {
        return String.format("owner = %s name = %s desc = %s",owner,name,desc);
    }

    private static OwnerNameDesc getInstanceOfObjectMethod(String mspec, boolean ownerInterface) {
        boolean ok = OBJECT_METHOD_DESC.validate(mspec);
        if (!ok) {
            // "Invalid method description %s"
            throw new LogIllegalArgumentException(M145,mspec);
        }
        int lbindex = mspec.indexOf(LEFT_BRACKET);
        if (lbindex < 0) {
            // "Invalid method description %s"
            throw new LogIllegalArgumentException(M145,mspec);
        }
        String mname = mspec.substring(0,lbindex);
        String mdesc = mspec.substring(lbindex);
        int slindex = mname.lastIndexOf(FORWARD_SLASH);
        if (slindex < 0) {
            // "Invalid method description %s"
            throw new LogIllegalArgumentException(M145,mspec);
        }
        String mclass = mname.substring(0,slindex);
        mname = mname.substring(slindex+1);
        if (mclass.charAt(0) == '[') {
            if (ownerInterface) {
                // "Invalid method description %s"
                throw new LogIllegalArgumentException(M145,mspec);
            }
            ARRAY_METHOD_NAME_DESC.validate(mname + mdesc);
        } else {
            CLASS_NAME.validate(mclass);
            if (ownerInterface) {
                INTERFACE_METHOD_NAME.validate(mname);
                INTERFACE_METHOD_NAME_DESC.validate(mname + mdesc);
            } else {
                METHOD_NAME.validate(mname);
                METHOD_NAME_DESC.validate(mname + mdesc);
            }
        }
        return new OwnerNameDesc(mclass,mname,mdesc,ownerInterface);
    }
    
    public static OwnerNameDesc getMethodDesc(String mspec) {
        boolean ok = METHOD_NAME_DESC.validate(mspec);
        if (!ok) {
            // "Invalid method description %s"
            throw new LogIllegalArgumentException(M145,mspec);
        }
        int lbindex = mspec.indexOf(LEFT_BRACKET);
        if (lbindex < 1) {  // must be at least m()
            // "Invalid method description %s"
            throw new LogIllegalArgumentException(M145,mspec);
        }
        String mname = mspec.substring(0,lbindex);
        String mdesc = mspec.substring(lbindex);
        int slindex = mname.lastIndexOf(FORWARD_SLASH);
        if (slindex >= 0) {
            // "Invalid method description %s"
            throw new LogIllegalArgumentException(M145,mspec);
        } else {
            METHOD_NAME.validate(mname);
        }
        return new OwnerNameDesc(null,mname,mdesc,false);
    }
    
    public static String getPackageName(String classname) {
        int slindex = classname.lastIndexOf(FORWARD_SLASH);
        if (slindex <= 0) {
            return "";
        }
        String pkgname = classname.substring(0, slindex);
        PACKAGE_NAME.validate(pkgname);
        return pkgname;
    }
    
    public static OwnerNameDesc getFieldDesc(String mname, String desc, String classname) {
        int slindex = mname.lastIndexOf(FORWARD_SLASH);
        String owner;
        String name;
        if (slindex >= 0) {
            owner = mname.substring(0,slindex);
            name = mname.substring(slindex+1);
        } else {
            LOG(M177); // "classname has been added to argument of incomplete field access instruction(s)"
            owner = classname;
            name = mname;
        }
        CLASS_NAME.validate(owner);
        FIELD_NAME.validate(name);
        FIELD_DESC.validate(desc);
        return new OwnerNameDesc(owner,name,desc,false);
    }
    
    public static OwnerNameDesc getClassOrMethodDesc(String mspec) {
        int lbindex = mspec.indexOf(LEFT_BRACKET);
        if (lbindex >= 0) {
            boolean ownerInterface = false;
            if (mspec.charAt(0) == INTERFACE_PREFIX) {
                mspec = mspec.substring(1);
                ownerInterface = true;
            }
            return getInstanceOfObjectMethod(mspec, ownerInterface);
        }
        CLASS_NAME.validate(mspec);
        return new OwnerNameDesc(mspec, null, null,false);
    }

    public static OwnerNameDesc getOwnerMethodDescAndCheck(String mspec, HandleType ht) {
        return getOwnerMethodDescAndCheck(mspec, ht.op());
    }

    public static OwnerNameDesc getOwnerMethodDescAndCheck(String mspec, AsmOp op) {
        boolean ownerInterface = false;
        if (mspec.charAt(0) == INTERFACE_PREFIX) {
            mspec = mspec.substring(1);
            ownerInterface = true;
        }
        ownerInterface = checkInterface(op, ownerInterface);
        mspec = addClassName(mspec, op);
        return getInstanceOfObjectMethod(mspec, ownerInterface);
    }
    
    private static String addClassName(String mspec, AsmOp op) {
        int lb = mspec.indexOf('(');
        int sl = mspec.indexOf('/');
        if (sl  < 0 || sl > lb) {
            LOG(M255,op); // "classname has been added to argument of some %s instruction(s)"
            mspec = CLASS_NAME() + "/" + mspec;
        }
        return mspec;
    }
    
    private static boolean checkInterface(AsmOp op, boolean ownerInterface) {
        switch(op) {
            case asm_invokeinterface:
                if (!ownerInterface){
                    LOG(M135,INTERFACE_PREFIX,op);   // "for consistency add %s prefix to method name for %s"
                }
                ownerInterface = true;
                break;
            case asm_invokevirtual:
                if (ownerInterface) {
                    LOG(M139,INTERFACE_PREFIX,op);   // "%s prefix is invalid for %s"
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
                    LOG(M139,INTERFACE_PREFIX,op);   // "%s prefix is invalid for %s"
                }
                ownerInterface = false;
                break;
        }
        return ownerInterface;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof OwnerNameDesc) {
            OwnerNameDesc other = (OwnerNameDesc)obj;
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


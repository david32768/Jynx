package jynx2asm.handles;

import java.util.EnumMap;
import java.util.EnumSet;

import static jynx.Global.LOG;
import static jynx.Message.M299;
import static jynx.Message.M300;
import static jynx.Message.M305;
import static jynx2asm.NameDesc.PACKAGE_NAME;

import jvm.Constants;
import jvm.HandleType;
import jynx.Global;
import jynx.LogIllegalArgumentException;

public enum HandlePart {

    HANDLE_TYPE,
    INTERFACE,
    OWNER,
    NAME,
    DESC,
    ;
    
    public static final char INTERFACE_PREFIX = '@';

    private static final char LEFT_BRACKET = '(';
    private static final char FORWARD_SLASH = '/';
    private static final char NAME_SEP = '.';

    public static EnumMap<HandlePart,String>  getInstance(String spec, EnumSet<HandlePart> expected) {
        String htype = null;
        int index = spec.indexOf(HandleType.SEP);
        if (index >= 0) {
            htype = spec.substring(0,index);
        }
        String itf = null;;
        if (spec.charAt(0) == INTERFACE_PREFIX) {
            spec = spec.substring(1);
            itf = "" + INTERFACE_PREFIX;
        }
        int lbindex = spec.indexOf(LEFT_BRACKET);
        String mname = spec;
        String mdesc = null;
        if (lbindex >= 0) {
            mname = spec.substring(0,lbindex);
            mdesc = spec.substring(lbindex);
        }
        int slindex = mname.lastIndexOf(NAME_SEP);
        if (slindex < 0) {
            slindex = mname.lastIndexOf(FORWARD_SLASH);
            if (slindex >= 0) {
                // "it is preferred that name is separated from owner by '%c' not '%c'"
                LOG(M305,NAME_SEP,FORWARD_SLASH);
            }
        }
        String mclass = null;
        if (slindex >= 0) {
            mclass = mname.substring(0,slindex);
            mname = mname.substring(slindex+1);
        }
        EnumMap<HandlePart,String> result = new EnumMap<>(HandlePart.class);
        if (mclass != null) {
            mclass = Global.TRANSLATE_OWNER(mclass);
            result.put(OWNER, mclass);
        }
        if (mname != null) {
            result.put(NAME, mname);
        }
        if (mdesc != null) {
            mdesc = Global.TRANSLATE_DESC(mdesc);
            result.put(DESC, mdesc);
        }
        if (itf != null) {
            result.put(INTERFACE, itf);
        }
        if (htype != null) {
            result.put(HANDLE_TYPE, htype);
        }
        for (HandlePart part:values()) {
            boolean inresult = result.containsKey(part);
            boolean inexpected = expected.contains(part);
            if (inresult && !inexpected) {
                Global.LOG(M299,part,result.get(part)); // "Unexpected %s %s removed"
                result.remove(part);
            } else if(!inresult && inexpected) {
                switch(part) {
                    case INTERFACE:
                        break;
                    case OWNER:
                        result.put(OWNER, Global.CLASS_NAME());
                        break;
                    default:
                        throw new LogIllegalArgumentException(M300, part); // "required %s is missing"
                }
            }
        }
        return result;
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

    public static boolean isSamePackage(String class1, String class2) {
        return packageNameOf(class1).equals(packageNameOf(class2));
    }
    
    private static final String VOID_METHOD = ")V";

    public static boolean isInit(String name,String desc) {
        return Constants.CLASS_INIT_NAME.equalString(name) && desc.endsWith(VOID_METHOD);
    }
    
    public static String ownerName(String owner, String name) {
        return owner + NAME_SEP + name;
    }
}

package jynx2asm;

import static jynx.Message.M145;
import static jynx2asm.NameDesc.METHOD_NAME;
import static jynx2asm.NameDesc.METHOD_NAME_DESC;

import jynx.LogIllegalArgumentException;

public class MethodDesc extends OwnerNameDesc {

    private MethodDesc(String name, String desc) {
        super(null,name,desc,false);
    }

   
    public static MethodDesc getInstance(String mspec) {
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
        return new MethodDesc(mname,mdesc);
    }
    
}

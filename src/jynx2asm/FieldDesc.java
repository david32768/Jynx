package jynx2asm;

import static jynx.Global.LOG;
import static jynx.Message.M177;
import static jynx2asm.NameDesc.CLASS_NAME;
import static jynx2asm.NameDesc.FIELD_DESC;
import static jynx2asm.NameDesc.FIELD_NAME;
import static jynx2asm.OwnerNameDesc.FORWARD_SLASH;

import jynx.Global;

public class FieldDesc extends OwnerNameDesc {
    
    private FieldDesc(String owner, String name, String desc) {
        super(owner,name,desc,false);
    }
    
    public static FieldDesc getInstance(String mname, String desc) {
        int slindex = mname.lastIndexOf(FORWARD_SLASH);
        String owner;
        String name;
        if (slindex >= 0) {
            owner = mname.substring(0,slindex);
            name = mname.substring(slindex+1);
        } else {
            LOG(M177); // "classname has been added to argument of incomplete field access instruction(s)"
            owner = Global.CLASS_NAME();
            name = mname;
        }
        CLASS_NAME.validate(owner);
        FIELD_NAME.validate(name);
        FIELD_DESC.validate(desc);
        return new FieldDesc(owner,name,desc);
    }
    
}

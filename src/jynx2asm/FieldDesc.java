package jynx2asm;

import static jynx.Global.LOG;
import static jynx.Global.OPTION;
import static jynx.Message.M255;
import static jynx2asm.NameDesc.CLASS_NAME;
import static jynx2asm.NameDesc.FIELD_DESC;
import static jynx2asm.NameDesc.FIELD_NAME;
import static jynx2asm.OwnerNameDesc.FORWARD_SLASH;

import jvm.AsmOp;
import jynx.Global;
import jynx.GlobalOption;

public class FieldDesc extends OwnerNameDesc {
    
    private FieldDesc(String owner, String name, String desc) {
        super(owner,name,desc,false);
    }
    
    public static FieldDesc getInstance(String mname, String desc, AsmOp asmop) {
        int slindex = mname.lastIndexOf(FORWARD_SLASH);
        String owner;
        String name;
        if (slindex >= 0) {
            owner = mname.substring(0,slindex);
            name = mname.substring(slindex+1);
        } else if (OPTION(GlobalOption.PREPEND_CLASSNAME)) {
            // "classname has been added to argument of some %s instruction(s)"
            LOG(M255,asmop);
            owner = Global.CLASS_NAME();
            name = mname;
        } else {
            owner = null;
            name = mname;
        }
        CLASS_NAME.validate(owner);
        FIELD_NAME.validate(name);
        FIELD_DESC.validate(desc);
        return new FieldDesc(owner,name,desc);
    }
    
}

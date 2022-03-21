package jynx2asm;

import static jynx.Global.LOG;
import static jynx.Global.OPTION;
import static jynx.Message.M255;
import static jynx2asm.NameDesc.CLASS_NAME;
import static jynx2asm.NameDesc.FIELD_DESC;
import static jynx2asm.NameDesc.FIELD_NAME;

import jvm.AsmOp;
import jynx.Global;
import jynx.GlobalOption;

public class FieldDesc extends OwnerNameDesc {
    
    private FieldDesc(ONDRecord ond) {
        super(ond);
        assert !ond.isInterface() && ond.desc() != null && !ond.hasParameters();
    }
    
    public static FieldDesc getInstance(String mname, String desc, AsmOp asmop) {
        ONDRecord ond = ONDRecord.getInstance(mname);
        ond = ond.changeDesc(desc);
        if (ond.owner() == null && OPTION(GlobalOption.PREPEND_CLASSNAME)) {
            // "classname has been added to argument of some %s instruction(s)"
            LOG(M255,asmop);
            ond = ond.changeOwner(Global.CLASS_NAME());
        }
        CLASS_NAME.validate(ond.owner());
        FIELD_NAME.validate(ond.name());
        FIELD_DESC.validate(ond.desc());
        return new FieldDesc(ond);
    }
    
}

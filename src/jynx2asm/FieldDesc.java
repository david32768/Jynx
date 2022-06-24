package jynx2asm;

import static jynx.Message.M137;
import static jynx.Message.M275;
import static jynx2asm.NameDesc.CLASS_NAME;
import static jynx2asm.NameDesc.FIELD_DESC;
import static jynx2asm.NameDesc.FIELD_NAME;

import jvm.AsmOp;
import jvm.Context;
import jvm.HandleType;
import jynx.LogIllegalArgumentException;

public class FieldDesc extends OwnerNameDesc {

    private FieldDesc(ONDRecord ond) {
        super(ond);
        assert !ond.isInterface() && ond.desc() != null && !ond.hasParameters();
    }
    
    public static FieldDesc getInstance(String mname, String desc, AsmOp asmop) {
        ONDRecord ond = ONDRecord.getInstance(mname);
        ond = ond.changeDesc(desc);
        ond = ond.addClassName(asmop);
        CLASS_NAME.validate(ond.owner());
        FIELD_NAME.validate(ond.name());
        FIELD_DESC.validate(ond.desc());
        return new FieldDesc(ond);
    }
    
    public static FieldDesc getInstance(String name, String desc) {
        ONDRecord ond = ONDRecord.getInstance(name);
        ond = ond.changeDesc(desc);
        if (ond.desc() == null || ond.name() == null || ond.name().isEmpty() || ond.owner() != null) {
            // "invalid %s description %s %s"
            throw new LogIllegalArgumentException(M137,Context.FIELD,name,desc);
        }
        FIELD_NAME.validate(ond.name());
        FIELD_DESC.validate(ond.desc());
        return new FieldDesc(ond);
    }

    public static FieldDesc getInstance(String mspec, HandleType ht) {
        ONDRecord ond = ONDRecord.getInstance(mspec);
        String desc = ond.desc();
        if (!desc.startsWith("()")) {
            // "descriptor '%s' for %s must start with '()'"
            throw new LogIllegalArgumentException(M275, desc , ht);
        }
        desc = desc.substring(2);
        return getInstance(ond.ownerName(),desc,ht.op());
    }
}

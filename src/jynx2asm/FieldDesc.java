package jynx2asm;

import static jynx.Message.M137;
import static jynx.Message.M275;
import static jynx2asm.NameDesc.CLASS_NAME;
import static jynx2asm.NameDesc.FIELD_DESC;
import static jynx2asm.NameDesc.FIELD_NAME;

import jvm.Context;
import jvm.HandleType;
import jynx.LogIllegalArgumentException;

public class FieldDesc extends OwnerNameDesc {

    private FieldDesc(ONDRecord ond) {
        super(ond);
        assert ond.isField();
    }
    
    public static FieldDesc getInstance(String name, String desc) {
        ONDRecord ond = ONDRecord.getInstance(name,desc);
        ond = ond.translateOwner();
        if (ond.owner() == null || !ond.isField()) {
            // "invalid %s description %s %s"
            throw new LogIllegalArgumentException(M137,Context.FIELD,name,desc);
        }
        CLASS_NAME.validate(ond.owner());
        FIELD_NAME.validate(ond.name());
        FIELD_DESC.validate(ond.desc());
        return new FieldDesc(ond);
    }
    
    public static FieldDesc getLocalInstance(String name, String desc) {
        ONDRecord ond = ONDRecord.getInstance(name,desc);
        if (ond.owner() != null || !ond.isField()) {
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
        if (!ond.hasEmptyParm()) {
            // "descriptor '%s' for %s must start with '()'"
            throw new LogIllegalArgumentException(M275, desc , ht);
        }
        desc = desc.substring(2);
        return getInstance(ond.ownerName(),desc);
    }
}

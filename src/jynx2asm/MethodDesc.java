package jynx2asm;

import org.objectweb.asm.tree.MethodNode;

import static jynx.Message.M145;
import static jynx2asm.NameDesc.METHOD_NAME;
import static jynx2asm.NameDesc.METHOD_NAME_DESC;

import jynx.LogIllegalArgumentException;

public class MethodDesc extends OwnerNameDesc {

    private MethodDesc(ONDRecord ond) {
        super(ond);
        assert ond.owner() == null && !ond.isInterface() && ond.hasParameters();
    }

   
    public static MethodDesc of(MethodNode mn) {
        return new MethodDesc(ONDRecord.of(mn));
    }
    
    public static MethodDesc getInstance(String mspec) {
        ONDRecord ond = ONDRecord.getInstance(mspec);
        if (ond.desc() == null || ond.name() == null || ond.name().isEmpty() || ond.owner() != null) {
            // "Invalid method description %s"
            throw new LogIllegalArgumentException(M145,mspec);
        }
        METHOD_NAME.validate(ond.name());
        METHOD_NAME_DESC.validate(ond.nameDesc());
        return new MethodDesc(ond);
    }
    
}

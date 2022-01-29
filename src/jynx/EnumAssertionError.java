package jynx;

import static jynx.Global.LOG;
import static jynx.Message.M900;

public class EnumAssertionError extends AssertionError {
    
    public EnumAssertionError(Enum<?> enumx) {
        LOG(M900,enumx,enumx.getClass()); //  // "unknown enum constant %s in enum %s"
    }

}

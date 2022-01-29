package jynx;

import static jynx.Global.LOG;

public class LogAssertionError extends AssertionError {
    
    public LogAssertionError(Message msg,Object... args) {
        LOG(msg,args);
    }

}

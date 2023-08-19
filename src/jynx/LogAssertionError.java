package jynx;

import static jynx.Global.LOG;

public class LogAssertionError extends AssertionError {

    private static final long serialVersionUID = 1L;
    
    public LogAssertionError(Message msg,Object... args) {
        LOG(msg,args);
    }

}

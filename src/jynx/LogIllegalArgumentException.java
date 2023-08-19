package jynx;

import static jynx.Global.LOG;

public class LogIllegalArgumentException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;
    
    public LogIllegalArgumentException(Message msg,Object... args) {
        LOG(msg,args);
    }

}

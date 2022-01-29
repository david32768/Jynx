package jynx;

import static jynx.Global.LOG;

public class LogIllegalStateException extends IllegalStateException {

    public LogIllegalStateException(Message msg,Object... args) {
        LOG(msg,args);
    }

}

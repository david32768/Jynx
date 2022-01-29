package jynx;

import static jynx.Global.LOG;

public class LogIllegalArgumentException extends IllegalArgumentException {

    public LogIllegalArgumentException(Message msg,Object... args) {
        LOG(msg,args);
    }

}

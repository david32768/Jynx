package jynx;

import static jynx.Global.OPTION;
import static jynx.GlobalOption.DEBUG;

public class Constants {
    
    public final static String SUFFIX = ".jx";

    private final static int JYNX_VERSION = 0;
    private final static int JYNX_RELEASE = 20;
    private final static int JYNX_BUILD = 2;
    
    public static String version() {
        if (OPTION(DEBUG)) {
            return String.format("%d+%d-%d",JYNX_VERSION,JYNX_RELEASE,JYNX_BUILD);
        } else {
            return String.format("%d.%d",JYNX_VERSION,JYNX_RELEASE);
        }
    }

}

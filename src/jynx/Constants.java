package jynx;

public class Constants {
    
    public final static String SUFFIX = ".jx";

    private final static int JYNX_VERSION = 0;
    private final static int JYNX_RELEASE = 20;
    private final static int JYNX_BUILD = 9;
    
    public static String version(boolean exact) {
        if (exact) {
            return String.format("%d+%d-%d",JYNX_VERSION,JYNX_RELEASE,JYNX_BUILD);
        } else {
            return String.format("%d.%d",JYNX_VERSION,JYNX_RELEASE);
        }
    }

}

package jynx;

public class Constants {
    
    public final static String SUFFIX = ".jx";

    private final static int JYNX_VERSION = 0;
    private final static int JAVA_RELEASE = 22;
    private final static int JYNX_BUILD = 8;
    
    public static String version(boolean exact) {
        if (exact) {
            return String.format("%d+%d-%d",JYNX_VERSION,JAVA_RELEASE,JYNX_BUILD);
        } else {
            return String.format("%d.%d",JYNX_VERSION,JAVA_RELEASE);
        }
    }

}

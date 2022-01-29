package jynx;

public enum LogMsgType {

    // errors in ascending severity
    BLANK(' '),
    ENDINFO('C'),
    INFO('I'),
    STYLE('T'),
    WARNING('W'),
    ERROR('E'),
    SEVERE('S'),
    ;
    
    private final char abbrev;

    private LogMsgType(char abbrev) {
        this.abbrev = abbrev;
    }

    public String prefix(String msgname) {
        if (abbrev == ' ') {
            return "";
        }
        return abbrev + msgname.substring(1) + ": ";
    }
    
}

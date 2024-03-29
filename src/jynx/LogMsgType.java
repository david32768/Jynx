package jynx;

public enum LogMsgType {

    // errors in ascending severity
    FINEST('t'),
    FINER('r'),
    FINE('e'),
    BLANK(' '),
    ENDINFO('C'),
    INFO('I'),
    LINE('L'),
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

    public LogMsgType up() {
        switch(this) {
            case SEVERE:
                return this;
            case INFO:
                return WARNING;
            default:
                return values()[ordinal() + 1];
        }
    }
}

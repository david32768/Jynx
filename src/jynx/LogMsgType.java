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
            case FINEST:
            case FINER:
            case FINE:
            case ENDINFO:
                return LINE;
            case WARNING:
                return ERROR;
            case ERROR:
                return SEVERE;
            case INFO:
            case LINE:
            case BLANK:
            case STYLE:
            case SEVERE:
                return this;
            default:
                throw new EnumConstantNotPresentException(this.getClass(), this.name());
        }
    }

    public LogMsgType supress() {
        switch(this) {
            case FINEST:
            case FINER:
            case FINE:
            case ENDINFO:
            case WARNING:
            case INFO:
            case LINE:
            case STYLE:
                return FINEST;
            case BLANK:
            case ERROR:
            case SEVERE:
                return this;
            default:
                throw new EnumConstantNotPresentException(this.getClass(), this.name());
        }
    }

}

package jynx;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import static jynx.Global.OPTION;

import static jynx.Message.*;

public class Logger {

    private static final int MAX_ERRORS = 5;

    private final Deque<String> contexts;
    private final Deque<String> lines;
    private final Set<String> endinfo;
    private final boolean exitOnError;

    private String currentLine;
    private String lastErrorLine;
    
    private int errct = 0;

    public Logger(boolean exitOnError) {
        this.exitOnError = exitOnError;
        this.contexts = new ArrayDeque<>();
        this.lines = new ArrayDeque<>();
        this.endinfo = new LinkedHashSet<>(); // so order of info messages is reproducible
    }

    public int numErrors() {
        return errct;
    }

    public void setLine(String line) {
        this.currentLine = line;
    }
    
    public void pushContext() {
        contexts.push(currentLine); // addFirst
    }
    
    public void popContext() {
        String line = contexts.pop(); // removeFirst
    }

    public void pushCurrent() {
        lines.push(currentLine); // addFirst
    }
    
    public void popCurrent() {
        currentLine = lines.pop(); // removeFirst
    }

    private void printLineMessage(String fmt, Object... args) {
        if (Objects.equals(currentLine,lastErrorLine)) {
        } else {
            System.err.println();
            String context = contexts.peekFirst();
            if (context != null && !Objects.equals(context,currentLine)) {
                System.err.println(context);
            }
            if (currentLine != null) {
                System.err.println(currentLine);
            }
        }
        lastErrorLine = currentLine;
        System.err.format(fmt, args);
        System.err.println();
    }

    private void printError(String fmt, Object... args) {
        printLineMessage(fmt, args);
        errct++;
    }

    private void printInfo(String fmt, Object... args) {
        System.err.format(fmt,args);
        System.err.println();
    }
    
    private void addEndInfo(String fmt, Object... args) {
        endinfo.add(String.format(fmt, args));
    }
    
    boolean printEndInfo(String classname){
        System.err.println();
        for (String msg:endinfo) {
            printInfo(msg);
        }
        if (!endinfo.isEmpty()) {
            System.err.println();
        }
        endinfo.clear();
        if (errct == 0) {
            printInfo(M104.format(classname)); // "class %s assembly completed successfully"
        } else {
            printInfo(M131.format(classname,errct)); // "class %s assembly completed  unsuccesfully - number of errors is %d"
        }
        currentLine = null;
        return errct == 0;
    }

    void log(LogMsgType logtype, Message msg, Object... objs) {
        String message = msg.format(objs);
        switch (logtype) {
            case SEVERE:
                printError(message);
                printInfo(M84.format()); // "assembly terminated because of severe error"
                throw new SevereError();
            case STYLE:
                // fall through to warning
            case WARNING:
                printLineMessage(message);
                break;
            case ERROR:
                printError(message);
                if (exitOnError) {
                    if (OPTION(GlobalOption.__EXIT_IF_ERROR)) {
                        (new Exception()).printStackTrace();
                    }
                    System.exit(1);
                }
                if (errct > MAX_ERRORS) {
                    printInfo(M85.format()); // "assembly terminated because of too many errors"
                    throw new SevereError();
                }
                break;
            case ENDINFO:
                addEndInfo(message);
                break;
            case INFO:
                printInfo(message);
                break;
            case BLANK:
                printInfo(message);
                break;
            default:
                throw new EnumAssertionError(logtype);
        }
        
    }

    void log(String line, LogMsgType logtype, Message msg, Object... objs) {
        String savedline = currentLine;
        setLine(line);
        log(logtype,msg,objs);
        currentLine = savedline;
    }

    void log(Exception ex) {
        printError("%s",ex);
        if (exitOnError) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

}

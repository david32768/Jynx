package jynx;

import java.util.EnumSet;
import java.util.Objects;

import static jynx.Message.M24;

import jvm.ConstantPoolType;
import jvm.JvmVersion;
import jvm.JvmVersioned;
import jynx2asm.Line;

public class Global {

    private final Logger logger;
    private final EnumSet<GlobalOption> options;
    private JvmVersion jvmVersion;

    private Global() {
        this.options = EnumSet.noneOf(GlobalOption.class);
        this.logger  = new Logger(false);
        this.jvmVersion = null;
    }

    private Global(EnumSet<GlobalOption> options) {
        this.options = options;
        boolean exiterr = options.contains(GlobalOption.__EXIT_IF_ERROR);
        this.logger  = new Logger(exiterr);
        this.jvmVersion = null;
    }
    
    private static Global global = new Global();
    
    public static Logger LOGGER() {
        return global.logger;
    }

    public static GlobalOption resolveAmbiguity(GlobalOption defaultopt, GlobalOption... otheropt) {
        EnumSet<GlobalOption> optpos = EnumSet.of(defaultopt,otheropt);
        optpos.retainAll(global.options);
        switch (optpos.size()) {
            default:
                LOG(M24,optpos,defaultopt); // "ambiguous option %s: %s assumed"
                global.options.removeAll(optpos);
                // FALL THROUGH
            case 0:
                ADD_OPTION(defaultopt);
                return defaultopt;
            case 1:
                return (GlobalOption)optpos.toArray()[0];
        }
    }


    static void newGlobal(EnumSet<GlobalOption> options) {
        global = new Global(options);
    }
    
    public static void setJvmVersion(JvmVersion jvmversion) {
        assert global.jvmVersion == null;
        global.jvmVersion = jvmversion;
    }
    
    public static JvmVersion JVM_VERSION() {
        Objects.nonNull(global.jvmVersion);
        return global.jvmVersion;
    }
    
    public static boolean CHECK_SUPPORTS(JvmVersioned feature) {
        if (feature != null && global.jvmVersion != null) {
            return global.jvmVersion.checkSupports(feature);
        }
        return true;
    }
    
    public static boolean CHECK_CAN_LOAD(ConstantPoolType cp) {
        if (global.jvmVersion != null) {
            return cp.isLoadableBy(global.jvmVersion);
        }
        return true;
    }
    
    public static boolean SUPPORTS(JvmVersioned feature) {
        if (feature != null && global.jvmVersion != null) {
            return global.jvmVersion.supports(feature);
        }
        return feature == null;
    }
    
    public static boolean ADD_OPTION(GlobalOption option) {
        return global.options.add(option);
    }
    
    public static boolean OPTION(GlobalOption option) {
        return global.options.contains(option);
    }
    
    public static EnumSet<GlobalOption> OPTIONS() {
        return global.options.clone();
    }
    
    private static LogMsgType msgType(Message msg) {
        LogMsgType logtype = msg.logtype();
        if (logtype.compareTo(LogMsgType.STYLE) >= 0 && OPTION(GlobalOption.__TREAT_WARNINGS_AS_ERRORS)) {
            logtype = LogMsgType.ERROR;
        }
        return logtype;
    }
    
    public static void LOG(Message msg,Object... objs) {
        global.logger.log(msgType(msg),msg,objs);
    }

    public static void LOG(Line line, Message msg, Object... objs) {
        global.logger.log(line.toString(),msgType(msg),msg, objs);
    }

    public static void LOG(Exception ex) {
        if (OPTION(GlobalOption.__PRINT_STACK_TRACES)) {
            ex.printStackTrace();;
        }
        if (ex instanceof LogIllegalArgumentException) {
            return; // already logged
        }
        if (ex instanceof LogIllegalStateException) {
            return; // already logged
        }
        global.logger.log(ex);
    }

    public static boolean END_MESSAGES(String classname) {
        return global.logger.printEndInfo(classname);
    }
    
}

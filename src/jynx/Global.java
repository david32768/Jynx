package jynx;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.function.UnaryOperator;
import java.util.Objects;
import java.util.Optional;

import static jynx.Message.M218;
import static jynx.Message.M219;
import static jynx.Message.M24;
import static jynx.Message.M32;
import static jynx.Message.M4;

import com.github.david32768.jynx.Main;
import jvm.ConstantPoolType;
import jvm.JvmVersion;
import jvm.JvmVersioned;
import jynx2asm.Line;

public class Global {

    private final Logger logger;
    private final EnumSet<GlobalOption> options;
    private JvmVersion jvmVersion;
    private String classname;
    private final Main.MainOption main;
    private UnaryOperator<String> parmtrans;
    
    private Global() {
        this.options = EnumSet.noneOf(GlobalOption.class);
        this.logger  = new Logger("",false);
        this.jvmVersion = null;
        this.classname = null;
        this.main = null;
        this.parmtrans = null;
    }

    private Global(EnumSet<GlobalOption> options,Main.MainOption type) {
        this.options = options;
        this.logger  = new Logger(type.name().toLowerCase(),true);
        this.jvmVersion = null;
        this.main = type;
        this.parmtrans = null;
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


    public static void newGlobal(Main.MainOption type, EnumSet<GlobalOption> options) {
        global = new Global(options,type);
        // "%n%s; Java runtime version %s"
        LOG(M4,type.version(),System.getProperty("java.runtime.version"));
    }
    
    public static void setJvmVersion(JvmVersion jvmversion) {
        assert global.jvmVersion == null;
        global.jvmVersion = jvmversion;
    }
    
    public static void setClassName(String classname) {
        assert global.classname == null;
        global.classname = classname;
    }
    
    public static JvmVersion JVM_VERSION() {
        Objects.nonNull(global.jvmVersion);
        return global.jvmVersion;
    }
    
    public static String CLASS_NAME() {
        Objects.nonNull(global.classname);
        return global.classname;
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
    
    public static Optional<String> setOptions(String[] args) {
        int i = 0;
        String[] remainder = new String[0];
        for (; i < args.length; ++i) {
            String argi = args[i];
            if (argi.isEmpty()) {
                continue;
            }
            if (GlobalOption.mayBeOption(argi)) {
                Optional<GlobalOption> opt = GlobalOption.optArgInstance(argi);
                if (opt.isPresent()) {
                    GlobalOption option = opt.get();
                    ADD_OPTION(option);
                } else {
                    LOG(M32,argi); // "%s is not a valid option"
                }
            } else {
                remainder = Arrays.copyOfRange(args, i, args.length);
                if (remainder.length == 1) {
                    return Optional.of(args[i]);
                }
                break;
            }
        }
        if (remainder.length == 0) {
            LOG(M218); //"SYSIN will be used as input"
        } else {
            LOG(M219,Arrays.asList(remainder)); // "wrong number of parameters after options %s"
        }
        return Optional.empty();
    }

    private static LogMsgType msgType(Message msg) {
        LogMsgType logtype = msg.getLogtype();
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

    public static void LOG(String linestr, Message msg, Object... objs) {
        global.logger.log(linestr,msgType(msg),msg, objs);
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
    
    public static Main.MainOption MAIN_OPTION() {
        return global.main;
    }

    public static void setParmTrans(UnaryOperator<String> parmtrans) {
        assert global.parmtrans == null;
        global.parmtrans = parmtrans;
    }
    
    public static String TRANSLATE(String string) {
        if (string == null || global.parmtrans == null || !string.startsWith("(") || !string.contains("->")) {
            return string;
        } else {
            return global.parmtrans.apply(string);
        }
    }
}

package com.github.david32768.jynx;

import java.util.Arrays;
import java.util.Optional;

import static jynx.Global.*;
import static jynx.GlobalOption.*;
import static jynx.Message.*;

import jvm.JvmVersion;
import jynx.Global;
import jynx.GlobalOption;
import jynx.MainOption;
import jynx.SevereError;

public class Main {
    
    private static void outputVersion(MainOption main) {
        // // "Jynx version %s; maximum Java version is %s"
        LOG(M0, main.version(), JvmVersion.MAX_VERSION);
    }

    private static void appUsage() {
        LOG(M12); // "%nUsage:%n"
        LOG(M5, GlobalOption.VERSION.description()); // "  %s%n"
        LOG(M5, GlobalOption.HELP.description());
        for (MainOption mo: MainOption.values()) {
            mo.appUsageSummary();
        }
        String mainstr = MainOption.mains();
        LOG(M24,mainstr,GlobalOption.HELP.asArg()); // "%s %s     gives list of options"
    }

    private static void appUsage(MainOption mo) {
        LOG(M12); // "%nUsage:%n"
        mo.appUsage();
    }

    private static Optional<MainOption> getMainOption(String[] args) {
        if (args.length == 0) {
            appUsage();
            return Optional.empty();
        }
        String option = args[0];
        if (args.length == 1) {
            if (VERSION.isArg(option)) {
                outputVersion(MainOption.ASSEMBLY);
                return Optional.empty();
            }
            if (HELP.isArg(option)) {
                appUsage();
                return Optional.empty();
            }
        }
        Optional<MainOption> mainopt = MainOption.getInstance(option);
        if (!mainopt.isPresent()) {
            LOG(M26,option); // "invalid main-option name - %s"
            return Optional.empty();
        }
        MainOption main = mainopt.get();
        if (args.length == 1) {
            // "no args have been specified for main option %s"
            LOG(M28,main.extname());
            appUsage(main);
            return Optional.empty();
        }
        option = args[1];
        if (args.length == 2) {
            if (VERSION.isArg(option)) {
                outputVersion(main);
                return Optional.empty();
            }
            if (HELP.isArg(option)) {
                appUsage(main);
                return Optional.empty();
            }
        }
        return mainopt;
    }
    
    private static boolean mainz(String[] args) {
        Optional<MainOption> optmain = getMainOption(args);
        if (!optmain.isPresent()) {
            return false;
        }
        MainOption main = optmain.get();
        args = Arrays.copyOfRange(args, 1, args.length);
        newGlobal(main);
        Optional<String> optname = setOptions(args);
        if (LOGGER().numErrors() != 0) {
            LOG(M3); // "program terminated because of errors"
            appUsage();
            return false;
        }
        boolean success;
        try {
            success = main.fn().test(optname);
        } catch (SevereError ex) {
            if (OPTION(GlobalOption.DEBUG)) {
                ex.printStackTrace();;
            }
            success = false;
        }
        if (!success) {
            String classname = CLASS_NAME();
            if (classname == null) {
                classname = optname.orElse("SYSIN");
            }
            LOG(M298,main.name(),classname); // "%s of %s failed"
        }
        return success;
    }
    
    public static void main(String[] args) {
        boolean success = mainz(args);
        if (!success) {
            System.exit(1);
        }
    }
    
}

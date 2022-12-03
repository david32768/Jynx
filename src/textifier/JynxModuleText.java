package textifier;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.objectweb.asm.util.Printer;

import static jvm.Context.MODULE;

import static jynx.Directive.dir_exports;
import static jynx.Directive.dir_main;
import static jynx.Directive.dir_module_info;
import static jynx.Directive.dir_opens;
import static jynx.Directive.dir_packages;
import static jynx.Directive.dir_provides;
import static jynx.Directive.dir_requires;
import static jynx.Directive.dir_uses;
import static jynx.ReservedWord.res_to;
import static jynx.ReservedWord.res_with;

import asm2jynx.JynxStringBuilder;
import jvm.AccessFlag;
import jvm.Context;
import jvm.JvmVersion;

public class JynxModuleText extends AbstractPrinter {

    private final JynxStringBuilder jsb;
    private final List<String> packages;

    private JvmVersion jvmVersion;

    public JynxModuleText(JvmVersion jvmVersion, JynxStringBuilder jsb) {
        super();
        this.jsb = jsb;
        this.jvmVersion = jvmVersion;
        this.packages = new ArrayList<>();
    }

    @Override
    public Printer visitModule(String name, int access, String version) {
        EnumSet<AccessFlag> accflags = AccessFlag.getEnumSet(access, MODULE,jvmVersion);
        jsb.start(0)
                .append(dir_module_info)
                .appendFlags(accflags)
                .appendName(name)
                .append(version)
                .nl();
        return this;
    }

    @Override
    public void visitMainClass(String mainClass) {
        jsb.start(0)
                .appendDir(dir_main,mainClass);
    }

    @Override
    public void visitRequire(String require, int access, String version) {
        EnumSet<AccessFlag> accflags = AccessFlag.getEnumSet(access, Context.REQUIRE, jvmVersion);
        jsb.start(0)
                .append(dir_requires)
                .appendFlags(accflags)
                .appendName(require)
                .appendNonNull(version)
                .nl();
    }

    @Override
    public void visitExport(String packaze, int access, String... modules) {
        EnumSet<AccessFlag> accflags = AccessFlag.getEnumSet(access, MODULE,jvmVersion);
        jsb.start(0)
                .append(dir_exports)
                .appendFlags(accflags)
                .appendName(packaze);
        if (modules != null && modules.length != 0) {
            jsb.append(res_to)
                    .appendDotArray(modules);
        } else {
            jsb.nl();
        }
    }

    @Override
    public void visitOpen(String packaze, int access, String... modules) {
        EnumSet<AccessFlag> accflags = AccessFlag.getEnumSet(access, MODULE,jvmVersion);
        jsb.start(0)
                .append(dir_opens)
                .appendFlags(accflags)
                .appendName(packaze);
        if (modules != null && modules.length != 0) {
            jsb.append(res_to)
                    .appendDotArray(modules);
        } else {
            jsb.nl();
        }
    }

    @Override
    public void visitUse(String use) {
        jsb.start(0)
                .appendDir(dir_uses,use);
    }

    @Override
    public void visitProvide(String provide, String... providers) {
            jsb.start(0)
                    .append(dir_provides)
                    .append(provide)
                    .append(res_with)
                    .appendDotArray(providers);
    }

    @Override
    public void visitPackage(String packaze) {
        packages.add(packaze);
    }

    @Override
    public void visitModuleEnd() {
        if (!packages.isEmpty()) {
            jsb.start(0)
                    .append(dir_packages)
                    .appendDotArray(packages);
        }
    }

}

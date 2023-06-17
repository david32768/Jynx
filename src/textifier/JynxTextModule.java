package textifier;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.objectweb.asm.util.Printer;

import static jvm.Context.MODULE;

import static jynx.Directive.dir_exports;
import static jynx.Directive.dir_main;
import static jynx.Directive.dir_module;
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
import jynx.Directive;

public class JynxTextModule extends AbstractPrinter {

    private final JynxStringBuilder jsb;
    private final List<String> packages;

    private JvmVersion jvmVersion;

    public JynxTextModule(JvmVersion jvmVersion, JynxStringBuilder jsb) {
        super();
        this.jsb = jsb;
        this.jvmVersion = jvmVersion;
        this.packages = new ArrayList<>();
    }

    @Override
    public Printer visitModule(String name, int access, String version) {
        EnumSet<AccessFlag> accflags = AccessFlag.getEnumSet(access, MODULE,jvmVersion);
        jsb.start(0)
                .append(dir_module)
                .appendFlags(accflags)
                .appendName(name)
                .append(version)
                .nl();
        return this;
    }

    @Override
    public void visitMainClass(String mainClass) {
        jsb.start(1)
                .appendDir(dir_main,mainClass);
    }

    @Override
    public void visitRequire(String require, int access, String version) {
        EnumSet<AccessFlag> accflags = AccessFlag.getEnumSet(access, Context.REQUIRE, jvmVersion);
        jsb.start(1)
                .append(dir_requires)
                .appendFlags(accflags)
                .appendName(require)
                .appendNonNull(version)
                .nl();
    }

    @Override
    public void visitExport(String packaze, int access, String... modules) {
        EnumSet<AccessFlag> accflags = AccessFlag.getEnumSet(access, MODULE,jvmVersion);
        jsb.start(1)
                .append(dir_exports)
                .appendFlags(accflags)
                .appendName(packaze)
                .appendRWArray(res_to, modules);
    }

    @Override
    public void visitOpen(String packaze, int access, String... modules) {
        EnumSet<AccessFlag> accflags = AccessFlag.getEnumSet(access, MODULE,jvmVersion);
        jsb.start(1)
                .append(dir_opens)
                .appendFlags(accflags)
                .appendName(packaze)
                .appendRWArray(res_to, modules);
    }

    @Override
    public void visitUse(String use) {
        jsb.start(1)
                .appendDir(dir_uses,use);
    }

    @Override
    public void visitProvide(String provide, String... providers) {
            jsb.start(1)
                    .append(dir_provides)
                    .append(provide)
                    .appendRWArray(res_with, providers);
    }

    @Override
    public void visitPackage(String packaze) {
        packages.add(packaze);
    }

    @Override
    public void visitModuleEnd() {
            jsb.start(1)
                    .appendDirArray(dir_packages, packages)
                    .start(0)
                    .append(Directive.end_module)
                    .nl();
    }

}

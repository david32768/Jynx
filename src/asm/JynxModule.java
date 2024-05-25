package asm;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.tree.ModuleNode;

import static jynx.Global.*;
import static jynx.Message.*;
import static jynx2asm.NameDesc.*;

import jvm.AccessFlag;
import jvm.Constants;
import jvm.Context;
import jvm.Feature;
import jvm.JvmVersion;
import jvm.StandardAttribute;
import jynx.Access;
import jynx.ClassType;
import jynx.Directive;
import jynx.LogAssertionError;
import jynx.LogIllegalStateException;
import jynx.ReservedWord;
import jynx2asm.Line;
import jynx2asm.NameDesc;
import jynx2asm.Token;
import jynx2asm.TokenArray;

public class JynxModule {
    
    private final ModuleNode modNode;
    private final JvmVersion jvmVersion;
    private final Map<String,EnumSet<Directive>> packageUse;
    private final Map<String,Line> providerUse;
    private final Map<String,Line> services;

    private boolean javaBase;
    private boolean packagesVisited;
    private final Map<Directive,Line> unique_directives;


    private JynxModule(ModuleNode modnode, JvmVersion jvmversion) {
        this.modNode = modnode;
        this.jvmVersion = jvmversion;
        this.packageUse = new HashMap<>();
        this.providerUse = new HashMap<>();
        this.services = new HashMap<>();
        this.javaBase = false;
        this.packagesVisited = false;
        this.unique_directives = new HashMap<>();
    }
    
    public static JynxModule getInstance(Line line, JvmVersion jvmversion) {
        EnumSet<AccessFlag> flags = line.getAccFlags();
        String name = line.nextToken().asName();
        Access accessname = Access.getInstance(flags, jvmversion, name, ClassType.MODULE_CLASS);
        MODULE_NAME.validate(name);
        Token token = line.nextToken();
        String version = token.isEndToken()?null:token.asString();
        line.noMoreTokens();
        accessname.check4Module();
        int access = accessname.getAccess();
        ModuleNode mnode = new ModuleNode(name, access, version);
        return new JynxModule(mnode, jvmversion);
    }

    private void checkPackage(String packaze, Directive dir) {
        boolean added = packageUse.computeIfAbsent(packaze, v -> EnumSet.noneOf(Directive.class))
                .add(dir);
        if (!added) {
            // "package %s has already appeared in %s"
            throw new LogIllegalStateException(M133,packaze,dir);
        }
    }
    
    private void checkClassPackage(String classname, Directive dir) {
        int index = classname.lastIndexOf("/"); // >= 0 as checked to be CLASS_NAME_IN_MODULE
        String packaze = classname.substring(0,index);
        packageUse.computeIfAbsent(packaze, v -> EnumSet.noneOf(Directive.class))
                .add(dir);
    }
    
    private Access getAccess(Line line) {
        EnumSet<AccessFlag> flags = line.getAccFlags();
        String name = line.nextToken().asName();
        return Access.getInstance(flags, jvmVersion, name, ClassType.MODULE_CLASS);
    }
    
    public void visitDirective(Directive dir, Line line) {
        dir.checkUnique(unique_directives, line);
        switch(dir) {
            case dir_main:
                visitMain(line);
                break;
            case dir_packages:
                visitPackages(line);
                break;
            case dir_uses:
                visitUses(line);
                break;
            case dir_exports:
                visitExports(line);
                break;
            case dir_opens:
                visitOpens(line);
                break;
            case dir_requires:
                visitRequires(line);
                break;
            case dir_provides:
                visitProviders(line);
                break;
            default:
                // "unknown directive %s for context %s"
                throw new LogAssertionError(M907,dir,Context.MODULE);
        }
    }
    
    private void visitMain(Line line) {
        String main = line.nextToken().asString();
        CHECK_SUPPORTS(StandardAttribute.ModuleMainClass);
        CLASS_NAME_IN_MODULE.validate(main);
        modNode.visitMainClass(main);
        checkClassPackage(main, Directive.dir_main);
    }
    
    private void visitRequires(Line line) {
        Access accessname = getAccess(line);
        String mod = accessname.name();
        MODULE_NAME.validate(mod);
        javaBase |= Constants.JAVA_BASE_MODULE.equalsString(mod);
        accessname.check4Require();
        int access = accessname.getAccess();
        Token token = line.nextToken();
        String version = token.isEndToken()?null:token.asString();
        line.noMoreTokens();
        modNode.visitRequire(mod, access, version);
    }

    private void visitExports(Line line) {
        Access accessname = getAccess(line);
        String packaze = accessname.name();
        checkPackage(packaze, Directive.dir_exports);
        PACKAGE_NAME.validate(packaze);
        accessname.check4Export();
        int access = accessname.getAccess();
        String[] modarr = new String[0];
        Token to = line.nextToken();
        if (!to.isEndToken()) {
            to.mustBe(ReservedWord.res_to);
            modarr = TokenArray.arrayString(Directive.dir_exports, line, MODULE_NAME);
        }
        modNode.visitExport(packaze,access,modarr);
    }

    private void visitOpens(Line line) {
        Access accessname = getAccess(line);
        String packaze = accessname.name();
        checkPackage(packaze, Directive.dir_opens);
        PACKAGE_NAME.validate(packaze);
        accessname.check4Open();
        int access = accessname.getAccess();
        String[] modarr = new String[0];
        Token to = line.nextToken();
        if (!to.isEndToken()) {
            to.mustBe(ReservedWord.res_to);
            modarr = TokenArray.arrayString(Directive.dir_opens, line, MODULE_NAME);
        }
        line.noMoreTokens();
        modNode.visitOpen(packaze,access,modarr);
    }

    private void visitUses(Line line) {
        String service = line.lastToken().asString();
        boolean ok = CLASS_NAME_IN_MODULE.validate(service);
        if (ok) {
            Line previous = services.put(service, line);
            if (previous == null) {
                checkClassPackage(service,Directive.dir_uses);
                modNode.visitUse(service);
            } else {
                LOG(M233,service,Directive.dir_uses, previous.getLinect()); // "Duplicate entry %s in %s: previous entry at line %d"
            }
        }
    }

    private void visitProviders(Line line) {
        String service = line.nextToken().asName();
        CLASS_NAME_IN_MODULE.validate(service);
        Line linex = providerUse.put(service, line);
        line.nextToken().mustBe(ReservedWord.res_with);
        String[] modarr = TokenArray.arrayString(Directive.dir_provides, line, CLASS_NAME_IN_MODULE);
        if (linex == null) {
            if (modarr.length == 0) {
                LOG(M225,Directive.dir_provides); // "empty %s ignored"
                return;
            }
            for (String mod:modarr) {
                checkClassPackage(mod, Directive.dir_provides);
            }
            modNode.visitProvide(service,modarr);
        } else {
            LOG(M40,Directive.dir_provides,service,linex.getLinect()); // "duplicate %s: %s already defined at line %d"
        }
    }

    private void visitPackages(Line line) {
        packagesVisited = true;
        String[] packages = TokenArray.arrayString(Directive.dir_packages, line, PACKAGE_NAME);
        for (String pkg:packages) {
            checkPackage(pkg, Directive.dir_packages);
            modNode.visitPackage(pkg);
        }
    }

    public ModuleNode visitEnd() {
        if (!javaBase) {
            LOG(M126,Directive.dir_requires,Constants.JAVA_BASE_MODULE);    // "'%s %s' is required and has been added"
            modNode.visitRequire(Constants.JAVA_BASE_MODULE.stringValue(), AccessFlag.acc_mandated.getAccessFlag(), null);
        }
        CHECK_SUPPORTS(Feature.modules);
        if (packagesVisited) {
            for (Map.Entry<String,EnumSet<Directive>> me:packageUse.entrySet()) {
                String pkg = me.getKey();
                EnumSet<Directive> dirs = me.getValue();
                if (!NameDesc.isJava(pkg) && !dirs.contains(Directive.dir_packages)) {
                    if (dirs.size() == 1 && dirs.contains(Directive.dir_uses)) {
                        // "package %s used in %s is not in %s (OK if package not in module)"
                        LOG(M297,pkg,Directive.dir_uses,Directive.dir_packages);
                    } else {
                        LOG(M169,pkg,dirs,Directive.dir_packages); // "package %s used in %s has been added to %s"
                        modNode.visitPackage(pkg);
                    }
                }
            }
        }
        modNode.visitEnd();
        return modNode;
    }
    
}

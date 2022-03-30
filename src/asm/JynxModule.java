package asm;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.tree.ModuleNode;

import static jynx.ClassType.MODULE;
import static jynx.Global.*;
import static jynx.Message.*;
import static jynx2asm.NameDesc.*;

import jvm.AccessFlag;
import jvm.AttributeName;
import jvm.Constants;
import jvm.Context;
import jvm.Feature;
import jvm.JvmVersion;
import jynx.Access;
import jynx.Directive;
import jynx.LogAssertionError;
import jynx.LogIllegalStateException;
import jynx.ReservedWord;
import jynx2asm.JynxScanner;
import jynx2asm.Line;
import jynx2asm.NameDesc;
import jynx2asm.Token;
import jynx2asm.TokenArray;

public class JynxModule {
    
    private final ModuleNode modNode;
    private final JynxScanner js;
    private final JvmVersion jvmVersion;
    private final Map<String,EnumSet<Directive>> packageUse;
    private final Map<String,Line> providerUse;
    private final Set<String> services;

    private boolean javaBase;
    private final Map<Directive,Line> unique_directives;


    private JynxModule(ModuleNode modnode, JynxScanner js, JvmVersion jvmversion) {
        this.modNode = modnode;
        this.js = js;
        this.jvmVersion = jvmversion;
        this.packageUse = new HashMap<>();
        this.providerUse = new HashMap<>();
        this.services = new HashSet<>();
        this.javaBase = false;
        this.unique_directives = new HashMap<>();
    }
    
    public static JynxModule getInstance(JynxScanner js, JvmVersion jvmversion) {
        Line line = js.getLine();
        EnumSet<AccessFlag> flags = line.getAccFlags();
        String name = line.nextToken().asName();
        Access accessname = Access.getInstance(flags, jvmversion, name,MODULE);
        MODULE_NAME.validate(name);
        String main = line.optAfter(ReservedWord.res_main);
        Token token = line.nextToken();
        String version = token == Token.END_TOKEN?null:token.asString();
        accessname.check4Module();
        int access = accessname.getAccess();
        ModuleNode mnode = new ModuleNode(name, access, version);
        if (main != null) {
            CHECK_SUPPORTS(AttributeName.ModuleMainClass);
            CLASS_NAME_IN_MODULE.validate(main);
            mnode.visitMainClass(main);
        }
        return new JynxModule(mnode, js, jvmversion);
    }

    public ModuleNode getModNode() {
        return modNode;
    }

    private void checkPackage(String packaze, Directive dir) {
        boolean added = packageUse.computeIfAbsent(packaze, v -> EnumSet.noneOf(Directive.class))
                .add(dir);
        if (!added) {
            // "package %s has already appeared in %s"
            throw new LogIllegalStateException(M133,packaze,dir);
        }
    }
    
    public void visitDirective(Directive dir, JynxScanner js) {
        Line line = js.getLine();
        dir.checkUnique(unique_directives, line);
        switch(dir) {
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
    
    private String[] arrayString(Directive dir, Line line,NameDesc nd) {
        TokenArray array = TokenArray.getInstance(js, line);
        Set<String> modlist = new LinkedHashSet<>();
        while(true) {
            Token token = array.firstToken();
            if (token.is(ReservedWord.right_array)) {
                break;
            }
            String mod = token.asString();
            boolean ok = nd.validate(mod);
            if (ok) {
                ok = modlist.add(mod);
                if (!ok) {
                    LOG(M233,mod,dir); // "Duplicate entry %s in %s"
                }
            }
            array.noMoreTokens();
        }
        return modlist.toArray(new String[0]);
    }
    
    private void visitPackages(Line line) {
        String[] packages = arrayString(Directive.dir_packages,line, PACKAGE_NAME);
        for (String pkg:packages) {
            checkPackage(pkg, Directive.dir_packages);
            modNode.visitPackage(pkg);
        }
    }

    private void visitUses(Line line) {
        String service = line.lastToken().asString();
        boolean ok = CLASS_NAME_IN_MODULE.validate(service);
        if (ok) {
            ok = services.add(service);
            if (ok) {
                modNode.visitUse(service);
            } else {
                LOG(M233,service,Directive.dir_uses); // "Duplicate entry %s in %s"
            }
        }
    }

    private Access getAccess(Line line) {
        EnumSet<AccessFlag> flags = line.getAccFlags();
        String name = line.nextToken().asName();
        return Access.getInstance(flags, jvmVersion, name, MODULE);
    }
    
    private void visitExports(Line line) {
        Access accessname = getAccess(line);
        String packaze = accessname.getName();
        checkPackage(packaze, Directive.dir_exports);
        PACKAGE_NAME.validate(packaze);
        accessname.check4Module();
        int access = accessname.getAccess();
        String[] modarr = new String[0];
        Token to = line.nextToken();
        if (to != Token.END_TOKEN) {
            to.mustBe(ReservedWord.res_to);
            modarr = arrayString(Directive.dir_exports,line,MODULE_NAME);
        }
        modNode.visitExport(packaze,access,modarr);
    }

    private void visitOpens(Line line) {
        Access accessname = getAccess(line);
        String packaze = accessname.getName();
        checkPackage(packaze, Directive.dir_opens);
        PACKAGE_NAME.validate(packaze);
        accessname.check4Module();
        int access = accessname.getAccess();
        String[] modarr = new String[0];
        Token to = line.nextToken();
        if (to != Token.END_TOKEN) {
            to.mustBe(ReservedWord.res_to);
            modarr = arrayString(Directive.dir_opens,line,MODULE_NAME);
        }
        line.noMoreTokens();
        modNode.visitOpen(packaze,access,modarr);
    }

    private void visitRequires(Line line) {
        Access accessname = getAccess(line);
        String mod = accessname.getName();
        MODULE_NAME.validate(mod);
        javaBase |= Constants.JAVA_BASE_MODULE.equalString(mod);
        accessname.check4Require();
        int access = accessname.getAccess();
        Token token = line.nextToken();
        String version = token == Token.END_TOKEN?null:token.asString();
        line.noMoreTokens();
        modNode.visitRequire(mod, access, version);
    }

    private void visitProviders(Line line) {
        String service = line.nextToken().asName();
        CLASS_NAME_IN_MODULE.validate(service);
        Line linex = providerUse.put(service, line);
        line.nextToken().mustBe(ReservedWord.res_with);
        String[] modarr = arrayString(Directive.dir_provides,line, CLASS_NAME_IN_MODULE);
        if (linex == null) {
            if (modarr.length == 0) {
                LOG(M225,Directive.dir_provides); // "empty %s ignored"
                return;
            }
            for (String mod:modarr) {
                int index = mod.lastIndexOf("/"); // >= 0 as checked to be CLASS_NAME_IN_MODULE
                String pkg = mod.substring(0,index);
                checkPackage(pkg, Directive.dir_provides);
            }
            modNode.visitProvide(service,modarr);
        } else {
            LOG(M40,Directive.dir_provides,service,linex.getLinect()); // "duplicate %s: %s already defined at line %d"
        }
    }

    public void visitEnd() {
        if (!javaBase) {
            LOG(M126,Directive.dir_requires,Constants.JAVA_BASE_MODULE);    // "'%s %s' is required and has been added"
            modNode.visitRequire(Constants.JAVA_BASE_MODULE.toString(), AccessFlag.acc_mandated.getAccessFlag(), null);
        }
        CHECK_SUPPORTS(Feature.modules);
        boolean ok = true;
        for (Map.Entry<String,EnumSet<Directive>> me:packageUse.entrySet()) {
            String pkg = me.getKey();
            EnumSet<Directive> dirs = me.getValue();
            if (!dirs.contains(Directive.dir_packages)) {
                ok = false;
                LOG(M169,pkg,dirs,Directive.dir_packages); // "package %s used in %s is not in %s"
            }
        }
        if (ok) { // to prevent further errors
            modNode.visitEnd();
        }
    }
    
}

package jynx2asm;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.Handle;

import static jvm.AccessFlag.acc_final;
import static jynx.ClassType.RECORD;
import static jynx.Global.*;
import static jynx.Message.*;

import asm.JynxComponentNode;
import asm.JynxFieldNode;
import asm.JynxMethodNode;
import jvm.AccessFlag;
import jvm.AsmOp;
import jvm.Constants;
import jvm.Context;
import jvm.Feature;
import jvm.HandleType;
import jvm.JvmOp;
import jvm.JvmVersion;
import jynx.Access;
import jynx.ClassType;
import jynx.Directive;

public class ClassChecker {
    
    private final Map<String,JynxComponentNode> components = new HashMap<>();
    private final Map<String,JynxComponentNode> componentMethod = new HashMap<>();
    private final Map<OwnerNameDesc,Line> methods = new HashMap<>();
    private final Map<OwnerNameDesc,Line> staticMethods = new HashMap<>();
    private final Map<OwnerNameDesc,Line> virtualMethods = new HashMap<>();

    private final Map<OwnerNameDesc,Line> ownVirtualMethodsUsed;
    private final Map<OwnerNameDesc,Line> ownSpecialMethodsUsed;
    private final Map<OwnerNameDesc,Line> ownStaticMethodsUsed;
    
    private int methodComponentCt = 0;
    private int fieldComponentCt = 0;
    private final Set<JynxFieldNode> instanceFields = new HashSet<>();
    private final Map<String,JynxFieldNode> fields = new HashMap<>();

    private final String className;
    private final Access classAccess;
    private final ClassType classType;
    private final JvmVersion jvmVersion;

    private ClassChecker(String cname, Access classAccess, ClassType classType, JvmVersion jvmversion) {
        this.className = cname;
        this.classAccess = classAccess;
        this.classType = classType;
        this.jvmVersion = jvmversion;
        this.ownStaticMethodsUsed = new HashMap<>();
        this.ownVirtualMethodsUsed = new HashMap<>();
        this.ownSpecialMethodsUsed = new HashMap<>();
    }

    public final static OwnerNameDesc EQUALS_METHOD = OwnerNameDesc.getMethodDesc(Constants.EQUALS.toString());
    public final static OwnerNameDesc TOSTRING_METHOD = OwnerNameDesc.getMethodDesc(Constants.TOSTRING.toString());
    public final static OwnerNameDesc HASHCODE_METHOD = OwnerNameDesc.getMethodDesc(Constants.HASHCODE.toString());

    public final static OwnerNameDesc FINALIZE_METHOD = OwnerNameDesc.getMethodDesc(Constants.FINALIZE.toString());

    public static ClassChecker getInstance(String cname, Access classAccess, ClassType classType, JvmVersion jvmversion) {
        ClassChecker checker = new ClassChecker(cname, classAccess, classType, jvmversion);
        if (classType == ClassType.ENUM) { // final emthods in java/lang/Enum
            Constants.FINAL_ENUM_METHODS.stream()
                    .map(Constants::toString)
                    .map(OwnerNameDesc::getMethodDesc)
                    .forEach(ond ->checker.virtualMethods.put(ond,Line.EMPTY));
            OwnerNameDesc compare = OwnerNameDesc.getMethodDesc(String.format(Constants.COMPARETO_FORMAT.toString(),cname));
            checker.virtualMethods.put(compare, Line.EMPTY);
        }
        if (!Constants.OBJECT_CLASS.equalString(cname)) {
            Constants.FINAL_OBJECT_METHODS.stream()
                    .map(Constants::toString)
                    .map(OwnerNameDesc::getMethodDesc)
                    .forEach(ond ->checker.virtualMethods.put(ond,Line.EMPTY));
        }
        return checker;
    }
    
    public void setSuper(String csuper) {
        if (classType == ClassType.ENUM && csuper.equals(Constants.ENUM_SUPER.toString())) {
            OwnerNameDesc values = OwnerNameDesc.getMethodDesc(String.format(Constants.VALUES_FORMAT.toString(),className));
            ownStaticMethodsUsed.put(values, Line.EMPTY);
            OwnerNameDesc valueof = OwnerNameDesc.getMethodDesc(String.format(Constants.VALUEOF_FORMAT.toString(),className));
            ownStaticMethodsUsed.put(valueof, Line.EMPTY);
        }
    }
    
    public String getClassName() {
        return className;
    }

    public JvmVersion getJvmVersion() {
        return jvmVersion;
    }

    public ClassType getClassType() {
        return classType;
    }

    public void used(OwnerNameDesc cmd, JvmOp jvmop, Line line) {
        if (cmd.getOwner().equals(className)) {
            AsmOp base = jvmop.getBase();
            switch (base) {
                case asm_invokevirtual:
                    ownVirtualMethodsUsed.putIfAbsent(cmd, line);
                    break;
                case asm_invokestatic:
                    ownStaticMethodsUsed.putIfAbsent(cmd, line);
                    break;
                case asm_invokespecial:
                    ownSpecialMethodsUsed.putIfAbsent(cmd, line);
                    break;
            }
        }
    }
    
    public void mayBeHandle(Object handleobj, Line line) {
        if (handleobj instanceof Handle) {
            Handle handle =  (Handle)handleobj;
            OwnerNameDesc cmd = OwnerNameDesc.of(handle);
            HandleType htype = HandleType.getInstance(handle.getTag());
            used(cmd,htype.op(),line);
        }
    }
    
    public Access getAccess(Context context, Line line) {
        EnumSet<AccessFlag> flags = line.getAccFlags();
        String name = line.nextToken().asName();
        boolean component = isComponent(name,context);
        if (component) {
            flags.add(AccessFlag.xxx_component);
        }
        return Access.getInstance(flags, jvmVersion, name,classType);
    }
    
    public void checkComponent(JynxComponentNode jcn) {
        String compname = jcn.getName();
        JynxComponentNode previous = components.put(compname,jcn);
        if (previous == null) {
            componentMethod.put(jcn.getMethodName(), jcn);
        } else {
            // "duplicate %s: %s already defined at line %d"
            LOG(M40,Directive.dir_component,compname,previous.getLine().getLinect());
        }
    }

    
    public void checkMethod(JynxMethodNode jmn) {
        if (classType == RECORD && methods.isEmpty() && fieldComponentCt != components.size()) {
            // "number of Record components %d disagrees with number of instance fields %d"
            LOG(M48,components.size(),fieldComponentCt);
        }
        OwnerNameDesc ond = jmn.getOwnerNameDesc();
        Line previous = methods.put(ond, jmn.getLine());
        if (previous == Line.EMPTY) {
            // "%s cannot be overridden"            
            LOG(M262,ond);
        } else if (previous != null) {
            // "duplicate %s: %s already defined at line %d"            
            LOG(M40,Directive.dir_method,ond,previous.getLinect());
        }
        if (jmn.isStatic()) {
            staticMethods.put(ond, jmn.getLine());
        } else {
            virtualMethods.put(ond, jmn.getLine());
        }
        JynxComponentNode jcomp = getComponent4Method(jmn.getName(), jmn.getDesc());
        if (jcomp != null) {
            ++methodComponentCt;
        }
         if (classAccess.is(acc_final) && jmn.isAbstract()) {
            LOG(M59,jmn.getName());  // "method %s cannot be abstract in final class"
        }
   }    
    
    public JynxComponentNode getComponent4Method(String mname, String mdesc) {
        return componentMethod.get(mname + mdesc);
    }
    
    public boolean isComponent(String name,Context context) {
        if (context == Context.METHOD) {
            return componentMethod.containsKey(name);
        } else if (context == Context.FIELD) {
            return components.containsKey(name);
        }
        return false;
    }
    
    public JynxComponentNode getComponent4Field(String mname) {
        return components.get(mname);
    }
    
    
    public void checkField(JynxFieldNode jfn) {
        String namedesc = jfn.getName() + jfn.getDesc();
        JynxFieldNode previous = fields.put(namedesc,jfn);
        if (previous != null) {
            // "duplicate %s: %s %s already defined at line %d"
            LOG(M55,Directive.dir_field,jfn.getName(),jfn.getDesc(),previous.getLine().getLinect());
        }
        if (jfn.isInstanceField()) {
            instanceFields.add(jfn);
            if (classType == RECORD) {
                ++fieldComponentCt;
            }
        }
    }

    private void mustHaveVirtualMethod(OwnerNameDesc namedesc) {
        if (!virtualMethods.containsKey(namedesc)) {
            LOG(M132,classType, namedesc.toJynx());   // "%s must have a %s method"
        }
    }
    
    private void shouldHaveVirtualMethod(OwnerNameDesc has, OwnerNameDesc should) {
        if (!virtualMethods.containsKey(should)) {
            LOG(M153,has.toJynx(),should.toJynx());   // "as class has a %s method it should have a %s method"
        }
    }
    
    public void visitEnd() {
        if (methodComponentCt != components.size()) {
            // "number of component methods is %d but number of components is %d"
            LOG(M137,methodComponentCt,components.size());
        }
        if (classType == RECORD) {
            mustHaveVirtualMethod(TOSTRING_METHOD);
            mustHaveVirtualMethod(HASHCODE_METHOD);
            mustHaveVirtualMethod(EQUALS_METHOD);
        } else if (classType == ClassType.CLASS) {
            boolean init = false;
            for (OwnerNameDesc namedesc:virtualMethods.keySet()) {
                if (namedesc.isInit()) {
                    init = true;
                } else if (namedesc.equals(EQUALS_METHOD)) {
                    shouldHaveVirtualMethod(namedesc, HASHCODE_METHOD);
                } else if (namedesc.getName().equals(EQUALS_METHOD.getName())
                        && !virtualMethods.containsKey(EQUALS_METHOD)) {
                    LOG(M239,namedesc.toJynx(),className); //"%s does not override object equals method in %s"
                }
            }
            if (!instanceFields.isEmpty() && !init) {
                LOG(M156,NameDesc.CLASS_INIT_NAME); // "instance variables with no %s method"
            }
        }
        ownStaticMethodsUsed.entrySet().stream()
                .forEach(me->{
                    if (!staticMethods.containsKey(OwnerNameDesc.getMethodDesc(me.getKey().getNameDesc()))) {
                         // "own static method %s not found"
                        LOG(me.getValue(),M251,me.getKey().getName());
                    }
                });
        virtualMethods.putIfAbsent(EQUALS_METHOD, Line.EMPTY);
        virtualMethods.putIfAbsent(HASHCODE_METHOD, Line.EMPTY);
        virtualMethods.putIfAbsent(TOSTRING_METHOD, Line.EMPTY);
        String[] missing = ownVirtualMethodsUsed.keySet().stream()
                .filter(k->!virtualMethods.containsKey(OwnerNameDesc.getMethodDesc(k.getNameDesc())))
                .map(k->k.getName())
                .toArray(String[]::new);
        if (missing.length != 0) {
            // "the following own virtual method(s) are used but not found in class (but may be in super class or interface)%n    %s"
           LOG(M250,Arrays.asList(missing));
        }
        ownSpecialMethodsUsed.entrySet().stream()
                .forEach(me->{
                    if (!virtualMethods.containsKey(OwnerNameDesc.getMethodDesc(me.getKey().getNameDesc()))) {
                         // "own init method %s not found"
                        LOG(me.getValue(),M252,me.getKey().getName());
                    }
                });
        if (virtualMethods.containsKey(FINALIZE_METHOD) && classType != ClassType.ENUM 
                || ownVirtualMethodsUsed.containsKey(FINALIZE_METHOD)) {
            jvmVersion.checkSupports(Feature.finalize);
        }
    }
    
}

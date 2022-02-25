package jynx2asm;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import org.objectweb.asm.Handle;

import static jvm.AccessFlag.acc_final;
import static jvm.HandleType.*;
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
import jynx.GlobalOption;

public class ClassChecker {
    
    private final Map<String,JynxComponentNode> components = new HashMap<>();
    private final Map<String,JynxComponentNode> componentMethod = new HashMap<>();

    private final Map<OwnerNameDesc,ObjectLine<HandleType>> ownMethods;
    private final Map<OwnerNameDesc,ObjectLine<HandleType>> ownMethodsUsed;
    
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
        this.ownMethodsUsed = new TreeMap<>(); // sorted for reproducibilty
        this.ownMethods = new TreeMap<>(); // sorted for reproducibilty
    }

    public final static OwnerNameDesc EQUALS_METHOD = OwnerNameDesc.getMethodDesc(Constants.EQUALS.toString());
    public final static OwnerNameDesc TOSTRING_METHOD = OwnerNameDesc.getMethodDesc(Constants.TOSTRING.toString());
    public final static OwnerNameDesc HASHCODE_METHOD = OwnerNameDesc.getMethodDesc(Constants.HASHCODE.toString());

    public final static OwnerNameDesc FINALIZE_METHOD = OwnerNameDesc.getMethodDesc(Constants.FINALIZE.toString());

    public static ClassChecker getInstance(String cname, Access classAccess, ClassType classType, JvmVersion jvmversion) {
        ClassChecker checker = new ClassChecker(cname, classAccess, classType, jvmversion);
        ObjectLine<HandleType> virtual = new ObjectLine<>(REF_invokeVirtual, Line.EMPTY);
        if (classType == ClassType.ENUM) { // final emthods in java/lang/Enum
            Constants.FINAL_ENUM_METHODS.stream()
                    .map(Constants::toString)
                    .map(OwnerNameDesc::getMethodDesc)
                    .forEach(ond -> checker.ownMethods.put(ond,virtual));
            OwnerNameDesc compare = OwnerNameDesc.getMethodDesc(String.format(Constants.COMPARETO_FORMAT.toString(),cname));
            checker.ownMethods.put(compare,virtual);
        }
        if (!Constants.OBJECT_CLASS.equalString(cname)) {
            Constants.FINAL_OBJECT_METHODS.stream()
                    .map(Constants::toString)
                    .map(OwnerNameDesc::getMethodDesc)
                    .forEach(ond ->checker.ownMethods.put(ond,virtual));
        }
        return checker;
    }
    
    public void setSuper(String csuper) {
        if (classType == ClassType.ENUM && csuper.equals(Constants.ENUM_SUPER.toString())) {
            ObjectLine<HandleType> objline = new ObjectLine<>(REF_invokeStatic,Line.EMPTY);
            String str = String.format(Constants.VALUES_FORMAT.toString(),className);
            OwnerNameDesc values = OwnerNameDesc.getOwnerMethodDescAndCheck(str,REF_invokeStatic);
            ownMethodsUsed.put(values,objline);
            str = String.format(Constants.VALUEOF_FORMAT.toString(),className);
            OwnerNameDesc valueof = OwnerNameDesc.getOwnerMethodDescAndCheck(str,REF_invokeStatic);
            ownMethodsUsed.put(valueof,objline);
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

    public void usedMethod(OwnerNameDesc cmd, JvmOp jvmop, Line line) {
        HandleType ht = fromOp(jvmop.getBase(), cmd.isInit());
        usedMethod(cmd, ht, line);
    }
    
    private void usedMethod(OwnerNameDesc cmd, HandleType ht, Line line) {
        assert !ht.isField();
        String owner = cmd.getOwner();
        if (owner.equals(className)) {
            ObjectLine<HandleType> objline = new ObjectLine<>(ht,line);
            ObjectLine<HandleType> previous = ownMethodsUsed.putIfAbsent(cmd, objline);
            if (previous != null && objline.object() != previous.object()) {
                // "%s has different type %s from previous %s at line %d"
                LOG(M405,cmd.toJynx(),objline.object(), previous.object(),previous.line().getLinect());
            }
        } else if (OPTION(GlobalOption.CHECK_METHOD_REFERENCES)) {
            checkMethodExists(cmd, ht);
        }
    }
    
    private final static MethodHandles.Lookup LOOKUP = MethodHandles.publicLookup();
    
    private void checkMethodExists(OwnerNameDesc ond, HandleType ht) {
        assert !ht.isField();
        String owner = ond.getOwner();
        assert !owner.equals(className);
        String mname = ond.getName();
        String desc = ond.getDesc();
        try {
            MethodType mt = MethodType.fromMethodDescriptorString(desc, null);
            Class<?> klass = Class.forName(owner.replace('/', '.'),false,
                    ClassLoader.getSystemClassLoader());
            switch (ht) {
                case REF_invokeStatic:
                    LOOKUP.findStatic(klass, mname, mt);
                    break;
                case REF_invokeInterface:
                case REF_invokeVirtual:
                    LOOKUP.findStatic(klass, mname, mt);
                    break;
                case REF_invokeSpecial:
                    // no check
                    break;
                case REF_newInvokeSpecial:
                    LOOKUP.findConstructor(klass, mt);
                    break;
                default: 
                    throw new EnumConstantNotPresentException(ht.getClass(), ht.name());
            }
        } catch (ClassNotFoundException
                | IllegalArgumentException
                | NoSuchMethodException
                | IllegalAccessException ex) {
             // "unable to find method %s because of %s"
            LOG(M400,ond.toJynx(),ex.getClass().getSimpleName());
        } catch (TypeNotPresentException typex) {
            String typename = typex.typeName().replace(".","/");
            if (typename.equals(className))  {
                return;
            }
            String cause = typex.getClass().getSimpleName() + " " + typename;
             // "unable to find method %s because of %s"
            LOG(M400,ond.toJynx(),cause);
        }
    }
    
    public void mayBeHandle(Object handleobj, Line line) {
        if (handleobj instanceof Handle) {
            Handle handle =  (Handle)handleobj;
            OwnerNameDesc cmd = OwnerNameDesc.of(handle);
            HandleType htype = HandleType.getInstance(handle.getTag());
            if (!htype.isField()) {
                usedMethod(cmd,htype,line);
            }
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
        if (classType == RECORD && ownMethods.isEmpty() && fieldComponentCt != components.size()) {
            // "number of Record components %d disagrees with number of instance fields %d"
            LOG(M48,components.size(),fieldComponentCt);
        }
        OwnerNameDesc ond = jmn.getOwnerNameDesc();
        HandleType ht;
        if (jmn.isStatic()) {
            ht = REF_invokeStatic;
        } else if (ond.isInit()){
            ht = REF_newInvokeSpecial;
        } else {
            ht = REF_invokeVirtual;
        }
        ObjectLine<HandleType> objline = new ObjectLine<>(ht,jmn.getLine());
        ObjectLine<HandleType> previous = ownMethods.put(ond, objline);
        if (previous != null) {
            if (previous.line() == Line.EMPTY) {
                // "%s cannot be overridden"            
                LOG(M262,ond);
            } else {
                // "duplicate %s: %s already defined at line %d"            
                LOG(M40,Directive.dir_method,ond,previous.line().getLinect());
            }
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

    public void usedField(OwnerNameDesc fd, JvmOp jvmop) {
        if (fd.getOwner().equals(className)) {
            AsmOp asmop = jvmop.getBase();
            boolean instance = asmop == AsmOp.asm_getfield || asmop == AsmOp.asm_putfield;
            JynxFieldNode jfn = fields.get(fd.getNameDesc());
            if (jfn == null) {
                // "field %s does not exist in this class but may exist in superclass/superinterface"
                LOG(M214,fd.getName());
            } else if (jfn.isInstanceField() ^ instance) {
                String fieldtype = jfn.isInstanceField()?"instance":"static";
                String optype = instance?"instance":"static";
                LOG(M215,fieldtype,fd.getName(),optype,jvmop); // " %s field %s accessed by %s op %s"
            }
        }
    }
    
    private void mustHaveVirtualMethod(OwnerNameDesc namedesc) {
        ObjectLine<HandleType> objline = ownMethods.get(namedesc);
        if (objline == null || objline.object() != REF_invokeVirtual) {
            // "%s must have a %s method of type %s"
            LOG(M132,classType, namedesc.toJynx(),REF_invokeVirtual);
        }
    }
    
    private void shouldHaveVirtualMethod(OwnerNameDesc has, OwnerNameDesc should) {
        ObjectLine<HandleType> objline = ownMethods.get(should);
        if (objline == null || objline.object() != REF_invokeVirtual) {
            // "as class has a %s method it should have a %s method"
            LOG(M153,has.toJynx(),should.toJynx());
        }
    }

    private boolean isMethodDefined(OwnerNameDesc cnd, HandleType ht) {
        OwnerNameDesc nd = OwnerNameDesc.getMethodDesc(cnd.getNameDesc());
        ObjectLine<HandleType> objline = ownMethods.get(nd);
        return objline != null && objline.object() == ht;
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
            boolean init = ownMethods.values().stream()
                    .filter(ol -> ol.object() == REF_newInvokeSpecial)
                    .findFirst()
                    .isPresent();
            if (!instanceFields.isEmpty() && !init) {
                LOG(M156,NameDesc.CLASS_INIT_NAME); // "instance variables with no %s method"
            }
            boolean equals = ownMethods.keySet().stream()
                    .filter(ond -> ond.equals(EQUALS_METHOD))
                    .findFirst()
                    .isPresent();
            if (equals) {
                    shouldHaveVirtualMethod(EQUALS_METHOD, HASHCODE_METHOD);
            }
            Optional<OwnerNameDesc> xequals = ownMethods.entrySet().stream()
                    .filter(me -> me.getValue().object() == REF_invokeVirtual)
                    .map(me -> me.getKey())
                    .filter(ond -> ond.getName().equals(EQUALS_METHOD.getName()))
                    .findFirst();
            if (xequals.isPresent() && !equals) {
                //"%s does not override object equals method in %s"
                LOG(M239,xequals.get().toJynx(),className);
            }
        }
        ownMethodsUsed.entrySet().stream()
                .filter(me -> me.getValue().object() == REF_invokeStatic)
                .forEach(me->{
                    if (!isMethodDefined(me.getKey(), REF_invokeStatic)) {
                         // "own static method %s not found"
                        LOG(me.getValue().line(),M251,me.getKey().getName());
                    }
                });
        ObjectLine<HandleType> virtual = new ObjectLine<>(REF_invokeVirtual, Line.EMPTY); 
        ownMethods.putIfAbsent(EQUALS_METHOD, virtual);
        ownMethods.putIfAbsent(HASHCODE_METHOD, virtual);
        ownMethods.putIfAbsent(TOSTRING_METHOD, virtual);
        String[] missing = ownMethodsUsed.entrySet().stream()
                .filter(me -> me.getValue().object() == REF_invokeVirtual)
                .map(me-> me.getKey())
                .filter(k->!isMethodDefined(k,REF_invokeVirtual))
                .map(k->k.getName())
                .toArray(String[]::new);
        if (missing.length != 0) {
            // "the following own virtual method(s) are used but not found in class (but may be in super class or interface)%n    %s"
           LOG(M250,Arrays.asList(missing));
        }
        ownMethodsUsed.entrySet().stream()
                .filter(me -> me.getValue().object() == REF_newInvokeSpecial)
                .forEach(me->{
                    if (!isMethodDefined(me.getKey(),REF_newInvokeSpecial)) {
                         // "own init method %s not found"
                        LOG(me.getValue().line(),M252,me.getKey().getName());
                    }
                });
        if (ownMethods.containsKey(FINALIZE_METHOD) && classType != ClassType.ENUM ) {
            jvmVersion.checkSupports(Feature.finalize);
        }
    }
    
}

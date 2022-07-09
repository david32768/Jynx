package jynx2asm;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.objectweb.asm.Handle;

import static jvm.AccessFlag.acc_final;
import static jvm.Context.FIELD;
import static jvm.Context.METHOD;
import static jvm.HandleType.*;
import static jynx.ClassType.RECORD;
import static jynx.Global.*;
import static jynx.Message.*;

import asm.JynxComponentNode;
import asm.JynxFieldNode;
import asm.JynxMethodNode;
import jvm.AccessFlag;
import jvm.Constants;
import jvm.Context;
import jvm.Feature;
import jvm.HandleType;
import jvm.JvmVersion;
import jynx.Access;
import jynx.ClassType;
import jynx.Directive;
import jynx.GlobalOption;
import jynx2asm.ops.JvmOp;

public class ClassChecker {
    
    private final Map<String,JynxComponentNode> components = new HashMap<>();

    private final Map<MethodDesc,ObjectLine<HandleType>> ownMethods;
    private final Map<OwnerNameDesc,ObjectLine<HandleType>> ownMethodsUsed;
    
    private int fieldComponentCt = 0;
    private int instanceFieldCt = 0;
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

    public final static MethodDesc EQUALS_METHOD = Constants.EQUALS.methodDesc();
    public final static MethodDesc TOSTRING_METHOD = Constants.TOSTRING.methodDesc();
    public final static MethodDesc HASHCODE_METHOD = Constants.HASHCODE.methodDesc();

    public final static MethodDesc FINALIZE_METHOD = Constants.FINALIZE.methodDesc();

    public final static Map<String,MethodDesc> SERIAL_METHODS;
    
    static {
        SERIAL_METHODS = new HashMap<>();
        for (Constants method: Constants.PRIVATE_SERIALIZATION_METHODS) {
            MethodDesc md = method.methodDesc();
            SERIAL_METHODS.put(md.getName(),md);
        }
    }
    
    public static ClassChecker getInstance(String cname, Access classAccess,
            ClassType classType, JvmVersion jvmversion) {
        ClassChecker checker = new ClassChecker(cname, classAccess, classType, jvmversion);
        if (classType == ClassType.ENUM) { // final methods in java/lang/Enum
            ObjectLine<HandleType> virtual = new ObjectLine<>(REF_invokeVirtual, Line.EMPTY);
            Constants.FINAL_ENUM_METHODS.stream()
                    .map(Constants::toString)
                    .map(MethodDesc::getInstance)
                    .forEach(ond -> checker.ownMethods.put(ond,virtual));
            MethodDesc compare = MethodDesc.getInstance(String.format(Constants.COMPARETO_FORMAT.toString(),cname));
            checker.ownMethods.put(compare,virtual);
        }
        if (!Constants.OBJECT_CLASS.equalString(cname)
                && classType != ClassType.MODULE_CLASS && classType != ClassType.PACKAGE) {
            ObjectLine<HandleType> virtual = new ObjectLine<>(REF_invokeVirtual, Line.EMPTY);
            Constants.FINAL_OBJECT_METHODS.stream()
                    .map(Constants::toString)
                    .map(MethodDesc::getInstance)
                    .forEach(ond ->checker.ownMethods.put(ond,virtual));
        }
        return checker;
    }
    
    public void setSuper(String csuper) {
        if (classType == ClassType.ENUM && Constants.ENUM_SUPER.equalString(csuper)) {
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
        HandleType ht = fromOp(jvmop, cmd.isInit());
        usedMethod(cmd, ht, line);
    }
    
    private void usedMethod(OwnerNameDesc cmd, HandleType ht, Line line) {
        assert !ht.isField();
        String owner = cmd.getOwner();
        if (owner.equals(className)) {
            ObjectLine<HandleType> objline = new ObjectLine<>(ht,line);
            ObjectLine<HandleType> previous = ownMethodsUsed.putIfAbsent(cmd, objline);
            if (previous != null && !ht.maybeOK(previous.object())) {
                // "%s has different type %s from previous %s at line %d"
                LOG(M405,cmd.toJynx(),ht, previous.object(),previous.line().getLinect());
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
                case REF_invokeSpecial:
                case REF_invokeInterface:
                case REF_invokeVirtual:
                    LOOKUP.findVirtual(klass, mname, mt);
                    break;
                case REF_newInvokeSpecial:
                    LOOKUP.findConstructor(klass, mt);
                    break;
                default: 
                    throw new EnumConstantNotPresentException(ht.getClass(), ht.name());
            }
        } catch (ClassNotFoundException
                | IllegalArgumentException
                | NoSuchMethodException ex) {
             // "unable to find method %s because of %s"
            LOG(M400,ond.toJynx(),ex.getClass().getSimpleName());
        } catch (IllegalAccessException iaex) {
            if (ht == REF_invokeSpecial) { // maybe protected
                return;
            } else if (ond.isSamePackage(className)) { // maybe package-private
                return;
            }
             // "unable to find method %s because of %s"
            LOG(M400,ond.toJynx(),iaex.getClass().getSimpleName());
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
        return Access.getInstance(flags, jvmVersion, name,classType);
    }
    
    public void checkComponent(JynxComponentNode jcn) {
        String compname = jcn.getName();
        JynxComponentNode previous = components.put(compname,jcn);
        if (previous != null) {
            // "duplicate %s: %s already defined at line %d"
            LOG(M40,Directive.dir_component,compname,previous.getLine().getLinect());
        }
    }

    public void checkMethod(JynxMethodNode jmn) {
        MethodDesc md = jmn.getMethodDesc();
        HandleType ht;
        if (jmn.isStatic()) {
            ht = REF_invokeStatic;
        } else if (md.isInit()){
            ht = REF_newInvokeSpecial;
        } else if (classType == ClassType.INTERFACE) {
            ht = REF_invokeInterface;
        } else {
            ht = REF_invokeVirtual;
        }
        switch (classType) {
            case ANNOTATION_CLASS:
                if (jmn.isStatic() || !jmn.isAbstract() || !md.getDesc().contains("()")) {
                    // "method %s in %s class must be %s, not %s and have no parameters"
                    LOG(M406,md.toJynx(),classType,AccessFlag.acc_abstract,AccessFlag.acc_static);
                }
                break;
        }
        ObjectLine<HandleType> objline = new ObjectLine<>(ht,jmn.getLine());
        ObjectLine<HandleType> previous = ownMethods.put(md, objline);
        if (previous != null) {
            if (previous.line() == Line.EMPTY) {
                // "%s cannot be overridden"            
                LOG(M262,md);
            } else {
                // "duplicate %s: %s already defined at line %d"            
                LOG(M40,Directive.dir_method,md,previous.line().getLinect());
            }
        }
        if (classAccess.is(acc_final) && jmn.isAbstract()) {
            LOG(M59,jmn.getName());  // "method %s cannot be abstract in final class"
        }
        MethodDesc sermd = SERIAL_METHODS.get(md.getName());
        if (sermd != null) {
            if (sermd.equals(md)) {
                if (!jmn.isPrivate()) {
                    LOG(M207,md.getName()); // "possible serialization method %s is not private"
                }
            } else if (jmn.isPrivate()) {
                LOG(M227,md.toJynx(),sermd.toJynx()); // "possible serialization method %s does not match %s"
            }
        }
   }    
    
    private JynxComponentNode getComponent4Method(String mname, String mdesc) {
        JynxComponentNode jcn = components.get(mname);
        if (jcn != null && jcn.getMethodDesc().getDesc().equals(mdesc)) {
            return jcn;
        }
        return null;
    }
    
    public boolean isComponent(Context context, String name, String desc) {
        if (context == Context.METHOD) {
            JynxComponentNode jcn = getComponent4Method(name, desc);
            return jcn != null;
        } else if (context == Context.FIELD) {
            return components.containsKey(name);
        }
        return false;
    }
    
    private JynxComponentNode getComponent4Field(String mname) {
        return components.get(mname);
    }
    
    public void checkField(JynxFieldNode jfn) {
        String name = jfn.getName();
        JynxFieldNode previous = fields.put(name,jfn);
        if (previous != null) {
            // "duplicate %s: %s %s already defined at line %d"
            LOG(M55,Directive.dir_field,jfn.getName(),"",previous.getLine().getLinect());
        }
        if (!jfn.isStatic()) {
            ++instanceFieldCt;
            if (classType == RECORD) {
                JynxComponentNode jcn = components.get(name);
                if (jcn != null && !jcn.getDesc().equals(jfn.getDesc())) {
                    // "component %s description %s differs from field description %s"
                    LOG(M269,name,jcn.getDesc(),jfn.getDesc());
                }
                ++fieldComponentCt;
            }
        }
    }

    public void usedField(FieldDesc fd, JvmOp jvmop) {
        if (fd.getOwner().equals(className)) {
            JvmOp asmop = jvmop;
            boolean instance = asmop == JvmOp.asm_getfield || asmop == JvmOp.asm_putfield;
            JynxFieldNode jfn = fields.get(fd.getName());
            if (jfn == null || !fd.getDesc().equals(jfn.getDesc())) {
                // "field %s %s does not exist in this class but may exist in superclass/superinterface"
                LOG(M214,fd.getName(), fd.getDesc());
            } else if (jfn.isStatic() == instance) {
                String fieldtype = jfn.isStatic()?"static":"instance";
                String optype = instance?"instance":"static";
                LOG(M215,fieldtype,fd.getName(),optype,jvmop); // " %s field %s accessed by %s op %s"
            }
        }
    }
    
    public void checkSignature4Method(String signature, String name, String desc) {
        JynxComponentNode jcn = getComponent4Method(name,desc);
        jcn.checkSignature(signature, METHOD);
    }
    
    public void checkSignature4Field(String signature, String name) {
        JynxComponentNode jcn = getComponent4Field(name);
        jcn.checkSignature(signature, FIELD);
    }
    
    private void mustHaveVirtualMethod(MethodDesc namedesc) {
        ObjectLine<HandleType> objline = ownMethods.get(namedesc);
        if (objline == null || objline.object() != REF_invokeVirtual) {
            // "%s must have a %s method of type %s"
            LOG(M132,classType, namedesc.toJynx(),REF_invokeVirtual);
        }
    }
    
    private void shouldHaveVirtualMethod(MethodDesc has, MethodDesc should) {
        ObjectLine<HandleType> objline = ownMethods.get(should);
        if (objline == null || objline.object() != REF_invokeVirtual) {
            // "as class has a %s method it should have a %s method"
            LOG(M153,has.toJynx(),should.toJynx());
        }
    }

    private boolean isMethodDefined(OwnerNameDesc cnd, HandleType ht) {
        MethodDesc nd = MethodDesc.getInstance(cnd.getNameDesc());
        ObjectLine<HandleType> objline = ownMethods.get(nd);
        return objline != null && objline.object() == ht;
    }

    private void visitRecordEnd() {
        if (fieldComponentCt != components.size()) {
            // "number of Record components %d disagrees with number of instance fields %d"
            LOG(M48,components.size(),fieldComponentCt);
        }
        for (JynxComponentNode jcn : components.values()) {
            mustHaveVirtualMethod(jcn.getMethodDesc());
        }
        mustHaveVirtualMethod(TOSTRING_METHOD);
        mustHaveVirtualMethod(HASHCODE_METHOD);
        mustHaveVirtualMethod(EQUALS_METHOD);
    }
    
    private void visitClassEnd() {
        boolean init = ownMethods.keySet().stream()
            .filter(OwnerNameDesc::isInit)
            .findFirst()
            .isPresent();
        long instanceMethodCoumt = ownMethods.values().stream()
            .filter(ol-> ol.object() == REF_invokeVirtual)
            .filter(ol->ol.line() != Line.EMPTY)
            .count();
        if (!init && (instanceFieldCt != 0 || instanceMethodCoumt != 0)) {
            LOG(M156,NameDesc.CLASS_INIT_NAME); // "instance variables or methods with no %s method"
        }
        boolean equals = ownMethods.keySet().stream()
            .filter(ond -> ond.equals(EQUALS_METHOD))
            .findFirst()
            .isPresent();
        if (equals) {
            shouldHaveVirtualMethod(EQUALS_METHOD, HASHCODE_METHOD);
        }
        Optional<MethodDesc> xequals = ownMethods.entrySet().stream()
            .filter(me -> me.getValue().object() == REF_invokeVirtual)
            .map(me -> me.getKey())
            .filter(ond -> ond.getName().equals(EQUALS_METHOD.getName()))
            .findFirst();
        if (xequals.isPresent() && !equals) {
            //"%s does not override object equals method in %s"
            LOG(M239,xequals.get().toJynx(),className);
        }
        ownMethodsUsed.entrySet().stream()
                .filter(me -> me.getValue().object() == REF_newInvokeSpecial)
                .forEach(me->{
                    if (!isMethodDefined(me.getKey(),REF_newInvokeSpecial)) {
                         // "own init method %s not found"
                        LOG(me.getValue().line(),M252,me.getKey().getName());
                    }
                });
        checkMissing(REF_invokeVirtual);
    }

    private void checkMissing(HandleType ht) {
        ObjectLine<HandleType> virtual = new ObjectLine<>(ht, Line.EMPTY); 
        ownMethods.putIfAbsent(EQUALS_METHOD, virtual);
        ownMethods.putIfAbsent(HASHCODE_METHOD, virtual);
        ownMethods.putIfAbsent(TOSTRING_METHOD, virtual);
        String[] missing = ownMethodsUsed.entrySet().stream()
                .filter(me -> me.getValue().object() == ht)
                .map(me-> me.getKey())
                .filter(k->!isMethodDefined(k,ht))
                .map(k->k.getName())
                .toArray(String[]::new);
        if (missing.length != 0) {
            // "the following own virtual method(s) are used but not found in class (but may be in super class or interface)%n    %s"
           LOG(M250,Arrays.asList(missing));
        }
    }
    
    public void visitEnd() {
        switch (classType) {
            case RECORD:
                visitRecordEnd();
                visitClassEnd();
                break;
            case ENUM:
            case BASIC:
                visitClassEnd();
                break;
            case INTERFACE:
                checkMissing(REF_invokeInterface);
                break;
            case ANNOTATION_CLASS:
                break;
            case MODULE_CLASS:
            case PACKAGE:
                assert ownMethods.isEmpty() && ownMethodsUsed.isEmpty();
                break;
            default:
                throw new EnumConstantNotPresentException(classType.getClass(), classType.name());
        }
        ownMethodsUsed.entrySet().stream()
                .filter(me -> me.getValue().object() == REF_invokeStatic)
                .forEach(me->{
                    if (!isMethodDefined(me.getKey(), REF_invokeStatic)) {
                         // "own static method %s not found (but may be in super class)"
                        LOG(me.getValue().line(),M251,me.getKey().getName());
                    }
                });
        if (ownMethods.containsKey(FINALIZE_METHOD) && classType != ClassType.ENUM ) {
            jvmVersion.checkSupports(Feature.finalize);
        }
    }
    
}

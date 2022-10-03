package jynx2asm;

import java.util.function.Consumer;
import java.util.Optional;

import org.objectweb.asm.Type;
import org.objectweb.asm.util.CheckClassAdapter;

import static jynx.Global.LOG;
import static jynx.Global.SUPPORTS;
import static jynx.Message.M158;
import static jynx.Message.M236;
import static jynx.Message.M258;
import static jynx.Message.M295;
import static jynx.Message.M401;
import static jynx.Message.M93;

import jvm.JavaReserved;
import jynx.Global;
import jynx.GlobalOption;

public enum Style {
    CLASS_NAME(Style::checkClassStyle),
    PACKAGE_NAME(Style::checkPackageStyle),
    ARRAY_DESC(Style::checkTypeStyle),
    FIELD_NAME(Style::checkFieldNameStyle),
    METHOD_NAME(Style::checkMethodNameStyle),
    DESC(Style::checkDescStyle),
    CLASS_SIGNATURE(CheckClassAdapter::checkClassSignature),
    METHOD_SIGNATURE(CheckClassAdapter::checkMethodSignature),
    FIELD_SIGNATURE(CheckClassAdapter::checkFieldSignature),
    ;
    
    private final Consumer<String> validfn;
    
    private Style(Consumer<String> validfn) {
        this.validfn = validfn;
    }
    
    public void check(String str) {
        try {
            validfn.accept(str);
        } catch(IllegalArgumentException ex) {
            LOG(M295,this,ex.getMessage()); // "%s is invalid: %s"
        }
    }
    
    private static void checkNotJavaReserved(String str) {
        Optional<JavaReserved> javaid = JavaReserved.of(str);
        if (javaid.isPresent()) {
            JavaReserved jres = javaid.get();
            if (jres.isContextual()) {
                LOG(M401,str); // "%s is a contextual reserved word"
            } else if (SUPPORTS(jres.feature())) {
                LOG(M258,str); // "%s is a reserved word and cannot be a Java Id"
            }
        }
    }
    
    private static void checkClassStyle(String str) {
        int index = str.lastIndexOf('/');
        if (index >= 0) {
            checkPackageStyle(str.substring(0,index));
        }
        String klass = str.substring(index + 1);
        checkNotJavaReserved(klass);
        if (Global.OPTION(GlobalOption.WARN_STYLE)) {
            int ch = klass.codePointAt(0);
            if (!Character.isUpperCase(ch)) {
                String classname = str.substring(index + 1);
                LOG(M93,classname); // "class name (%s) does not start with uppercase letter"
            }
        }
    }
    
    private static boolean packageChar(int codepoint) {
        return Character.isLowerCase(codepoint)
                || Character.isDigit(codepoint)
                || codepoint == '/';
    }
    
    private static void checkPackageStyle(String str) {
        String[] components = str.split("/");
        for (String component:components) {
            checkNotJavaReserved(component);
        }
        if (NameDesc.isJava(str) || Global.OPTION(GlobalOption.WARN_STYLE)) {
            if (!str.codePoints().allMatch(Style::packageChar)) {
                LOG(M158,str); // "components of package %s are not all lowercase"
            }
        }
    }

    private static void checkJavaMethodNameStyle(String str) {
        int first = str.codePointAt(0);
        if (Character.isUpperCase(first) && !str.toUpperCase().equals(str)) {
            LOG(M236,METHOD_NAME,str); // "%s (%s) starts with uppercase letter and is not all uppercase"
        }
    }
    
    private static void checkMethodNameStyle(String str) {
        checkNotJavaReserved(str);
        if (Global.OPTION(GlobalOption.WARN_STYLE)) {
            checkJavaMethodNameStyle(str);
        }
    }
    
    private static void checkTypeStyle(String str) {
        Type type = Type.getType(str);
        checkType(type);
    }
    
    private static void checkFieldNameStyle(String str) {
        checkNotJavaReserved(str);
        if (Global.OPTION(GlobalOption.WARN_STYLE)) {
            int first = str.codePointAt(0);
            if (Character.isUpperCase(first) && !str.toUpperCase().equals(str)) {
                LOG(M236,FIELD_NAME,str); // "%s (%s) starts with uppercase letter and is not all uppercase"
            }
        }
    }

    private static void checkType(Type type) {
        if (type.getSort() == Type.ARRAY) {
            type = type.getElementType();
        }
        if (type.getSort() == Type.OBJECT) {
            checkClassStyle(type.getInternalName());
        }
    }
    
    private static void checkDescStyle(String str) {
        Type mnd = Type.getMethodType(str);
        Type[] parmt = mnd.getArgumentTypes();
        for (Type type:parmt) {
            checkType(type);
        }
        Type rt = mnd.getReturnType();
        checkType(rt);
    }
    
}

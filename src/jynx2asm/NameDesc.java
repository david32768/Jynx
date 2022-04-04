package jynx2asm;

import java.util.EnumSet;
import java.util.function.Consumer;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.objectweb.asm.Type;
import org.objectweb.asm.util.CheckClassAdapter;


import static jvm.AccessFlag.acc_final;
import static jvm.AccessFlag.acc_static;
import static jvm.AccessFlag.acc_synthetic;
import static jynx.Global.LOG;
import static jynx.Global.OPTION;
import static jynx.Global.SUPPORTS;
import static jynx.GlobalOption.WARN_STYLE;
import static jynx.Message.*;

import jvm.Constants;
import jvm.JavaReserved;
import jynx.Access;
import jynx.LogIllegalArgumentException;

public enum NameDesc {

    STATIC_INIT_NAME(Constants.STATIC_INIT_NAME.toString()),
    CLASS_INIT_NAME(Constants.CLASS_INIT_NAME.toString()),

    PRIMITIVE("[BCDFIJSZ]"),
    JAVA_ID("\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*"),
    GENERATED_LABEL("@%s",JAVA_ID),
    LABEL("%s|%s",JAVA_ID,GENERATED_LABEL),
    UNQUALIFIED_NAME(JAVA_ID),
    MODULE_ID(JAVA_ID),
    METHOD_ID(JAVA_ID),
    CLASS_NAME(NameDesc::checkClassStyle,"%s(/%s)*", UNQUALIFIED_NAME,UNQUALIFIED_NAME),
    PACKAGE_NAME(NameDesc::checkPackageStyle,CLASS_NAME),
    INNER_CLASS_NAME(NameDesc::checkClassStyle,JAVA_ID),
    CLASS_NAME_IN_MODULE(NameDesc::checkClassStyle,"%s(/%s)+", UNQUALIFIED_NAME,UNQUALIFIED_NAME),
    MODULE_NAME("%s(\\.%s)*", MODULE_ID,MODULE_ID),
    CLASS_PARM("L%s;",CLASS_NAME),
    ARRAY_DESC(NameDesc::checkTypeStyle,"\\[+(%s|%s)",PRIMITIVE,CLASS_PARM),
    FIELD_NAME(NameDesc::checkFieldNameStyle,"%s",UNQUALIFIED_NAME),
    FIELD_DESC("\\[*(%s|%s)",PRIMITIVE,CLASS_PARM),
    INTERFACE_METHOD_NAME(NameDesc::checkMethodNameStyle,METHOD_ID),
    METHOD_NAME(NameDesc::checkMethodNameStyle,"(%s|%s|%s)",STATIC_INIT_NAME,CLASS_INIT_NAME,METHOD_ID),
    PARMS("\\((%s)*\\)",FIELD_DESC),
    DESC(NameDesc::checkDescStyle,"%s(V|%s)",PARMS,FIELD_DESC),
    NAME_DESC("%s%s",METHOD_ID,DESC),
    STATIC_INIT_NAME_DESC(Constants.STATIC_INIT.regex()),
    CLASS_INIT_NAME_DESC("%s%sV",CLASS_INIT_NAME,PARMS),
    INTERFACE_METHOD_NAME_DESC(NAME_DESC),
    METHOD_NAME_DESC("(%s|%s|%s)",NAME_DESC,STATIC_INIT_NAME_DESC,CLASS_INIT_NAME_DESC),
    ARRAY_METHOD_NAME_DESC(Constants.ARRAY_METHODS),
    OBJECT_NAME("%s|%s",ARRAY_DESC,CLASS_NAME),
    OBJECT_METHOD_DESC("(%s)\\/%s",OBJECT_NAME,METHOD_NAME_DESC),
    // for signatures regex only checks valid characters not format
    SIGNATURE_PART("[\\p{javaJavaIdentifierPart}<>/:;\\[\\+\\-\\*\\.\\^]"),
    CLASS_SIGNATURE(CheckClassAdapter::checkClassSignature,"%s+",SIGNATURE_PART),
    METHOD_SIGNATURE(CheckClassAdapter::checkMethodSignature,"%s*\\(%s*\\)%s+",SIGNATURE_PART,SIGNATURE_PART,SIGNATURE_PART),
    FIELD_SIGNATURE(CheckClassAdapter::checkFieldSignature,"%s+",SIGNATURE_PART),
    ;

    private final String regex;
    private final Pattern pattern;
    private final Consumer<String> checkfn;
    
    private NameDesc(String regex) {
        this.regex = regex;
        this.pattern = Pattern.compile(regex);
        this.checkfn = null;
    }

    private NameDesc(Consumer<String> checkfn, String format, NameDesc... nds) {
        int n = nds.length;
        Object[] strings = new Object[n];
        for (int i = 0; i < n; ++i) {
            strings[i] = nds[i].regex;
        }
        this.regex = String.format(format, strings);
        this.pattern = Pattern.compile(this.regex);
        this.checkfn = checkfn;
    }

    private NameDesc(EnumSet<Constants> constants) {
        this.regex = constants.stream()
                .map(Constants::regex)
                .collect(Collectors.joining("|", "(", ")"));
        this.pattern = Pattern.compile(this.regex);
        this.checkfn = null;
    }

    private NameDesc(NameDesc model) {
        this(model.regex);
    }
    
    private NameDesc(String format, NameDesc... nds) {
        this(null,format,nds);
    }

    private NameDesc(Consumer<String> checkfn, NameDesc model) {
        this.regex = model.regex;
        this.pattern = Pattern.compile(regex);
        this.checkfn = checkfn;
    }

    private static void checkNotJavaReserved(String str) {
        Optional<JavaReserved> javaid = JavaReserved.of(str);
        if (javaid.isPresent()) {
            JavaReserved jres = javaid.get();
            if (jres.isContextual()) {
                LOG(M401,jres); // "%s is a contextual reserved word"
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
        if (OPTION(WARN_STYLE)) {
            int ch = klass.codePointAt(0);
            if (!Character.isUpperCase(ch)) {
                String classname = str.substring(index + 1);
                LOG(M93,classname); // "class name (%s) does not start with uppercase letter"
            }
        }
    }
    
    // incomplete test
    public static boolean isJavaBase(String str) {
        return isJava(str);
    }
    
    private static boolean isJava(String str) {
        return str.startsWith("java/") || str.startsWith("javax/");
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
        if (isJava(str) || OPTION(WARN_STYLE)) {
            if (!str.codePoints().allMatch(NameDesc::packageChar)) {
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
        if (OPTION(WARN_STYLE)) {
            checkJavaMethodNameStyle(str);
        }        
    }
    
    private static void checkTypeStyle(String str) {
        Type type = Type.getType(str);
        checkType(type);
    }
    
    private static void checkFieldNameStyle(String str) {
        checkNotJavaReserved(str);
        if (OPTION(WARN_STYLE)) {
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
    
    public boolean isValid(String str) {
        if (str == null) {
            throw new LogIllegalArgumentException(M177, this); // "missing %s"
        }
        boolean ok = pattern.matcher(str).matches();
        if (ok && checkfn != null) {
            try {
                checkfn.accept(str);
            } catch (IllegalArgumentException  | AssertionError iaex) {
                return false;
            } 
        }
        return ok;
    }

    public boolean validate(String str) {
        boolean ok = isValid(str);
        if (!ok) {
            LOG(M66,str,this);   // "%s is not a valid %s"
        }
        return ok;
    }

    public boolean validate(Access accessname) {
        String name = accessname.getName();
        switch (this) {
            case FIELD_NAME:
                boolean ok = validate(name);
                if (OPTION(WARN_STYLE)
                        && accessname.is(acc_static)
                        && accessname.is(acc_final)
                        && !accessname.is(acc_synthetic)
                        && !name.toUpperCase().equals(name)
                        && !name.equals("serialVersionUID")
                        && ok) {
                    LOG(M64,name); // "final static field name (%s) is not in uppercase"
                    ok = false;
                }
                return ok;
            default:
                throw new AssertionError();
        }
    }
    
    @Override
    public String toString() {
        return name().toLowerCase().replace('_', ' ');
    }

    public static boolean isInnerClass(String classname) {
            return classname.lastIndexOf('/') < classname.lastIndexOf('$');
    }

    public final static char GENERATED_LABEL_MARKER = GENERATED_LABEL.regex.charAt(0);
}

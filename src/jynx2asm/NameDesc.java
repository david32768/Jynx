package jynx2asm;

import java.util.EnumSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static jvm.AccessFlag.acc_final;
import static jvm.AccessFlag.acc_static;
import static jvm.AccessFlag.acc_synthetic;
import static jynx.Global.LOG;
import static jynx.Global.OPTION;
import static jynx.GlobalOption.WARN_STYLE;
import static jynx.Message.*;

import jvm.Constants;
import jynx.Access;
import jynx.LogIllegalArgumentException;

public enum NameDesc {

    STATIC_INIT_NAME(Constants.STATIC_INIT_NAME.stringValue()),
    CLASS_INIT_NAME(Constants.CLASS_INIT_NAME.stringValue()),

    OP_STACK("[AIJFD]"),
    OP_PARMS("\\((%s)*\\)",OP_STACK),
    OP_DESC("%s(V|%s)",OP_PARMS,OP_STACK),
    STACKOP_DESC("[nN]?[tT]->[nNtT]*"),

    NOT_QUOTE_ESCAPE("^\"\\\\"),
    ESCAPED_CHAR("\\\\."),
    QUOTED_STRING("\"[%s]*(?:%s[%s]*)*\"",NOT_QUOTE_ESCAPE,ESCAPED_CHAR,NOT_QUOTE_ESCAPE),
    TOKEN("[^\\p{javaWhitespace}]+"),
    
    PRIMITIVE("[BCDFIJSZ]"),
    JAVA_ID("\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*"),
    GENERATED_LABEL("@%s",JAVA_ID),
    LABEL("%s|%s",JAVA_ID,GENERATED_LABEL),
    UNQUALIFIED_NAME(JAVA_ID),
    OP_ID(JAVA_ID),

    MODULE_ID(JAVA_ID),
    METHOD_ID(JAVA_ID),
    CLASS_NAME(Style.CLASS_NAME,"%s(/%s)*", UNQUALIFIED_NAME,UNQUALIFIED_NAME),
    KEY_NAME(CLASS_NAME),
    PACKAGE_NAME(Style.PACKAGE_NAME,CLASS_NAME),
    INNER_CLASS_NAME(Style.CLASS_NAME,JAVA_ID),
    CLASS_NAME_IN_MODULE(Style.CLASS_NAME,"%s(/%s)+", UNQUALIFIED_NAME,UNQUALIFIED_NAME),
    OWNER_VALUE_NAME(CLASS_NAME_IN_MODULE),
    PARM_VALUE_NAME("(%s|%s)",PRIMITIVE,CLASS_NAME_IN_MODULE),
    MODULE_NAME("%s(\\.%s)*", MODULE_ID,MODULE_ID),
    CLASS_PARM("L%s;",CLASS_NAME),
    ARRAY_DESC(Style.ARRAY_DESC,"\\[+(%s|%s)",PRIMITIVE,CLASS_PARM),
    FRAME_NAME("(%s|%s)", ARRAY_DESC, CLASS_NAME),
    FIELD_NAME(Style.FIELD_NAME,"%s",UNQUALIFIED_NAME),
    FIELD_DESC("\\[*(%s|%s)",PRIMITIVE,CLASS_PARM),
    INTERFACE_METHOD_NAME(Style.METHOD_NAME,METHOD_ID),
    METHOD_NAME(Style.METHOD_NAME,"(%s|%s|%s)",STATIC_INIT_NAME,CLASS_INIT_NAME,METHOD_ID),
    PARMS("\\((%s)*\\)",FIELD_DESC),
    DESC(Style.DESC,"%s(V|%s)",PARMS,FIELD_DESC),
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
    CLASS_SIGNATURE(Style.CLASS_SIGNATURE,"%s+",SIGNATURE_PART),
    METHOD_SIGNATURE(Style.METHOD_SIGNATURE,"%s*\\(%s*\\)%s+",SIGNATURE_PART,SIGNATURE_PART,SIGNATURE_PART),
    FIELD_SIGNATURE(Style.FIELD_SIGNATURE,"%s+",SIGNATURE_PART),
    ;

    private final String regex;
    private final Pattern pattern;
    private final Style style;
    
    private NameDesc(String regex) {
        this.regex = regex;
        this.pattern = Pattern.compile(regex);
        this.style = null;
    }

    private NameDesc(Style style, String format, NameDesc... nds) {
        int n = nds.length;
        Object[] strings = new Object[n];
        for (int i = 0; i < n; ++i) {
            strings[i] = nds[i].regex;
        }
        this.regex = String.format(format, strings);
        this.pattern = Pattern.compile(this.regex);
        this.style = style;
    }

    private NameDesc(EnumSet<Constants> constants) {
        this.regex = constants.stream()
                .map(Constants::regex)
                .collect(Collectors.joining("|", "(", ")"));
        this.pattern = Pattern.compile(this.regex);
        this.style = null;
    }

    private NameDesc(NameDesc model) {
        this(model.regex);
    }
    
    private NameDesc(String format, NameDesc... nds) {
        this(null,format,nds);
    }

    
    private NameDesc(Style style, NameDesc model) {
        this.regex = model.regex;
        this.pattern = Pattern.compile(regex);
        this.style = style;
    }

    // incomplete test
    public static boolean isJavaBase(String str) {
        return isJava(str);
    }
    
    public static boolean isJava(String str) {
        return str.startsWith("java/") || str.startsWith("javax/");
    }

    public boolean isValid(String str) {
        if (str == null) {
            throw new LogIllegalArgumentException(M177, this); // "missing %s"
        }
        return pattern.matcher(str).matches();
    }

    public boolean validate(String str) {
        boolean ok = isValid(str);
        if (ok) {
            if (style != null) {
                style.check(str);
            }
        } else {
            LOG(M66,this,str);   // "invalid %s: %s"
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
                        && !name.equalsIgnoreCase(name)
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

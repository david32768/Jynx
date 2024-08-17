package jynx2asm;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;

import static jvm.NumType.*;
import static jynx.Global.*;
import static jynx.Message.*;

import jvm.ConstType;
import jvm.HandleType;
import jvm.NumType;
import jynx.StringUtil;
import jynx2asm.handles.JynxHandle;

public class String2Object {

    public String2Object() {}
    
    public boolean parseBoolean(String token) {
        token = token.toLowerCase();
        if (token.equals("true")) return true;
        if (token.equals("false")) return false;
        long val = decodeLong(token, t_boolean);
        return val != 0;
    }
  
    public char parseCharacter(String token) {
        int tokenlen = token.length();
        char first = token.charAt(0);
        char last = token.charAt(tokenlen - 1);
        if (first == '\'' && first == last) {
            String charstr = tokenlen > 1?StringUtil.unescapeSequence(token.substring(1, tokenlen - 1)):"";
            if (charstr.length() == 1) {
                return charstr.charAt(0);
            } else {
                LOG(M38,token);     // "%s is not a valid char literal - blank assumed"
                return ' ';
            }
        }
        return (char)decodeLong(token, t_char);
    }
    
    public long decodeLong(String token, NumType nt) {
        if ((nt == NumType.t_long || nt == NumType.t_int) && isUnsigned(token)) {
            return decodeUnsignedLong(token, nt);
        }
        token = removeL(token, nt);
        try {
            long var = parseLong(token);
            nt.checkInRange(var);
            return var;
        } catch(NumberFormatException nex) {
            // "%s%n  zero assumed"
            LOG(M157, nex.getMessage());
            return 0L;
        }
    }

    private long parseLong(String token) {
        if (token.toUpperCase().startsWith("0X")) {
            return Long.parseLong(token.substring(2), 16);
        } else {
            return Long.parseLong(token);
        }
    }
    
    public long decodeUnsignedLong(String token, NumType nt) {
        token = removeL(token, nt);
        token = removeLastIf(token, 'U');
        try {
            long var = parseUnsignedLong(token);
            nt.checkInUnsignedRange(var);
            return var;
        } catch(NumberFormatException nex) {
            // "%s%n  zero assumed"
            LOG(M157, nex.getMessage());
            return 0L;
        }
    }

    private long parseUnsignedLong(String token) {
        if (token.toUpperCase().startsWith("0X")) {
            return Long.parseUnsignedLong(token.substring(2), 16);
        } else {
            return Long.parseUnsignedLong(token);
        }
    }
    
    public Float parseFloat(String token) {
        token = removeLastIf(token, 'F');
        try {
            return Float.valueOf(token);
        } catch(NumberFormatException nex) {
            // "%s%n  zero assumed"
            LOG(M157, nex.getMessage());
            return 0.0F;
        }
    }
    
    public Double parseDouble(String token) {
        token = removeLastIf(token, 'D');
        try {
            return Double.valueOf(token);
        } catch(NumberFormatException nex) {
            // "%s%n  zero assumed"
            LOG(M157, nex.getMessage());
            return 0.0;
        }
    }
    
    public Type parseType(String token) {
        token = TRANSLATE_TYPE(token, true);
        NameDesc.FIELD_DESC.validate(token);
        return Type.getType(token);
    }

    public Handle parseHandle(String token) {
        return JynxHandle.getHandle(token);
    }

    private String removeL(String token, NumType nt) {
        int lastindex = token.length() - 1;
        char lastch = token.charAt(lastindex);
        if (isEqualIgnoreCase(lastch, 'L')) {
            if (nt != NumType.t_long) {
                // "value %s has 'L' suffix but must be a %s constant"
                LOG(M113, token, nt);
            }
            return token.substring(0, lastindex);
        }
        return token;
    }
    
    private String removeLastIf(String token, char dfl) {
        int lastindex = token.length() - 1;
        char lastch = token.charAt(lastindex);
        if (isEqualIgnoreCase(lastch, dfl)) {
            return token.substring(0, lastindex);
        }
        return token;
    }
    
    private boolean isEqualIgnoreCase(char x, char y) {
        return Character.toUpperCase(x) == Character.toUpperCase(y);
    }
    
     private boolean isUnsigned(String token) {
        token = removeLastIf(token, 'L');
        return isEqualIgnoreCase(token.charAt(token.length() - 1), 'U');
    }
     
    private ConstType typeConstant(String constant) {
        assert !constant.isEmpty();
        char typeconstant = constant.charAt(0);
        int index = "+-0123456789".indexOf(typeconstant);
        if (index >= 0) {
            String lcstr = constant.toLowerCase();
            if (index <= 1 && (lcstr.startsWith("nan",1) || lcstr.startsWith("inf",1))) {
                    return ConstType.ct_double;
            }
            if (lcstr.startsWith("0x") && !lcstr.contains(".") &&  !lcstr.contains("p")) {
                return ConstType.ct_long;
            }
            if (lcstr.contains(".") || lcstr.contains("e") || lcstr.contains("p")) {
                return ConstType.ct_double;
            }
            return ConstType.ct_long;
        }
        switch(typeconstant) {
            case '\"':
                return ConstType.ct_string;
            case '(':
                return ConstType.ct_method_type;
            case'\'':
                return ConstType.ct_char;
            default:    // class or method handle
                if (constant.indexOf(HandleType.SEP) >= 0) { // method handle
                    return ConstType.ct_method_handle;
                }
                return ConstType.ct_class;
        }
    }
    
    public Object getConst(Token token) {
        String constant = token.asString();
        if (constant.isEmpty()) {
            throw new AssertionError();
        }
        if (constant.equals("true")) {
            return 1;
        }
        if (constant.equals("false")) {
            return 0;
        }

        ConstType consttype = typeConstant(constant);
        char last = constant.toUpperCase().charAt(constant.length() - 1);
        switch(consttype) {
            case ct_long:
                // NB cannot use ?: unless both cast to Object
                if (last == 'L') {
                    return token.asLong();
                } else {
                    return token.asInt();
                }
            case ct_double:
                // NB cannot use ?: unless both cast to Object
                if (last == 'F') {
                    return token.asFloat();
                }
                return token.asDouble();
            case ct_char:
                return (int)token.asChar(); // Integer needed for invokedynamic parameters
            default:
                if (consttype == ConstType.ct_object) {
                    throw new AssertionError(); // to prevent infinite loop
                }
                return token.getValue(consttype);
        }
    }
    
}

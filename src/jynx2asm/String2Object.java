package jynx2asm;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;

import static jvm.NumType.*;
import static jynx.Global.*;
import static jynx.Message.*;
import static jynx2asm.NameDesc.CLASS_NAME;

import jvm.ConstType;
import jvm.HandleType;
import jvm.NumType;
import jynx.GlobalOption;
import jynx.StringUtil;
import jynx2asm.handles.JynxHandle;

public class String2Object {

    private final boolean ulong;
    
    public String2Object() {
        this.ulong = OPTION(GlobalOption.__UNSIGNED_LONG);
    }
    
    public boolean parseBoolean(String token) {
        token = token.toLowerCase();
        if (token.equals("true")) return true;
        if (token.equals("false")) return false;
        long val = decodeLong(token, t_boolean);
        return val == 1;
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
        long var;
        if (token.toUpperCase().startsWith("0X")) {
            var = Long.parseUnsignedLong(token.substring(2), 16);
        } else {
            var = Long.parseLong(token);
        }
        nt.checkInRange(var);
        return var;
    }

    public long decodeUnsignedLong(String token, NumType nt) {
        long var;
        if (token.toUpperCase().startsWith("0X")) {
            var = Long.parseUnsignedLong(token.substring(2), 16);
        } else {
            var = Long.parseUnsignedLong(token);
        }
        nt.checkInUnsignedRange(var);
        return var;
    }

    public Float parseFloat(String token) {
        return Float.parseFloat(token);
    }
    
    public Double parseDouble(String token) {
        return Double.parseDouble(token);
    }
    
    public Type parseType(String token) {
        token = TRANSLATE_TYPE(token, true);
        NameDesc.FIELD_DESC.validate(token);
        return Type.getType(token);
    }

    public Handle parseHandle(String token) {
        return JynxHandle.getHandle(token);
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
        char first = constant.charAt(0);
        char last = constant.toUpperCase().charAt(constant.length() - 1);
        switch(consttype) {
            case ct_long:
                Long lval = ulong && first != '-'? token.asUnsignedLong(): token.asLong();
                if (last != 'L') {
                    boolean unsigned = constant.toUpperCase().startsWith("0X");
                    if (NumType.t_int.isInRange(lval)
                            || unsigned && NumType.t_int.isInUnsignedRange(lval)) {
                            return lval.intValue();
                    } else {
                        // "value %s requires 'L' suffix as is a long constant"
                        LOG(M113,lval);
                    }
                }
                return lval;
            case ct_double:
                // NB cannot use ?: unless both cast to Object
                if (last == 'F') {
                    return token.asFloat();
                }
                return token.asDouble();
            case ct_char:
                return (int)token.asChar(); // Integer needed for invokedynamic parameters
            default:
                return consttype.getValue(token);
        }
    }
    
}

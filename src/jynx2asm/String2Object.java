package jynx2asm;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;

import static jvm.HandleType.REF_newInvokeSpecial;
import static jvm.HandleType.SEP;
import static jvm.NumType.*;
import static jynx.Global.*;
import static jynx.Message.*;

import jvm.ConstType;
import jvm.HandleType;
import jvm.NumType;
import jynx.GlobalOption;
import jynx.LogIllegalArgumentException;
import jynx.StringUtil;

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
        char first = token.charAt(0);
        char last = token.charAt(token.length() - 1);
        boolean array = first == '[';
        boolean type = first == 'L' && last == ';';
        boolean primitive = ConstType.isPrimitiveType(token);
        if (array || type || primitive) {
            return Type.getType(token);
        }
        return Type.getObjectType(token);
    }

    public Handle parseHandle(String token) {
        int colon = token.indexOf(SEP);
        if (colon < 0) {
            // "Separator '%s' not found in %s"
            throw new LogIllegalArgumentException(M99,SEP,token);
        }
        String htag = token.substring(0,colon);
        HandleType ht = HandleType.fromMnemonic(htag);
        String handle = token.substring(colon + 1);
        OwnerNameDesc ond = OwnerNameDesc.getInstance(handle,ht);
        
        String desc = ond.getDesc();
        if (!ht.isField() && ond.isStaticInit() || (ht == REF_newInvokeSpecial) != ond.isInit()) {
            // "method %s invalid for %s"
            throw new LogIllegalArgumentException(M102,ond.getName(),ht);
        }
        return new Handle(ht.reftype(),ond.getOwner(), ond.getName(), desc, ond.isOwnerInterface());
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
        char first = constant.charAt(0);
        char last = constant.toUpperCase().charAt(constant.length() - 1);

        char typeconstant = first;
        int index = "+-0123456789".indexOf(typeconstant);
        if (index >= 0) {
            typeconstant = '0';
            if (index <= 1) {   // "+-"
                String con1 = constant.substring(1).toLowerCase(); // remove +- for NaN/Infinity test 
                if (con1.startsWith("nan") || con1.startsWith("inf")) {
                    typeconstant = '.';
                }
            }
            if (constant.toUpperCase().startsWith("0X") && !constant.contains(".") &&  !constant.toLowerCase().contains("p")) {
                typeconstant = '0';
            } else if (constant.contains(".") || constant.toLowerCase().contains("e") || constant.toLowerCase().contains("p")) {
                typeconstant = '.';
            }
        }
        switch(typeconstant) {
            case '\"':
                return token.asQuoted();
            case '0':
                Long lval = ulong && first != '-'?token.asUnsignedLong():token.asLong();
                if (last != 'L') {
                    boolean unsigned = constant.toUpperCase().startsWith("0X");
                    if (NumType.t_int.isInRange(lval)
                            || unsigned && NumType.t_int.isInUnsignedRange(lval)) {
                            return lval.intValue();
                    }
                }
                return lval;
            case '.':
                if (last == 'F') {
                    return token.asFloat();
                }
                return token.asDouble();
            case '(':
                return token.asMethodType();
            case'\'':
                return (int)token.asChar();
            default:    // class or method handle
                if (constant.indexOf(HandleType.SEP) >= 0) { // method handle
                    return token.asHandle();
                }
                return token.asType();
        }
    }
    
}

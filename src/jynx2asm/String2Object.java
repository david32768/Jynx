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
import jynx.LogIllegalArgumentException;
import jynx.StringUtil;

public class String2Object {

    public String2Object() {
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
        if (token.length() == 16 + 2 && token.toUpperCase().startsWith("0X")) {
            var = Long.parseUnsignedLong(token.substring(2), 16);
        } else if (token.length() == 16 + 1 && token.startsWith("#")) {
            var = Long.parseUnsignedLong(token.substring(1), 16);
        } else {
            var = Long.parseLong(token);
        }
        nt.checkInRange(var);
        return var;
    }

    public long decodeUnsignedLong(String token, NumType nt) {
        long var;
        if (token.length() == 16 + 2 && token.toUpperCase().startsWith("0X")) {
                var = Long.parseUnsignedLong(token.substring(2), 16);
        } else if (token.length() == 16 + 1 && token.startsWith("#")) {
            var = Long.parseUnsignedLong(token.substring(1), 16);
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
        
        OwnerNameDesc mdesc = OwnerNameDesc.getOwnerMethodDescAndCheck(token.substring(colon + 1),ht.op());
        
        String desc = ht.isField()? mdesc.getDesc().substring(2): mdesc.getDesc();   // remove () from getfield etc;
        boolean notInit = ht.isInvoke() && ht != REF_newInvokeSpecial;
        if (notInit && ((mdesc.isInit() || mdesc.isStaticInit()) || (ht == REF_newInvokeSpecial && !mdesc.isInit()))) {
            // "method %s invalid for %s"
            throw new LogIllegalArgumentException(M102,mdesc.getName(),ht);
        }
        return new Handle(ht.reftype(),mdesc.getOwner(), mdesc.getName(), desc, mdesc.isOwnerInterface());
    }
    
    public Object getConst(Token token) {
        String constant = token.asString();
        if (constant.equals("true")) {
            return 1;
        }
        if (constant.equals("false")) {
            return 0;
        }
        char typeconstant = constant.charAt(0);
        int index = "+-0123456789.".indexOf(typeconstant);
        if (index >= 0) {
            typeconstant = '0';
            if (index <= 1) {   // "+-"
                String con1 = constant.substring(1); // remove +- for NaN/Infinity test 
                if (con1.startsWith("NaN") || con1.startsWith("Infinity")) {
                    typeconstant = '.';
                }
            }
            if (constant.contains(".") || constant.toLowerCase().contains("e") || constant.toLowerCase().contains("p")) {
                typeconstant = '.';
            }
        }
        char last = constant.toUpperCase().charAt(constant.length() - 1);
        switch(typeconstant) {
            case '\"':
                return token.asQuoted();
            case '0':
                Long lval = token.asLong();
                if (last == 'L' || lval.longValue() != lval.intValue()) {
                    return lval;
                }
                return lval.intValue();
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

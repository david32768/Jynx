package jynx2asm;

import java.util.EnumSet;
import java.util.Optional;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;

import static jynx.Global.*;
import static jynx.Message.*;

import jvm.ConstType;
import jvm.NumType;
import jynx.ClassType;
import jynx.Directive;
import jynx.LogIllegalArgumentException;
import jynx.LogIllegalStateException;
import jynx.ReservedWord;
import jynx.StringUtil;

public class Token {

    public final static Token END_TOKEN = new Token("\n     END_TOKEN");
    public final static Token END_CLASS_TOKEN = new Token("." + Directive.end_class.name());
    private final static String2Object S2O = new String2Object();
    
    private final static int  MAX_UTF8_STRING = 2*Short.MAX_VALUE + 1;
    
    private final String token;

    private Token(String token) {
        if (token == null) {
            throw new NullPointerException();
        }
        // remove '\'' only when contains unicode escape 
        if (token.contains("\\u") && token.charAt(0) == '\'' && token.charAt(token.length() - 1) == '\'') {
            token = token.substring(1, token.length() - 1);
            token = StringUtil.unescapeUnicode(token);
        }
        this.token = token;
    }

    public static Token getInstance(String tokenstr) {
        Token token =  new Token(tokenstr);
        if (token.equals(END_TOKEN)) {
            return END_TOKEN;
        }
        return token;
    }

    public String asString() {
        long len = token.length();
        if (len > MAX_UTF8_STRING/3 && StringUtil.modifiedUTF8Length(token) > MAX_UTF8_STRING) {
            // "String length of %d exceeds maximum %d"
            throw new LogIllegalArgumentException(M179,len,MAX_UTF8_STRING);
        }
        return token;
    }

    public String asLabel() {
        int index = token.indexOf(':');
        if (index !=  token.length() - 1 || token.contains(" ")) {
            LOG(M49,token);  // "Invalid label - %s"
        }
        return token.substring(0, index);
    }
    
    public String asQuoted() {
        return StringUtil.unescapeString(token);
    }
    
    public boolean asBoolean() {
        return S2O.parseBoolean(token);
    }
    
    public byte asByte() {
        return (byte)S2O.decodeLong(token, NumType.t_byte);
    }

    public char asChar() {
        return S2O.parseCharacter(token);
    }
    
    public short asShort() {
        return (short)S2O.decodeLong(token, NumType.t_short);
    }
    
    public int asInt() {
        if (token.charAt(0) == '\'') {
            return (int)asChar();
        }
        return (int)S2O.decodeLong(token, NumType.t_int);
    }

    private String removeLastIf(char dfl) {
        int lastindex = token.length() - 1;
        char lastch = token.charAt(lastindex);
        if (Character.toUpperCase(lastch) == Character.toUpperCase(dfl)) {
            return token.substring(0, lastindex);
        }
        return token;
    }
    
    public long asLong() {
        return S2O.decodeLong(removeLastIf('L'),NumType.t_long);
    }
    
    public float asFloat() {
        return S2O.parseFloat(removeLastIf('F'));
    }
    
    public double asDouble() {
        return S2O.parseDouble(removeLastIf('D'));
    }
    
    public Type asType() {
        return S2O.parseType(token);
    }
    
    public Type asMethodType() {
        return Type.getType(token);
    }
    
    public Handle asHandle() {
        return S2O.parseHandle(token);
    }

    public int asUnsignedInt() {
        return (int)S2O.decodeUnsignedLong(token, NumType.t_int);
    }
    
    public int asUnsignedShort() {
        return (int)S2O.decodeUnsignedLong(token, NumType.t_short);
    }
    
    public short asUnsignedByte() {
        return (short)S2O.decodeUnsignedLong(token, NumType.t_byte);
    }
    
    public Optional<Integer> asOptInt() {
        if (this == END_TOKEN) {
            return Optional.empty();
        }
        return Optional.of(asInt());
    }


    public Object getValue(ConstType ct) {
        return ct.getValue(this);
    }
    
    public Object getConst() {
        return S2O.getConst(this);
    }
    
    public String asName() {
        String name = token;
        char start = name.charAt(0);
        if (start == '\'' || start == '\"') {
            int lastind = name.lastIndexOf(start);
            if (lastind == name.length() - 1) {
                name = name.substring(1, lastind);
            }
        }
        if (name.isEmpty()) {
            // "zero length name"
            throw new LogIllegalArgumentException(M152);
        }
        return name;
    }

    public int asTypeCode() {
        NumType type = NumType.fromString(token)
                .orElseThrow(() -> new LogIllegalArgumentException(M159, token)); // "Invalid type - %s"
        return type.typecode();
    }

    public Directive asDirective() {
        if (token.startsWith(".")) {
            String dirstr = token.substring(1);
            return  Directive.getDirInstance(dirstr)
                        .orElseThrow(()->new LogIllegalStateException(M245,dirstr)); // "Unknown directive = %s";
        }
        return null;
    }

    public ClassType asInnerClassType() {
        return ClassType.getInnerClassType(token).orElseThrow(() -> new LogIllegalArgumentException(M172, token)); // "Invalid class type - %s"
    }
    
    public ReservedWord mayBe(ReservedWord res1,ReservedWord... res) {
        EnumSet<ReservedWord> rwset = EnumSet.of(res1,res);
        ReservedWord word = ReservedWord.getOptInstance(token);
        if (word == null || !rwset.contains((word))) {
            return null;
        }
        return word;
    }
    
    public boolean is(ReservedWord res) {
        return res.isString(token);
    }
    
    public void mustBe(ReservedWord res) {
        if (!res.isString(token)) {
            throw new LogIllegalStateException(M109,res, token); // "reserved word %s expected but found %s"
        }
    }
    
    public ReservedWord oneOf(ReservedWord res1,ReservedWord... res) {
        ReservedWord word = mayBe(res1,res);
        if (word == null) {
            EnumSet<ReservedWord> rwset = EnumSet.of(res1,res);
            throw new LogIllegalStateException(M109,rwset, token); // "reserved word %s expected but found %s"
        }
        return word;
    }
    
    public ReservedWord expectOneOf(ReservedWord res1,ReservedWord... res) {
        ReservedWord rw = mayBe(res1, res);
        if (rw == null) {
            EnumSet<ReservedWord> rwset = EnumSet.of(res1,res);
            LOG(M109,rwset, token); // "reserved word %s expected but found %s"
        }
        return rw;
    }

    @Override
    public String toString() {
        if (this == END_TOKEN) {
            return "\\n";
        }
        return token;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Token) {
            Token other = (Token)obj;
            return token.equals(other.token);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return token.hashCode();
    }

}

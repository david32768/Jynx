package jynx2asm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.Type;

import static jynx.Message.M241;
import static jynx.Message.M58;
import static jynx.Message.M82;
import static jynx.ReservedWord.res_common;
import static jynx.ReservedWord.res_subtypes;
import static jynx.ReservedWord.right_array;

import jynx.Global;
import jynx.ReservedWord;

public class TypeHints {

    private final Map<String,Set<String>> subtypes;
    private final Map<String,Map<String,String>> commons;

    public TypeHints() {
        this.subtypes = new HashMap<>();
        this.commons = new HashMap<>();
    }
    
    public void setHints(TokenArray dotarray) {
        while (true) {
            Token token = dotarray.firstToken();
            if (token.is(right_array)) {
                break;
            }
            
            String sub = token.asString();
            NameDesc.CLASS_NAME.validate(sub);
            ReservedWord rw = dotarray.nextToken()
                    .expectOneOf(res_common,res_subtypes);
            String base;
            switch (rw) {
                case res_common:
                    String common = sub;
                    base = dotarray.nextToken().asString();
                    NameDesc.CLASS_NAME.validate(base);
                    String base2 = dotarray.nextToken().asString();
                    NameDesc.CLASS_NAME.validate(base);
                    addCommon(common, base, base2);
                    addCommon(common, common, base2);
                    addCommon(common, common, base);
                    addSubtype(base,common);
                    addSubtype(base2,common);
                    break;
                case res_subtypes:
                    base = dotarray.nextToken().asString();
                    NameDesc.CLASS_NAME.validate(base);
                    addSubtype(sub, base);
                    addCommon(base,base,sub);
                    break;
                default:
                    throw new AssertionError();
            }
            dotarray.noMoreTokens();
        }
    }
    
    private void addSubtype(String sub, String base) {
        subtypes.computeIfAbsent(base, k-> new HashSet<>()).add(sub);
    }
    
    private void addCommon(String common, String name1,String name2) {
        if (name1.compareTo(name2) > 0) {
            String temp = name1;
            name1 = name2;
            name2 = temp;
        }
        String previous = commons.computeIfAbsent(name1, k->new HashMap<>()).putIfAbsent(name2,common);
        if (previous != null && !previous.equals(common)) {
            // "ambiguous hint for common supertype of %s and %s%n    %s and %s"
            Global.LOG(M241,name1,name2,common,previous);
        }
    }

    private static final Type OBJECT = Type.getObjectType("java/lang/Object");
    
    public boolean isSubTypeOf(final BasicValue value, final BasicValue  expected) {
        Type subtype = value.getType();
        Type basetype = expected.getType();
        if (subtype.equals(basetype)) {
            return true;
        }
        if (value.isReference() && expected.isReference()) {
            if (basetype.equals(OBJECT)) {
                return true;
            }
            if (subtype.getSort() == Type.ARRAY && basetype.getSort() == Type.ARRAY) {
                Type subelement = subtype.getElementType();
                Type baseelement = basetype.getElementType();
                if (baseelement.equals(OBJECT) && subelement.getSort() == Type.OBJECT) {
                    return subtype.getDimensions() >= basetype.getDimensions();
                }
            }
        }
        String sub = subtype.getInternalName();
        String base = basetype.getInternalName();
        if (subtypes.computeIfAbsent(base, k-> new HashSet<>()).contains(sub)) {
            Global.LOG(M58, sub,res_subtypes,base); // "used hint: %s %s %s"
            return true;
        }
        return false;
    }

    public Type getCommonType(final Type type1, final Type type2) {
        String common = getCommonSuperClass(type1.getInternalName(), type2.getInternalName());
        if (common == null) {
            return null;
        }
        return Type.getObjectType(common);
    }
    
    public String getCommonSuperClass(String name1, String name2) {
        if (name1.compareTo(name2) > 0) {
            String temp = name1;
            name1 = name2;
            name2 = temp;
        }
        String common = commons.computeIfAbsent(name1,k->new HashMap<>()).get(name2);
        if (common != null) {
            Global.LOG(M82,common,res_common,name1,name2); // "used hint: %s %s %s %s"
        }
        return common;
    }
}

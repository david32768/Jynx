package jynx2asm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.Type;

import static jynx.Global.LOG;
import static jynx.Global.OPTION;
import static jynx.GlobalOption.ALLOW_CLASS_FORNAME;
import static jynx.Message.M151;
import static jynx.Message.M241;
import static jynx.Message.M403;
import static jynx.Message.M404;
import static jynx.Message.M58;
import static jynx.Message.M82;
import static jynx.ReservedWord.res_common;
import static jynx.ReservedWord.res_subtypes;
import static jynx.ReservedWord.right_array;

import jvm.ConstType;
import jynx.Global;
import jynx.ReservedWord;

public class TypeHints {

    private final Map<String,Set<String>> subtypes;
    private final Map<String,Map<String,String>> commons;
    private final boolean forname;

    public TypeHints() {
        this.subtypes = new HashMap<>();
        this.commons = new HashMap<>();
        this.forname = OPTION(ALLOW_CLASS_FORNAME);
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
            switch (rw) {
                case res_common:
                    String common = sub;
                    String base1 = dotarray.nextToken().asString();
                    NameDesc.CLASS_NAME.validate(base1);
                    String base2 = dotarray.nextToken().asString();
                    NameDesc.CLASS_NAME.validate(base1);
                    addCommon(common, base1, base2);
                    addCommon(common, common, base2);
                    addCommon(common, common, base1);
                    addSubtype(base1,common);
                    addSubtype(base2,common);
                    break;
                case res_subtypes:
                    String base = dotarray.nextToken().asString();
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

    private boolean isPrimitive(Type type) {
        return ConstType.isPrimitiveType(type.getInternalName());
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
                if (isPrimitive(subelement) || isPrimitive(baseelement)) {
                    return false;
                }
            }
        }
        String sub = subtype.getInternalName();
        String base = basetype.getInternalName();
        if (subtypes.computeIfAbsent(base, k-> new HashSet<>()).contains(sub)) {
            Global.LOG(M58, sub,res_subtypes,base); // "used hint: %s %s %s"
            return true;
        }
        // "(redundant?) checkcast or hint needed if %s is subtype of %s"
        LOG(M403,subtype.getInternalName(),basetype.getInternalName());
        return false;
    }

    public BasicValue merge(BasicValue value1, BasicValue value2, String typename) {
        Type type1 = value1.getType();
        Type type2 = value2.getType();
        Type common = getCommonType(type1, type2);
         if (common != null) {
             return new BasicValue(common);
         }
         // "(redundant?) checkcasts or hint needed to obtain common supertype of%n    %s and %s"
         LOG(M404,value1.getType().getInternalName(),value2.getType().getInternalName());
         throw new TypeNotPresentException(typename, null);
    }

    private Type getCommonType(final Type type1, final Type type2) {
        if (type1.equals(OBJECT) || type2.equals(OBJECT)) {
            return OBJECT;
        }
        String common = getCommonSuperClass(type1.getInternalName(), type2.getInternalName());
        if (common == null) {
            return null;
        }
        return Type.getObjectType(common);
    }
    
    public String getCommonSuperClass(String name1, String name2) {
        if (useClassForName(name1) && useClassForName(name2)) {
           return null;
        }
        if (name1.compareTo(name2) > 0) {
            String temp = name1;
            name1 = name2;
            name2 = temp;
        }
        String common = commons.computeIfAbsent(name1,k->new HashMap<>()).get(name2);
        if (common == null) {
            // "(redundant?) checkcasts or hint needed to obtain common supertype of%n    %s and %s"
            LOG(M404, name1,name2);
            throw new TypeNotPresentException(name1 + " or " + name2, null);
        } else {
            Global.LOG(M82,common,res_common,name1,name2); // "used hint: %s %s %s %s"
        }
        return common;
    }

    private boolean useClassForName(final String base) {
        if (NameDesc.isJava(base) || ConstType.isPrimitiveType(base)) {
            return true;
        } else if (forname) {
            // "Class.forName(%s) has been used"
            LOG(M151,base);
            return true;
        } else {
            return false;
        }
    }
    
    public boolean useClassForName(final Type type) {
        String name = type.getInternalName();
        String base;
        if (type.getSort() == Type.ARRAY) {
            base = type.getElementType().getInternalName();
        } else {
            base = name;
        }
        return useClassForName(base);
    }

}

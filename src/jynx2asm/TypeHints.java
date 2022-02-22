package jynx2asm;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.Type;

import static jynx.Global.LOG;
import static jynx.Message.M58;
import static jynx.Message.M87;
import static jynx.ReservedWord.res_assignable;
import static jynx.ReservedWord.res_class;
import static jynx.ReservedWord.res_common;
import static jynx.ReservedWord.res_extends;
import static jynx.ReservedWord.res_interface;
import static jynx.ReservedWord.res_to;
import static jynx.ReservedWord.right_array;

import jynx.Global;
import jynx.ReservedWord;

public class TypeHints {

    private final Map<String,HintClass> hintClass;

    public TypeHints() {
        this.hintClass = new HashMap<>();
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
                    .expectOneOf(res_extends,res_class,res_interface,res_common,res_assignable);
            if (rw == null) {
                continue;
            }
            String base;
            switch (rw) {
                case res_common:
                    String common = sub;
                    base = dotarray.nextToken().asString();
                    NameDesc.CLASS_NAME.validate(base);
                    String base2 = dotarray.nextToken().asString();
                    NameDesc.CLASS_NAME.validate(base);
                    addCommon(common, base, base2);
                    break;
                case res_extends:
                    base = dotarray.nextToken().asString();
                    NameDesc.CLASS_NAME.validate(base);
                    addExtends(sub, base);
                    break;
                case res_assignable:
                    dotarray.nextToken().mustBe(res_to);
                    base = dotarray.nextToken().asString();
                    NameDesc.CLASS_NAME.validate(base);
                    addAssignable(base, sub);
                    break;
                case res_class:
                    addClass(sub);
                    break;
                case res_interface:
                    addInterface(sub);
                    break;
                default:
                    throw new AssertionError();
            }
            dotarray.noMoreTokens();
        }
    }
    
    private HintClass hint4name(String name) {
        return hintClass.computeIfAbsent(name,HintClass::new).setAsClass();
    }
    
    private void addClass(String name) {
        hint4name(name).setAsClass();
    }
    
    private void addInterface(String name) {
        hint4name(name).setAsInterface();
    }
    
    private void addExtends(String sub, String base) {
        hint4name(sub).addExtends(base);
    }
    
    private void addCommon(String common, String name1,String name2) {
        HintClass hint1 = hint4name(name1);
        HintClass hint2 = hint4name(name2);
        hint1.addCommon(hint2, common);
        hint2.addCommon(hint1, common);
    }

    private void addAssignable(String to, String from) {
        hint4name(from).addAssignableTo(to);
    }
    
    public String getSuper(String sub) {
        return hint4name(sub).getSuperClass();
    }
    
    public boolean isKnownInterface(String name) {
        return hint4name(name).isInterface();
    }
    
    public boolean isKnownClass(String name) {
        return hint4name(name).isClass();
    }
    
    private static final Type OBJECT = Type.getObjectType("java/lang/Object");
    
    public boolean isAssignableFrom(final Type type1, final Type type2) {
        if (type1.equals(type2) || type1.equals(OBJECT)) {
            return true;
        }
        String to = type1.getInternalName();
        String from = type2.getInternalName();
        HintClass hint = hint4name(from);
        if (hint.isAssignable2(to)) {
            Global.LOG(M58, to,from); // "used hint for %s <- %s"
            return true;
        }
        // "add hint if %s is assignable from %s"
        LOG(M87, type1.getInternalName(), type2.getInternalName());
        return false;
    }
    
    public String getCommonSuperClass(final String type1, final String type2) {
        HintClass h1 = hint4name(type1);
        HintClass h2 = hint4name(type2);
        return h1.common(h2);
    }
}

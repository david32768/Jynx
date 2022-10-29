package jynx2asm.handles;

import java.util.EnumMap;
import java.util.EnumSet;

import static jynx.Message.M303;
import static jynx2asm.handles.HandlePart.DESC;
import static jynx2asm.handles.HandlePart.NAME;
import static jynx2asm.handles.HandlePart.OWNER;
import static jynx2asm.NameDesc.CLASS_NAME;
import static jynx2asm.NameDesc.METHOD_NAME;
import static jynx2asm.NameDesc.METHOD_NAME_DESC;

import jynx.Global;

public class EnclosingMethodHandle implements JynxHandle {

    private final String owner;
    private final String name;
    private final String desc;

    public EnclosingMethodHandle(String owner, String name, String desc) {
        this.owner = owner;
        this.name = name;
        this.desc = desc;
    }

    @Override
    public String owner() {
        return owner;
    }

    
    @Override
    public String name() {
        return name;
    }

    @Override
    public String desc() {
        return desc;
    }

    public static EnclosingMethodHandle getInstance(String mspec) {
        EnumMap<HandlePart,String> map = HandlePart.getInstance(mspec, EnumSet.of(OWNER,NAME,DESC));
        String owner = map.get(OWNER);
        String name = map.get(NAME);
        String desc = map.get(DESC);
        CLASS_NAME.validate(owner);
        if (owner.equals(Global.CLASS_NAME())) {
            Global.LOG(M303); // "Enclosing method cannot be in enclosed class"
        }
        METHOD_NAME.validate(name);
        METHOD_NAME_DESC.validate(name + desc);
        return new EnclosingMethodHandle(owner,name,desc);
    }

    @Override
    public String ond() {
        return HandlePart.ownerName(owner, name) + desc;
    }

    @Override
    public String toString() {
        return ond();
    }

}

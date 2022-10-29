package jynx2asm.handles;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Objects;

import org.objectweb.asm.Handle;

import static jynx.Global.CLASS_NAME;
import static jynx.Message.M275;
import static jynx2asm.handles.HandlePart.DESC;
import static jynx2asm.handles.HandlePart.NAME;
import static jynx2asm.handles.HandlePart.OWNER;
import static jynx2asm.NameDesc.CLASS_NAME;
import static jynx2asm.NameDesc.FIELD_DESC;
import static jynx2asm.NameDesc.FIELD_NAME;

import jvm.HandleType;
import jynx.LogIllegalArgumentException;

public class FieldHandle implements JynxHandle {

    private final String owner;
    private final String name;
    private final String desc;
    private final HandleType ht;

    public FieldHandle(String owner, String name, String desc, HandleType ht) {
        this.owner = owner;
        this.name = name;
        this.desc = desc;
        this.ht = ht;
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

    @Override
    public HandleType ht() {
        return ht;
    }
    
    @Override
    public String ond() {
        return HandlePart.ownerName(owner, name) + "()" + desc;
    }

    @Override
    public String toString() {
        return ht.getPrefix() + ond();
    }

    public void checkReference() {
        assert !owner.equals(CLASS_NAME());
        (new CheckReference(this)).check();
    }
    
    public static FieldHandle of(Handle handle) {
        HandleType ht = HandleType.of(handle);
        assert ht.isField();
        return new FieldHandle(handle.getOwner(),handle.getName(),handle.getDesc(),ht);
    } 

    private static FieldHandle getInstance(EnumMap<HandlePart,String> map, String desc, HandleType ht) {
        String owner = map.get(OWNER);
        String name = map.get(NAME);
        CLASS_NAME.validate(owner);
        FIELD_NAME.validate(name);
        FIELD_DESC.validate(desc);
        assert ht.isField();
        return new FieldHandle(owner,name,desc,ht);
    }

    public static FieldHandle getInstance(String ownername, String desc, HandleType ht) {
        EnumMap<HandlePart,String> map = HandlePart.getInstance(ownername,EnumSet.of(OWNER,NAME));
        return getInstance(map,desc,ht);
    }

    private static final String EMPTY_PARM = "()";

    public static FieldHandle getInstance(String mdesc, HandleType ht) {
        EnumMap<HandlePart,String> map = HandlePart.getInstance(mdesc,EnumSet.of(OWNER,NAME,DESC));
        String desc = map.remove(DESC);
        if (desc.indexOf(EMPTY_PARM) != 0) {
            // "descriptor '%s' for %s must start with '()'"
            throw new LogIllegalArgumentException(M275, desc , ht);
        }
        desc = desc.substring(2);
        return getInstance(map,desc,ht);
    }

    private boolean equals(FieldHandle other) {
        return owner.equals(other.owner) && name.equals(other.name) && desc.equals(other.desc) && ht == other.ht;
    }
    
    @Override
    public boolean equals(Object obj) {
        return obj instanceof FieldHandle && equals((FieldHandle)obj);
    }

    @Override
    public int hashCode() {
        return Objects.hash(owner,name,desc,ht);
    }

}

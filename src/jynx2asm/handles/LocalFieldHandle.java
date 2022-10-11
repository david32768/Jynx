package jynx2asm.handles;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Objects;

import static jynx2asm.handles.HandlePart.*;
import static jynx2asm.NameDesc.FIELD_DESC;
import static jynx2asm.NameDesc.FIELD_NAME;

public class LocalFieldHandle implements JynxHandle {

    private final String name;
    private final String desc;

    private LocalFieldHandle(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String desc() {
        return desc;
    }
    
    public static LocalFieldHandle getInstance(String ownername, String desc) {
        EnumMap<HandlePart,String> map = HandlePart.getInstance(ownername,EnumSet.of(NAME));
        String name = map.get(NAME);
        FIELD_NAME.validate(name);
        FIELD_DESC.validate(desc);
        return new LocalFieldHandle(name,desc);
    }

    @Override
    public String ond() {
        return String.format("%s()%s",name,desc);
    }

    @Override
    public String toString() {
        return String.format("%s %s",name,desc);
    }

    private boolean equals(LocalFieldHandle other) {
        return name.equals(other.name) && desc.equals(other.desc);
    }
    
    @Override
    public boolean equals(Object obj) {
        return obj instanceof LocalFieldHandle && equals((LocalFieldHandle)obj);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name,desc);
    }

}

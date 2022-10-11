package jynx2asm.handles;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Objects;

import org.objectweb.asm.tree.MethodNode;

import static jynx2asm.handles.HandlePart.DESC;
import static jynx2asm.handles.HandlePart.NAME;
import static jynx2asm.NameDesc.METHOD_NAME;
import static jynx2asm.NameDesc.METHOD_NAME_DESC;

public class LocalMethodHandle implements JynxHandle, Comparable<LocalMethodHandle> {

    private final String name;
    private final String desc;

    public LocalMethodHandle(String name, String desc) {
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

    public boolean isInit() {
        return HandlePart.isInit(name, desc);
    }
    
    public static LocalMethodHandle getInstance(String mspec) {
        EnumMap<HandlePart,String> map = HandlePart.getInstance(mspec, EnumSet.of(NAME,DESC));
        String name = map.get(NAME);
        String desc = map.get(DESC);
        METHOD_NAME.validate(name);
        METHOD_NAME_DESC.validate(name + desc);
        return new LocalMethodHandle(name,desc);
    }

    public static LocalMethodHandle of(MethodNode mn) {
        return new LocalMethodHandle(mn.name, mn.desc);
    }
    
    @Override
    public String ond() {
        return name + desc;
    }

    @Override
    public String toString() {
        return name + desc;
    }

    @Override
    public int compareTo(LocalMethodHandle other) {
        int result = name.compareTo(other.name);
        if (result == 0) {
            result = desc.compareTo(other.desc);
        }
        return result;
    }

    private boolean equals(LocalMethodHandle other) {
        return name.equals(other.name) && desc.equals(other.desc);
    }
    
    @Override
    public boolean equals(Object obj) {
        return obj instanceof LocalMethodHandle && equals((LocalMethodHandle)obj);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name,desc);
    }

}

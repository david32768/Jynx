package jvm;

public interface Attribute extends JvmVersioned {

    AttributeEntry[] entries();

    boolean inContext(Context context);

    boolean isUnique();

    String name();
    
    @Override
    JvmVersionRange range();

    AttributeType type();
    
}

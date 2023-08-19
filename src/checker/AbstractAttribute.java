package checker;

import static jvm.Context.ATTRIBUTE;
import static jynx.Global.LOG;
import static jynx.Message.M508;

import jvm.Attribute;
import jvm.Context;
import jvm.JvmVersion;

public abstract class AbstractAttribute implements AttributeInstance {
    
    protected final Context context;
    protected final int level;
    protected final String name;
    protected final Buffer buffer;
    protected final int size;
    protected final Attribute attr;
    protected final ConstantPool pool;

    public AbstractAttribute(Attribute attr, Context context, String name, Buffer buffer) {
        this.attr = attr;
        this.context = context;
        this.level = level(context);
        this.name = name;
        this.buffer = buffer;
        this.pool = buffer.pool();
        this.size = buffer.limit() - buffer.position();
    }

    private static int level(Context context) {
        switch(context) {
                case COMPONENT:
                case CODE:
                    return 2;
                case CLASS:
                    return 0;
                default:
                    return 1;
        }
    }
    
    @Override
    public String toString() {
        return name;
    }

    @Override
    public int sizs() {
        return size;
    }

    @Override
    public Buffer buffer() {
        return buffer;
    }

    @Override
    public int level() {
        return level;
    }

    @Override
    public boolean isKnown() {
        return attr != null;
    }
    
    @Override
    public Attribute attribute() {
        return attr;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public void checkAtLimit() {
        if (buffer.hasRemaining()) {
            //"actual end(%#x) of %s does not match expected %#x"
            LOG(M508, buffer.position(), name, buffer.limit());
            buffer.advanceToLimit();
        }
    }

    @Override
    public String attrDesc(JvmVersion jvmversion) {
        String attrerror = "(unknown)";
        Attribute uattr = attribute();
        if (uattr != null) {
            attrerror = "";
            if (!jvmversion.supports(uattr)) {
                attrerror = String.format("(not supported in %s)",jvmversion);
            } else if (!uattr.inContext(context)) {
                attrerror = "(out of context)";
            }
        }

        return String.format("%s %s %s",ATTRIBUTE,this,attrerror );
    }
    
    @Override
    public void checkCPEntries(int codesz, int maxlocals) {
        throw new AssertionError();
    }
}

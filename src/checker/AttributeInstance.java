package checker;

import static jvm.Context.ATTRIBUTE;
import static jynx.Global.LOG;
import static jynx.Message.M508;

import jvm.Attribute;
import jvm.Context;
import jvm.JvmVersion;
import jvm.StandardAttribute;

public abstract class AttributeInstance {
    
    protected final int level;
    protected final String name;
    protected final AttributeBuffer buffer;
    protected final int size;
    protected final Attribute attr;
    protected final ConstantPool pool;

    public AttributeInstance(Attribute attr, String name, AttributeBuffer buffer) {
        this.attr = attr;
        this.level = level(buffer.context());
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

    public int sizs() {
        return size;
    }

    public AttributeBuffer buffer() {
        return buffer;
    }

    public int level() {
        return level;
    }

    public boolean isKnown() {
        return attr != null;
    }
    
    public Attribute attribute() {
        return attr;
    }

    public String name() {
        return name;
    }

    public void checkAtLimit() {
        if (buffer.hasRemaining()) {
            //"actual end(%#x) of %s does not match expected %#x"
            LOG(M508, buffer.position(), name, buffer.limit());
            buffer.advanceToLimit();
        }
    }

    public String attrDesc(JvmVersion jvmversion) {
        String attrerror = "(unknown)";
        Attribute uattr = attribute();
        if (uattr != null) {
            attrerror = "";
            if (!jvmversion.supports(uattr)) {
                attrerror = String.format("(not supported in %s)",jvmversion);
            } else if (!uattr.inContext(buffer.context())) {
                attrerror = "(out of context)";
            }
        }

        return String.format("%s %s %s",ATTRIBUTE,this,attrerror );
    }
    
    public void checkCPEntries(IndentPrinter ptr) {
        throw new AssertionError();
    }

    public static AttributeInstance getInstance(String attrname, AttributeBuffer attrbuff) {
        
       StandardAttribute uattr = StandardAttribute.getInstance(attrname);
        try {
            if (uattr == null) {
                return new UnknownAttribute(attrname, attrbuff);
            } else {
                switch(uattr.type()) {
                    case MODULE:
                        return new ModuleAttribute(uattr, attrbuff);
                }
                return new SimpleAttribute(uattr, attrbuff);
            }
        } catch (Exception ex) {
                 LOG(ex);
                 return null;
        }
    }
    
}

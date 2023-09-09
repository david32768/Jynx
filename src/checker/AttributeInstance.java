package checker;

import static jvm.Context.ATTRIBUTE;
import static jynx.Global.LOG;
import static jynx.Message.M508;

import jvm.Attribute;
import jvm.JvmVersion;
import jvm.StandardAttribute;

public abstract class AttributeInstance {
    
    protected final String name;
    protected final AttributeBuffer buffer;
    protected final int size;
    protected final Attribute attr;

    public AttributeInstance(Attribute attr, AttributeBuffer buffer) {
        this.attr = attr;
        this.name = buffer.name();
        this.buffer = buffer;
        this.size = buffer.limit() - buffer.position();
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

    public static AttributeInstance getInstance(AttributeBuffer attrbuff) {
        
       StandardAttribute uattr = StandardAttribute.getInstance(attrbuff.name());
        try {
            if (uattr == null) {
                return new UnknownAttribute(attrbuff);
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

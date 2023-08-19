package checker;

import static jynx.Global.LOG;

import jvm.Attribute;
import jvm.Context;
import jvm.JvmVersion;
import jvm.StandardAttribute;

public interface AttributeInstance {

    public int sizs();
    public Buffer buffer();
    public int level();
    public void checkAtLimit();
    public boolean isKnown();
    public Attribute attribute();
    public String attrDesc(JvmVersion jvmversion);
    public void checkCPEntries(int codesz, int maxlocals);
    public String name();

    public static AttributeInstance getInstance(Context context, String attrname, Buffer buffer) {
       StandardAttribute uattr = StandardAttribute.getInstance(attrname);
        try {
            int size = buffer.nextSize();
            Buffer attrbuff = buffer.duplicate();
            buffer.advance(size);
            attrbuff.limit(buffer.position());
            if (uattr == null) {
                return new UnknownAttribute(context, attrname, attrbuff);
            } else {
                switch(uattr.type()) {
                    case MODULE:
                        return new ModuleAttribute(context, uattr, attrbuff);
                }
                return new SimpleAttribute(context, uattr, attrbuff);
            }
        } catch (Exception ex) {
                 LOG(ex);
                 return null;
        }
    }
    
}

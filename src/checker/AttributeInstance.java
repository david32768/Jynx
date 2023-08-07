package checker;

import java.io.PrintWriter;
import static jynx.Global.LOG;

import jvm.Attribute;
import jvm.AttributeName;
import jvm.Context;
import jvm.JvmVersion;

public interface AttributeInstance {

    public int sizs();
    public Buffer buffer();
    public int level();
    public void checkAtLimit();
    public boolean isKnown();
    public Attribute attribute();
    public String attrDesc(Context context, JvmVersion jvmversion);
    public void checkCPEntries(PrintWriter pw, ConstantPool pool, int codesz, int maxlocals);
    public String name();

    public static AttributeInstance getInstance(int level, String attrname, Buffer buffer) {
       AttributeName uattr = AttributeName.getInstance(attrname);
        try {
            int size = buffer.nextSize();
            Buffer attrbuff = buffer.duplicate();
            buffer.advance(size);
            attrbuff.limit(buffer.position());
            if (uattr == null) {
                return new UnknownAttribute(level, attrname, attrbuff);
            } else {
                switch(uattr.type()) {
                    case BOOTSTRAP:
                        return new BootStrapAttribute(level, uattr, attrbuff);
                    case MODULE:
                        return new ModuleAttribute(level, uattr, attrbuff);
                }
                return new SimpleAttribute(level, uattr, attrbuff);
            }
        } catch (Exception ex) {
                 LOG(ex);
                 return null;
        }
    }
    
}

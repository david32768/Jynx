package checker;

import jvm.Context;

public class UnknownAttribute extends AbstractAttribute {
    
    public UnknownAttribute(Context context, String name, Buffer buffer) {
        super(null, context, name, buffer);
    }

}

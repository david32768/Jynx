package checker;

import jvm.ConstantPoolType;
import jvm.Context;
import jvm.StandardAttribute;

public class ModuleAttribute extends AbstractAttribute {

    public ModuleAttribute(Context context, StandardAttribute attr, Buffer buffer) {
        super(attr,context,attr.name(),buffer);
        assert attr == StandardAttribute.Module;
    }

    @Override
    public void checkCPEntries(int codesz, int maxlocals) {
        buffer.nextCPEntry(ConstantPoolType.CONSTANT_Module);
        buffer.nextUnsignedShort(); // flags
        buffer.nextOptCPEntry(ConstantPoolType.CONSTANT_Utf8); //version

        int ct = buffer.nextUnsignedShort(); // requires
        for (int i = 0; i < ct; ++i) {
            buffer.nextCPEntry(ConstantPoolType.CONSTANT_Module);
            buffer.nextUnsignedShort(); // flags
            buffer.nextOptCPEntry(ConstantPoolType.CONSTANT_Utf8); //version
        }
        
        ct = buffer.nextUnsignedShort(); // exports
        for (int i = 0; i < ct; ++i) {
            buffer.nextCPEntry(ConstantPoolType.CONSTANT_Package);
            buffer.nextUnsignedShort(); // flags
            int xct = buffer.nextUnsignedShort();
            for (int j = 0; j < xct;++j) {
                buffer.nextCPEntry(ConstantPoolType.CONSTANT_Module);
            }
        }
        
        ct = buffer.nextUnsignedShort(); // opens
        for (int i = 0; i < ct; ++i) {
            buffer.nextCPEntry(ConstantPoolType.CONSTANT_Package);
            buffer.nextUnsignedShort();
            int xct = buffer.nextUnsignedShort();
            for (int j = 0; j < xct;++j) {
                buffer.nextCPEntry(ConstantPoolType.CONSTANT_Module);
            }
        }
        
        ct = buffer.nextUnsignedShort(); // uses
        for (int i = 0; i < ct; ++i) {
            buffer.nextCPEntry(ConstantPoolType.CONSTANT_Class);
        }
        
        ct = buffer.nextUnsignedShort(); // provides
        for (int i = 0; i < ct; ++i) {
            buffer.nextCPEntry(ConstantPoolType.CONSTANT_Class);
            int xct = buffer.nextUnsignedShort();
            for (int j = 0; j < xct;++j) {
                buffer.nextCPEntry(ConstantPoolType.CONSTANT_Class);
            }
        }
        
    }

}

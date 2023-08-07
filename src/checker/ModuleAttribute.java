package checker;

import java.io.PrintWriter;
import jvm.AttributeName;
import jvm.ConstantPoolType;

public class ModuleAttribute extends AbstractAttribute {

    public ModuleAttribute(int level, AttributeName attr, Buffer buffer) {
        super(attr,level,attr.name(),buffer);
        assert attr == AttributeName.Module;
    }

    @Override
    public void checkCPEntries(PrintWriter pw, ConstantPool pool, int codesz, int maxlocals) {
        buffer.nextCPEntry(ConstantPoolType.CONSTANT_Module);
        buffer.nextUnsignedShort(); // flags
        buffer.nextOptCPEntry(ConstantPoolType.CONSTANT_Utf8);

        int ct = buffer.nextUnsignedShort(); // requires
        for (int i = 0; i < ct; ++i) {
            buffer.nextCPEntry(ConstantPoolType.CONSTANT_Module);
            buffer.nextUnsignedShort(); // flags
            buffer.nextOptCPEntry(ConstantPoolType.CONSTANT_Utf8);
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

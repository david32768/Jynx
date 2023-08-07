package checker;

import java.io.PrintWriter;
import static jynx.Global.JVM_VERSION;
import static jynx.Global.LOG;
import static jynx.Global.OPTION;
import static jynx.Message.M505;
import static jynx.Message.M507;

import jvm.AttributeName;
import jvm.ConstantPoolType;
import jvm.JvmVersion;
import jynx.GlobalOption;
import jynx.LogIllegalStateException;

public class BootStrapAttribute extends AbstractAttribute {

    public BootStrapAttribute(int level, AttributeName attr, Buffer buffer) {
        super(attr,level,attr.name(),buffer);
        assert attr == AttributeName.BootstrapMethods;
    }

    @Override
    public void checkCPEntries(PrintWriter pw, ConstantPool pool, int codesz, int maxlocals) {
        int ct = buffer.nextUnsignedShort();
        if (ct < pool.getMaxboot()) {
            // "maximun bootcp used by constatnt pool (%d)  is greater than supplied in attribute (%d)"
            throw new LogIllegalStateException(M505,pool.getMaxboot(), ct);
        }
        for (int j = 0; j < ct;++j) {
            CPEntry methodcp = buffer.nextCPEntry(ConstantPoolType.CONSTANT_MethodHandle);
            JvmVersion jvmversion = JVM_VERSION();
            int argct = buffer.nextUnsignedShort();
            CPEntry[] args = new CPEntry[argct];
            for (int k = 0; k < argct; ++k) {
                CPEntry argcp = buffer.nextCPEntry();
                args[k] = argcp;
                ConstantPoolType cptk = argcp.getType();
                if (!cptk.isLoadableBy(jvmversion)) {
                    // "boot argument %s is not loadable by %s"
                    LOG(M507, cptk, jvmversion);
                }
            }
            if (OPTION(GlobalOption.CP)) {
                pw.format("%s%d %s%n",Structure.spacer(level + 1),j,pool.stringValue(methodcp));
                for (int i = 0; i < args.length; ++i) {
                    pw.format("%s%d %s%n",Structure.spacer(level + 2),i,pool.stringValue(args[i]));
                }
            }
        }
    }

}

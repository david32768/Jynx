package checker;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static jvm.Context.*;
import static jynx.Global.*;
import static jynx.Message.M500;
import static jynx.Message.M502;
import static jynx.Message.M503;
import static jynx.Message.M504;
import static jynx.Message.M517;


import asm.JynxClassReader;
import jvm.Attribute;
import jvm.AttributeName;
import jvm.AttributeType;
import jvm.ConstantPoolType;
import jvm.Context;
import jvm.JvmVersion;
import jynx.GlobalOption;
import jynx.LogIllegalArgumentException;

public class Structure {

    private final PrintWriter pw;
    private final ConstantPool pool;
    private final Context topContext;
    private final String classname;
    private final JvmVersion jvmVersion;

    private int codesz;
    private int maxLocals;
    private boolean hasCode;
    private boolean hasRecord;
    
    private Structure(PrintWriter pw, Buffer buffer, Context topContext, String klass, JvmVersion jvmversion) {
        this.pw = pw;
        this.pool = buffer.pool();
        this.topContext = topContext;
        this.classname = klass;
        this.jvmVersion = jvmversion;
        this.hasCode = false;
        this.hasRecord = false;
    }

    private static final int MAGIC = 0xcafebabe;
    
    public static void checkInstance(PrintWriter pw, String klass) throws IOException {
        ByteBuffer bb = ByteBuffer.wrap(JynxClassReader.getClassBytes(klass));
        bb = bb.asReadOnlyBuffer();
        bb.order(ByteOrder.BIG_ENDIAN);
        int qmagic = bb.getInt();
        if (qmagic != MAGIC) {
            // "magic number is %#x; should be %#x"
            throw new LogIllegalArgumentException(M500,qmagic,MAGIC);
        }
        JvmVersion jvmversion = JvmVersion.fromASM(bb.getInt());
        setJvmVersion(jvmversion);
        ConstantPool pool = ConstantPool.getInstance(bb,jvmversion);
        pool.check();
        if (OPTION(GlobalOption.CP)) {
            pool.print(pw);
        }
        Buffer buffer = new Buffer(pool,bb);
        int access = buffer.nextUnsignedShort();
        Context topContext = (access & org.objectweb.asm.Opcodes.ACC_MODULE) == 0?CLASS:MODULE;
        CPEntry classcp = buffer.nextCPEntry(ConstantPoolType.CONSTANT_Class);
        int[] value = (int[])classcp.getValue();
        assert value.length == 1;
        int x = value[0];
        String klassname = (String)pool.getValue(x);
        Structure struct =  new Structure(pw, buffer, topContext, klassname, jvmversion);
        struct.checkClass(buffer);
    }

    public void checkClass(Buffer buffer) {
        try {
            pw.format("%s %s %s%n", topContext, classname,jvmVersion);
            buffer.nextShort(); // super
            int ct = buffer.nextUnsignedShort();
            buffer.advance(2*ct);
            check_fields(buffer);
            check_methods(buffer);
            check_attrs(topContext,0,buffer);
            if (buffer.hasRemaining()) {
                // "%s %s has %d extra bytes at end"
                LOG(M502, topContext, classname, buffer.remaining());
                buffer.advanceToLimit();
            }
        } catch (ArithmeticException ex) {
            LOG(ex);
        }
    }
    
    private void check_fields(Buffer buffer) {
        Context context = FIELD;
        int ct = buffer.nextUnsignedShort();
        for (int i = 0; i < ct; ++i) {
            buffer.nextShort();
            String name = (String)buffer.nextPoolValue();
            String type = (String)buffer.nextPoolValue();
            pw.format("%s %s %s%n", context, name,type);
            check_attrs(context,1,buffer);
        }
    }

    private void check_methods(Buffer buffer) {
        Context context = METHOD;
        int ct = buffer.nextUnsignedShort();
        for (int i = 0; i < ct; ++i) {
            hasCode = false;
            buffer.nextShort();
            String name = (String)buffer.nextPoolValue();
            String type = (String)buffer.nextPoolValue();
            pw.format("%s %s%s%n", context, name,type);
            check_attrs(context,1,buffer);
        }
    }

    public static String spacer(int level) {
        char[] spacearr = new char[2*level];
        Arrays.fill(spacearr,' ');
        return String.valueOf(spacearr);
    }

    private void checkAttributeStructure(AttributeInstance attrx) {
        Attribute attr = attrx.attribute();
        Buffer attrbuff = attrx.buffer();
        int level = attrx.level();
        AttributeType attrtype = attr.type();
        switch (attrtype) {
            case FIXED:
            case ARRAY1:
            case ARRAY:
            case BOOTSTRAP:
            case MODULE:
                attrx.checkCPEntries(pw, pool, codesz, maxLocals);
                break;
            case CODE:
                checkCode(attrbuff, level);
                break;
            case RECORD:
                checkRecord(attrbuff, level);
                break;
            default:
                throw new EnumConstantNotPresentException(attrtype.getClass(), attrtype.name());
        }
    }
    
    private void check_attrs(Context context,int level, Buffer buffex) {
        String spacer = spacer(level);
        Set<Attribute> attrset = new HashSet<>();
        int attrs_ct = buffex.nextUnsignedShort();
        for (int i = 0; i < attrs_ct; ++i) {
            int start_offset = buffex.position();
            String attrnamestr = (String)buffex.nextPoolValue();
            AttributeInstance attr = AttributeInstance.getInstance(level, attrnamestr, buffex);
            String attrdesc = attr.attrDesc(context, jvmVersion);
            pw.format("%s%s start = %#x length = %#x%n",
                    spacer, attrdesc, start_offset, attr.sizs());
            if (!attr.isKnown()) {
                continue;
            }
            Attribute attribute = attr.attribute();
            boolean added = attrset.add(attribute);
            if (!added && attribute.isUnique()) {
                // "duplicate attribute %s in contexr %s"
                LOG(M517,attr,context);
            }
            checkAttributeStructure(attr);
            attr.checkAtLimit();
        }
    }

    private void checkCode(Buffer attrbuff, int level) {
        if (hasCode) {
            // "multiple %s attributes in %s"
            LOG(M504, AttributeName.Code, Context.METHOD);
        }
        hasCode = true;
        attrbuff.nextShort(); // max stack
        maxLocals = attrbuff.nextShort(); //max locals
        int sz = attrbuff.nextSize(); // code length
        codesz = sz;
        attrbuff.advance(sz);
        int ct = attrbuff.nextUnsignedShort();
        for (int i = 0; i < ct; ++i) {
            int startpc = attrbuff.nextLabel(codesz);
            int endpc = attrbuff.nextLabel(codesz);
            if (endpc < startpc) {
                // "startpc (%d) > endpc (%d)"
                LOG(M503, startpc, endpc);
            }
            int handlerpc = attrbuff.nextLabel(codesz);
            CPEntry entry = attrbuff.nextOptCPEntry(ConstantPoolType.CONSTANT_Class);
        }
        check_attrs(CODE,level+1,attrbuff);
        codesz = 0;
        maxLocals = 0;
    }

    private void checkRecord(Buffer attrbuff, int level) {
        if (hasRecord) {
            // "multiple %s attributes in %s"
            LOG(M504, AttributeName.Record, Context.CLASS);
        }
        hasRecord = true;
        int ct = attrbuff.nextUnsignedShort();
        for (int j = 0; j < ct;++j) {
            String attrname = (String)attrbuff.nextPoolValue();
            String attrdesc = (String)attrbuff.nextPoolValue();
            pw.format("%s%s %s %s%n", spacer(level + 1),COMPONENT,attrname,attrdesc);
            check_attrs(COMPONENT,level + 2,attrbuff);
        }
    }

    public static boolean PrintClassStructure(String klass, PrintWriter pw) {
        try {
            pw.println("START " + klass);
            Structure.checkInstance(pw, klass);
            pw.println("END " + klass);
        } catch(IOException ioex) {
            LOG(ioex);
            return false;
        }
        pw.flush();
        return true;
    }

}

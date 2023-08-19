package checker;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashSet;
import java.util.Optional;
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
import jvm.AttributeType;
import jvm.ConstantPoolType;
import jvm.Context;
import jvm.JvmVersion;
import jvm.StandardAttribute;
import jynx.GlobalOption;
import jynx.LogIllegalArgumentException;

public class Structure {

    private final ConstantPool pool;
    private final String classname;
    private final JvmVersion jvmVersion;

    private int codesz;
    private int maxLocals;
    private boolean hasCode;
    private boolean hasRecord;
    
    private Structure(Buffer buffer, String klass, JvmVersion jvmversion) {
        this.pool = buffer.pool();
        this.classname = klass;
        this.jvmVersion = jvmversion;
        this.hasCode = false;
        this.hasRecord = false;
    }

    private static final int MAGIC = 0xcafebabe;
    
    public static void checkInstance(PrintWriter pw, String klass) throws IOException {
        IndentPrinter ptr = new IndentPrinter(pw);
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
        ptr.println("VERSION %s",jvmversion);
        int poolstart = bb.position();
        ConstantPool pool = ConstantPool.getInstance(bb,jvmversion);
        pool.check();
        int poolend = bb.position();
        ptr.println("CONSTANT POOL length = %#x entries = [1,%d]",poolend - poolstart,pool.last());
        Buffer buffer = new Buffer(pool,bb);
        int access = buffer.nextUnsignedShort();
        CPEntry classcp = buffer.nextCPEntry(ConstantPoolType.CONSTANT_Class);
        int[] value = (int[])classcp.getValue();
        assert value.length == 1;
        int x = value[0];
        String klassname = (String)pool.getValue(x);
        Structure struct =  new Structure(buffer, klassname, jvmversion);
        struct.checkClass(ptr,buffer);
        pool.checkBootstraps();
        if (OPTION(GlobalOption.CP)) {
            pool.printCP(ptr);
            pool.printBoot(ptr);
        }
    }

    public void checkClass(IndentPrinter ptr, Buffer buffer) {
        try {
            ptr.println("CLASS %s", classname);
            buffer.nextShort(); // super
            int ct = buffer.nextUnsignedShort();
            buffer.advance(2*ct);
            check_fields(ptr,buffer);
            check_methods(ptr,buffer);
            check_attrs(CLASS,ptr,buffer);
            if (buffer.hasRemaining()) {
                // "%s %s has %d extra bytes at end"
                LOG(M502, CLASS, classname, buffer.remaining());
                buffer.advanceToLimit();
            }
        } catch (ArithmeticException ex) {
            LOG(ex);
        }
    }
    
    private void check_fields(IndentPrinter ptr,Buffer buffer) {
        Context context = FIELD;
        int ct = buffer.nextUnsignedShort();
        for (int i = 0; i < ct; ++i) {
            buffer.nextShort();
            String name = (String)buffer.nextPoolValue();
            String type = (String)buffer.nextPoolValue();
            ptr.println("%s %s %s", context, name,type);
            check_attrs(context,ptr.shift(),buffer);
        }
    }

    private void check_methods(IndentPrinter ptr,Buffer buffer) {
        Context context = METHOD;
        int ct = buffer.nextUnsignedShort();
        for (int i = 0; i < ct; ++i) {
            hasCode = false;
            buffer.nextShort();
            String name = (String)buffer.nextPoolValue();
            String type = (String)buffer.nextPoolValue();
            ptr.println("%s %s%s", context, name,type);
            check_attrs(context,ptr.shift(),buffer);
        }
    }

    private void checkAttributeStructure(IndentPrinter ptr,AttributeInstance attrx) {
        Attribute attr = attrx.attribute();
        Buffer attrbuff = attrx.buffer();
        int level = attrx.level();
        AttributeType attrtype = attr.type();
        switch (attrtype) {
            case FIXED:
            case ARRAY1:
            case ARRAY:
            case MODULE:
                attrx.checkCPEntries(codesz, maxLocals);
                break;
            case CODE:
                checkCode(ptr, attrbuff, level);
                break;
            case RECORD:
                checkRecord(ptr,attrbuff, level);
                break;
            default:
                throw new EnumConstantNotPresentException(attrtype.getClass(), attrtype.name());
        }
    }
    
    private void check_attrs(Context context, IndentPrinter ptr, Buffer buffex) {
        Set<Attribute> attrset = new HashSet<>();
        int attrs_ct = buffex.nextUnsignedShort();
        for (int i = 0; i < attrs_ct; ++i) {
            int start_offset = buffex.position();
            String attrnamestr = (String)buffex.nextPoolValue();
            AttributeInstance attr = AttributeInstance.getInstance(context, attrnamestr, buffex);
            String attrdesc = attr.attrDesc(jvmVersion);
            ptr.println("%s start = %#x length = %#x",
                    attrdesc, start_offset, attr.sizs());
            if (!attr.isKnown()) {
                continue;
            }
            Attribute attribute = attr.attribute();
            boolean added = attrset.add(attribute);
            if (!added && attribute.isUnique()) {
                // "duplicate attribute %s in contexr %s"
                LOG(M517,attr,context);
            }
            checkAttributeStructure(ptr.shift(),attr);
            attr.checkAtLimit();
        }
    }

    private void checkCode(IndentPrinter ptr, Buffer attrbuff, int level) {
        if (hasCode) {
            // "multiple %s attributes in %s"
            LOG(M504, StandardAttribute.Code, Context.METHOD);
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
            Optional<CPEntry> optentry = attrbuff.nextOptCPEntry(ConstantPoolType.CONSTANT_Class);
        }
        check_attrs(CODE,ptr,attrbuff);
        codesz = 0;
        maxLocals = 0;
    }

    private void checkRecord(IndentPrinter ptr, Buffer attrbuff, int level) {
        if (hasRecord) {
            // "multiple %s attributes in %s"
            LOG(M504, StandardAttribute.Record, Context.CLASS);
        }
        hasRecord = true;
        int ct = attrbuff.nextUnsignedShort();
        for (int j = 0; j < ct;++j) {
            String attrname = (String)attrbuff.nextPoolValue();
            String attrdesc = (String)attrbuff.nextPoolValue();
            ptr.println("%s %s %s",COMPONENT,attrname,attrdesc);
            check_attrs(COMPONENT,ptr.shift(),attrbuff);
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

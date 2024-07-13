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
import static jynx.Message.M517;


import asm.JynxClassReader;
import jvm.AccessFlag;
import jvm.Attribute;
import jvm.AttributeType;
import jvm.ConstantPoolType;
import jvm.Context;
import jvm.JvmVersion;
import jynx.ClassUtil;
import jynx.GlobalOption;
import jynx.LogIllegalArgumentException;

public class Structure {

    private final String classname;
    private final JvmVersion jvmVersion;

    private Structure(String klass, JvmVersion jvmversion) {
        this.classname = klass;
        this.jvmVersion = jvmversion;
    }

    private static final int MAGIC = 0xcafebabe;
    
    public static void checkInstance(PrintWriter pw, String klass) throws IOException {
        IndentPrinter ptr = new IndentPrinter(pw);
        ByteBuffer bb = ByteBuffer.wrap(ClassUtil.getClassBytes(klass));
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
        if (OPTION(GlobalOption.DETAIL)) {
            pool.printCP(ptr,false);
        }
        Buffer buffer = new Buffer(pool,bb);
        int access = buffer.nextUnsignedShort();
        CPEntry classcp = buffer.nextCPEntry(ConstantPoolType.CONSTANT_Class);
        int[] value = (int[])classcp.getValue();
        assert value.length == 1;
        int x = value[0];
        String klassname = (String)pool.getValue(x);
        Structure struct =  new Structure(klassname, jvmversion);
        struct.checkClass(ptr,buffer,access);
        boolean bootok = pool.checkBootstraps();
        if (!bootok) {
            pool.printCP(ptr,true);
        }
    }
    
    private String accessString(Context context, int access) {
        return AccessFlag.getEnumSet(access, context, jvmVersion).toString();
    }

    private void checkClass(IndentPrinter ptr, Buffer buffer, int access) {
        try {
            ptr.println("CLASS %s %s", classname, accessString(CLASS, access));
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
            int access = buffer.nextShort();
            String name = (String)buffer.nextPoolValue();
            String type = (String)buffer.nextPoolValue();
            ptr.println("%s %s %s %s", context, name, type, accessString(FIELD, access));
            check_attrs(context,ptr.shift(),buffer);
        }
    }

    private void check_methods(IndentPrinter ptr,Buffer buffer) {
        Context context = METHOD;
        int ct = buffer.nextUnsignedShort();
        for (int i = 0; i < ct; ++i) {
            int access = buffer.nextShort();
            String name = (String)buffer.nextPoolValue();
            String type = (String)buffer.nextPoolValue();
            ptr.println("%s %s%s %s", context, name, type, accessString(METHOD, access));
            check_attrs(context,ptr.shift(),buffer);
        }
    }

    private void check_attrs(Context context, IndentPrinter ptr, Buffer buffer) {

        Set<Attribute> attrset = new HashSet<>();
        int attrs_ct = buffer.nextUnsignedShort();
        for (int i = 0; i < attrs_ct; ++i) {
            int start_offset = buffer.position();
            String attrnamestr = (String)buffer.nextPoolValue();
            int size = buffer.nextSize();
            AttributeBuffer attrbuff = buffer.attributeBuffer(context, attrnamestr, size);
            AttributeInstance attr = AttributeInstance.getInstance(attrbuff);
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

    private void checkAttributeStructure(IndentPrinter ptr,AttributeInstance attrx) {
        Attribute attr = attrx.attribute();
        AttributeBuffer attrbuff = attrx.buffer();
        AttributeType attrtype = attr.type();
        switch (attrtype) {
            case FIXED:
            case ARRAY1:
            case ARRAY:
            case MODULE:
                attrx.checkCPEntries(ptr);
                break;
            case CODE:
                checkCode(ptr, attrbuff);
                break;
            case RECORD:
                checkRecord(ptr,attrbuff);
                break;
            default:
                throw new EnumConstantNotPresentException(attrtype.getClass(), attrtype.name());
        }
    }

    private void checkCode(IndentPrinter ptr, AttributeBuffer attrbuff) {
        CodeBuffer codebuff = attrbuff.codeBuffer(ptr);
        int ct = codebuff.nextUnsignedShort();
        for (int i = 0; i < ct; ++i) {
            int startpc = codebuff.nextLabel();
            int endpc = codebuff.nextLabel();
            if (endpc < startpc) {
                // "startpc (%d) > endpc (%d)"
                LOG(M503, startpc, endpc);
            }
            int handlerpc = codebuff.nextLabel();
            Optional<CPEntry> optentry = codebuff.nextOptCPEntry(ConstantPoolType.CONSTANT_Class);
        }
        check_attrs(CODE, ptr, codebuff);
    }

    private void checkRecord(IndentPrinter ptr, AttributeBuffer attrbuff) {
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

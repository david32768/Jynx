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
import static jynx.Message.M516;
import static jynx.Message.M517;


import asm.JynxClassReader;
import jvm.Attribute;
import jvm.AttributeType;
import jvm.ConstantPoolType;
import jvm.Context;
import jvm.JvmVersion;
import jvm.NumType;
import jvm.OpArg;
import jvm.OpPart;
import jynx.GlobalOption;
import jynx.LogIllegalArgumentException;
import jynx2asm.ops.JvmOp;

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
        struct.checkClass(ptr,buffer);
        boolean bootok = pool.checkBootstraps();
        if (!bootok) {
            pool.printCP(ptr,true);
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
            buffer.nextShort();
            String name = (String)buffer.nextPoolValue();
            String type = (String)buffer.nextPoolValue();
            ptr.println("%s %s%s", context, name,type);
            check_attrs(context,ptr.shift(),buffer);
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

    private void check_attrs(Context context, IndentPrinter ptr, Buffer buffer) {

        Set<Attribute> attrset = new HashSet<>();
        int attrs_ct = buffer.nextUnsignedShort();
        for (int i = 0; i < attrs_ct; ++i) {
            int start_offset = buffer.position();
            String attrnamestr = (String)buffer.nextPoolValue();
            int size = buffer.nextSize();
            AttributeBuffer attrbuff = buffer.extract(size).attributeBuffer(context);
            AttributeInstance attr = AttributeInstance.getInstance(attrnamestr, attrbuff);
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

    private void checkCode(IndentPrinter ptr, AttributeBuffer attrbuff) {
        attrbuff.nextShort(); // max stack
        int maxlocals = attrbuff.nextShort(); //max locals
        int codesz = attrbuff.nextSize(); // code length
        CodeBuffer codebuff = attrbuff.codeBuffer(maxlocals, codesz);
        CodeBuffer instbuff = codebuff.extract(codesz);
        checkInsn(ptr.shift(), instbuff);
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

    private void checkInsn(IndentPrinter ptr, CodeBuffer instbuff) {
        int start = instbuff.position();
        while(instbuff.hasRemaining()) {
            int instoff = instbuff.position() - start;
            instbuff.setPosLabel(instoff);
            int opcode = instbuff.nextUnsignedByte();
            int read = 1;
            JvmOp jop = JvmOp.getOp(opcode);
            if (jop == JvmOp.opc_wide) {
                opcode = instbuff.nextUnsignedByte();
                jop = JvmOp.getOp(opcode);
                jop = jop.widePrepended();
                read = 2;
            }
            OpArg arg = jop.args();
            boolean print = OPTION(GlobalOption.DETAIL);
            switch(arg) {
                case arg_lookupswitch:
                    instbuff.align4(instoff + 1);
                    int deflab = instbuff.nextBranchLabel(instoff);
                    if (print) {
                        ptr.println("%5d:  %s default @%d .array", instoff, jop, deflab);
                    }
                    int n = instbuff.nextSize();
                    for (int i = 0; i < n; ++i) {
                        int value = instbuff.nextInt();
                        int brlab = instbuff.nextBranchLabel(instoff);
                        if (print) {
                            ptr.println("             %d -> @%d", value, brlab);
                        }
                    }
                    if (print) {
                        ptr.println("        .end_array");
                    }
                    break;
                case arg_tableswitch:
                    instbuff.align4(instoff + 1);
                    deflab = instbuff.nextBranchLabel(instoff);
                    if (print) {
                        ptr.println("%5d:  %s default @%d .array", instoff, jop, deflab);
                    }
                    int low = instbuff.nextInt();
                    int high = instbuff.nextInt();
                    if (low > high) {
                        // "low %d must be less than or equal to high %d"
                        LOG(M516,low,high);
                    }
                    for (int i = low; i <= high; ++i) {
                        int brlab = instbuff.nextBranchLabel(instoff);
                        if (print) {
                            ptr.println("             %d -> @%d", i, brlab);
                        }
                    }
                    if (print) {
                        ptr.println("        .end_array");
                    }
                    break;
                default:
                    String extra = extra(instbuff, jop, instoff);
                    if (print) {
                        ptr.println("%5d:  %s%s", instoff, jop, extra);
                    }
                    assert start + instoff + jop.length() == instbuff.position();
                    break;
            }
        }
        instbuff.checkLabels();
    }
    
    public String extra(CodeBuffer instbuff, JvmOp jop, int instoff) {
        OpArg arg = jop.args();
        String result = "";
        for (OpPart fmt:arg.getParts()) {
            String extra;
            switch(fmt) {
                case CP:
                    CPEntry cp;
                    if (jop == JvmOp.asm_ldc) {
                        cp = instbuff.nextCPEntryByte();
                    } else {
                        cp = instbuff.nextCPEntry();
                    }
                    extra = instbuff.stringValue(cp);
                    arg.checkCPType(cp.getType());
                    break;
                case LABEL:
                    int jmplab = jop.isWideForm()?
                            instbuff.nextBranchLabel(instoff):
                            instbuff.nextIfLabel(instoff);
                    extra = "@" + Integer.toString(jmplab);
                    break;
                case VAR:
                    int var;
                    if (jop.isImmediate()) {
                        var = jop.numericSuffix();
                    } else if (jop.isWideForm()) {
                        var = instbuff.nextUnsignedShort();
                    } else {
                        var = instbuff.nextUnsignedByte();
                    }
                    instbuff.checkLocalVar(var);
                    extra = jop.isImmediate()? "": Integer.toString(var);
                    break;
                case INCR:
                    int incr;
                    if (jop.isWideForm()) {
                        incr = instbuff.nextShort();
                    } else {
                        incr = instbuff.nextByte();
                    }
                    extra = Integer.toString(incr);
                    break;
                case BYTE:
                    int b = instbuff.nextByte();
                    extra = Integer.toString(b); 
                    break;
                case SHORT:
                    int s = instbuff.nextShort();
                    extra = Integer.toString(s);
                    break;
                case TYPE:
                    int t = instbuff.nextUnsignedByte();
                    extra = NumType.getInstance(t).externalName();
                    break;
                case UBYTE:
                    int u = instbuff.nextUnsignedByte();
                    extra = Integer.toString(u);
                    break;
                case ZERO:
                    int z = instbuff.nextByte();
                    extra = "";
                    break;
                default:
                    throw new EnumConstantNotPresentException(fmt.getClass(), fmt.name());
            }
            if (!extra.isEmpty()) {
                result += " " + extra;
            }
        }
        return result;
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

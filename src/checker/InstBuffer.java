package checker;

import java.nio.ByteBuffer;

import static jynx.Global.LOG;
import static jynx.Global.OPTION;
import static jynx.Message.M516;

import jvm.ConstantPoolType;
import jvm.NumType;
import jvm.OpArg;
import jvm.OpPart;
import jvm.StandardAttribute;
import jynx.GlobalOption;
import jynx.StringUtil;
import jynx2asm.ops.JvmOp;

public class InstBuffer extends AbstractCodeBuffer {
    
    public InstBuffer(ConstantPool pool, ByteBuffer bb, int maxlocal, int codesz) {
        super(pool, bb, maxlocal, codesz);
    }

    public CodeBuffer codeBuffer(ByteBuffer codebb) {
        labels.checkLabels();
        return new CodeBuffer(pool, codebb, StandardAttribute.Code.toString(), maxlocal, labels);
    }
    
    
    public int nextBranchLabel(int instoff) {
        return labels.labelOffset(instoff, nextInt());
    }
    
    public int nextIfLabel(int instoff) {
        return labels.labelOffset(instoff, nextShort());
    }
    
    public void setPosLabel(int instoff) {
        labels.setPosLabel(instoff);
    }
   
    public void align4(int offset) {
        int align = offset & 0x3;
        int padding = align == 0?0:4 - align;
        advance(padding );       
    }
    
    public void checkInsn(IndentPrinter ptr) {
        int start = position();
        while(hasRemaining()) {
            int instoff = position() - start;
            setPosLabel(instoff);
            int opcode = nextUnsignedByte();
            JvmOp jop = JvmOp.getOp(opcode);
            if (jop == JvmOp.opc_wide) {
                opcode = nextUnsignedByte();
                jop = JvmOp.getOp(opcode);
                jop = jop.widePrepended();
            }
            OpArg arg = jop.args();
            boolean print = OPTION(GlobalOption.DETAIL);
            switch(arg) {
                case arg_lookupswitch:
                    align4(instoff + 1);
                    int deflab = nextBranchLabel(instoff);
                    if (print) {
                        ptr.println("%5d:  %s default @%d .array", instoff, jop, deflab);
                    }
                    int n = nextSize();
                    for (int i = 0; i < n; ++i) {
                        int value = nextInt();
                        int brlab = nextBranchLabel(instoff);
                        if (print) {
                            ptr.println("             %d -> @%d", value, brlab);
                        }
                    }
                    if (print) {
                        ptr.println("        .end_array");
                    }
                    break;
                case arg_tableswitch:
                    align4(instoff + 1);
                    deflab = nextBranchLabel(instoff);
                    if (print) {
                        ptr.println("%5d:  %s default @%d .array", instoff, jop, deflab);
                    }
                    int low = nextInt();
                    int high = nextInt();
                    if (low > high) {
                        // "low %d must be less than or equal to high %d"
                        LOG(M516,low,high);
                    }
                    for (int i = low; i <= high; ++i) {
                        int brlab = nextBranchLabel(instoff);
                        if (print) {
                            ptr.println("             %d -> @%d", i, brlab);
                        }
                    }
                    if (print) {
                        ptr.println("        .end_array");
                    }
                    break;
                default:
                    String extra = extra(jop, instoff);
                    if (print) {
                        ptr.println("%5d:  %s%s", instoff, jop, extra);
                    }
                    assert start + instoff + jop.length() == position();
                    break;
            }
        }
    }
    
    public String extra(JvmOp jop, int instoff) {
        OpArg arg = jop.args();
        String result = "";
        for (OpPart fmt:arg.getParts()) {
            String extra;
            switch(fmt) {
                case CP:
                    CPEntry cp;
                    if (jop == JvmOp.asm_ldc) {
                        cp = nextCPEntryByte();
                    } else {
                        cp = nextCPEntry();
                    }
                    extra = stringValue(cp);
                    ConstantPoolType cpt = cp.getType();
                    if (cpt == ConstantPoolType.CONSTANT_String) {
                        extra = StringUtil.QuoteEscape(extra);
                    }
                    arg.checkCPType(cpt);
                    break;
                case LABEL:
                    int jmplab = jop.isWideForm()?
                            nextBranchLabel(instoff):
                            nextIfLabel(instoff);
                    extra = "@" + Integer.toString(jmplab);
                    break;
                case VAR:
                    int var;
                    if (jop.isImmediate()) {
                        var = jop.numericSuffix();
                    } else if (jop.isWideForm()) {
                        var = nextUnsignedShort();
                    } else {
                        var = nextUnsignedByte();
                    }
                    checkLocalVar(var);
                    extra = jop.isImmediate()? "": Integer.toString(var);
                    break;
                case INCR:
                    int incr;
                    if (jop.isWideForm()) {
                        incr = nextShort();
                    } else {
                        incr = nextByte();
                    }
                    extra = Integer.toString(incr);
                    break;
                case BYTE:
                    int b = nextByte();
                    extra = Integer.toString(b); 
                    break;
                case SHORT:
                    int s = nextShort();
                    extra = Integer.toString(s);
                    break;
                case TYPE:
                    int t = nextUnsignedByte();
                    extra = NumType.getInstance(t).externalName();
                    break;
                case UBYTE:
                    int u = nextUnsignedByte();
                    extra = Integer.toString(u);
                    break;
                case ZERO:
                    int z = nextByte();
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
    
}

package asm2jynx;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import static jynx.Global.*;
import static jynx.Message.*;

import jvm.ConstType;
import jvm.Feature;
import jvm.JvmVersion;
import jvm.NumType;
import jvm.OpArg;
import jvm.StandardAttribute;
import jynx.Directive;
import jynx.LogAssertionError;
import jynx.ReservedWord;
import jynx2asm.handles.HandlePart;
import jynx2asm.handles.MethodHandle;
import jynx2asm.Line;
import jynx2asm.NameDesc;
import jynx2asm.ops.JvmOp;

public class Insn2Jynx {
    
    private final JvmVersion jvmVersion;
    private final Object2String o2s;
    private final JynxStringBuilder lb;

    private final Map<Label, String> labelMap;
    private Object[] lastLocalStack;
    private JvmOp asmop;
    
    public Insn2Jynx(JynxStringBuilder lb, JvmVersion jvmVersion, Object2String o2s, Object[] initstack) {
        this.o2s = o2s;
        this.jvmVersion = jvmVersion;
        this.lb = lb;
        lb.setLabelNamer(this::getLabelName);
        this.labelMap = new HashMap<>();
        this.lastLocalStack = initstack;
    }

    public void printInsn(AbstractInsnNode in) {
        int asmcode = in.getOpcode();
        asmop = asmcode >= 0?JvmOp.getInstance(asmcode,jvmVersion):null;
        OpArg arg = asmop == null?OpArg.arg_dir:asmop.args();
        switchArg(arg,in);
    }

    private void switchArg(OpArg oparg, AbstractInsnNode in) {
        switch(oparg) {
            case arg_atype:arg_atype(in);break;
            case arg_byte:arg_byte(in);break;
            case arg_callsite:arg_callsite(in);break;
            case arg_class:arg_class(in);break;
            case arg_constant:arg_constant(in);break;
            case arg_dir:arg_dir(in);break;
            case arg_field:arg_field(in);break;
            case arg_incr:arg_incr(in);break;
            case arg_label:arg_label(in);break;
            case arg_lookupswitch:arg_lookupswitch(in);break;
            case arg_marray:arg_marray(in);break;
            case arg_method:case arg_interface:arg_method(in);break;
            case arg_none:arg_none(in);break;
            case arg_short:arg_short(in);break;
            case arg_stack:arg_none(in);break;
            case arg_tableswitch:arg_tableswitch(in);break;
            case arg_var:arg_var(in);break;
            default:
                throw new EnumConstantNotPresentException(oparg.getClass(), oparg.name());
        }
    }
    
    public String getLabelName(LabelNode ln) {
        Label label = ln.getLabel();
        return  getLabelName(label);
    }

    private String getLabelName(Label label) {
        String labref = labelMap.get(label);
        if (labref != null) {
            return labref;
        } else {
            labref = String.format("%cL%d", NameDesc.GENERATED_LABEL_MARKER, labelMap.size());
            labelMap.put(label,labref);
            return labref;
        }
    }
    
    
    private void arg_atype(AbstractInsnNode in) {
        IntInsnNode iin = (IntInsnNode)in;
        NumType type = NumType.getInstance(iin.operand);
        lb.append(asmop).append(type).nl();
    }

    
    private void arg_byte(AbstractInsnNode in) {
        int i = ((IntInsnNode)in).operand;
        lb.append(asmop).append(i).nl();
    }

    
    private void arg_callsite(AbstractInsnNode in) {
        jvmVersion.checkSupports(Feature.invokeDynamic);
        InvokeDynamicInsnNode idn = (InvokeDynamicInsnNode)in;
        ConstantDynamic cd = new ConstantDynamic(idn.name,idn.desc, idn.bsm, idn.bsmArgs);
        lb.append(asmop).appendRaw(o2s.constDynamic2String(cd)).nl();
    }

    
    private void arg_class(AbstractInsnNode in) {
        TypeInsnNode tin = (TypeInsnNode)in;
        lb.append(asmop).append(tin.desc).nl();
    }

    
    private void arg_constant(AbstractInsnNode in) {
        LdcInsnNode lin = (LdcInsnNode)in;
        Object cst = lin.cst;
        JvmOp jop = asmop;
        if (ConstType.getLDCsz(cst) == 2) {
            assert jop == JvmOp.asm_ldc;
            jop = JvmOp.opc_ldc2_w;
            CHECK_SUPPORTS(jop);
        }
        String cststr = o2s.asm2String(cst);
        lb.append(jop).appendRaw(cststr).nl();
    }

    
    private void arg_field(AbstractInsnNode in) {
        FieldInsnNode fin = (FieldInsnNode)in;
        String on = HandlePart.ownerName(fin.owner,fin.name);
        lb.append(asmop).append(on).append(fin.desc).nl();
   }

    
    private void arg_incr(AbstractInsnNode in) {
        IincInsnNode incn = (IincInsnNode)in;
        JvmOp jop = JvmOp.exactIncr(asmop,incn.var, incn.incr);
        lb.append(jop).append(incn.var).append(incn.incr).nl();
    }

    
    private void arg_label(AbstractInsnNode in) {
        JumpInsnNode jin = (JumpInsnNode)in;
        lb.append(asmop).appendLabelNode(jin.label).nl();
    }

    
    private void arg_lookupswitch(AbstractInsnNode in) {
        LookupSwitchInsnNode lsin = (LookupSwitchInsnNode)in;
        lb.append(asmop)
                .append(ReservedWord.res_default,lsin.dflt)
                .append(ReservedWord.dot_array)
                .nl();
        int i = 0;
        for (LabelNode labelnode:lsin.labels) {
            lb.append(lsin.keys.get(i))
                    .append(ReservedWord.right_arrow,labelnode)
                    .nl();
            ++i;
        }
        lb.append(Directive.end_array)
                .nl();
    }

    private void arg_marray(AbstractInsnNode in) {
        MultiANewArrayInsnNode manan = (MultiANewArrayInsnNode)in;
        lb.append(asmop).append(manan.desc).append(manan.dims).nl();
    }

    
    private void arg_method(AbstractInsnNode in) {
        MethodInsnNode min = (MethodInsnNode)in;
        MethodHandle mh = MethodHandle.of(min);
        lb.append(asmop).append(mh.iond()).nl();
    }

    
    private void arg_none(AbstractInsnNode in) {
        lb.append(asmop).nl();
    }

    
    private void arg_short(AbstractInsnNode in) {
        int i = ((IntInsnNode)in).operand;
        lb.append(asmop).append(i).nl();
    }

    
    private void arg_tableswitch(AbstractInsnNode in) {
        TableSwitchInsnNode tsin = (TableSwitchInsnNode)in;
        lb.append(JvmOp.asm_tableswitch)
                .append(tsin.min)
                .append(ReservedWord.res_default,tsin.dflt)
                .append(ReservedWord.dot_array)
                .nl();
        for (LabelNode labelnode:tsin.labels) {
            lb.appendLabelNode(labelnode)
                    .nl();
        }
        lb.append(Directive.end_array)
                .nl();
    }
    
    private void arg_var(AbstractInsnNode in) {
        VarInsnNode varnode = (VarInsnNode)in;
        int v = varnode.var;
        JvmOp jop = JvmOp.exactVar(asmop,v);
        if (jop.isImmediate()) {
            lb.append(jop).nl();
        } else {
            lb.append(jop).append(v).nl();
        }
    }

    private void frame(AbstractInsnNode in) {
        if (!jvmVersion.supports(StandardAttribute.StackMapTable)) {
             // "Version %s does not support %s (supported %s)"
            LOG(M57,jvmVersion,StandardAttribute.StackMapTable,StandardAttribute.StackMapTable.range());
            return;
        }
        FrameNode fn = (FrameNode) in;
        assert fn.type == Opcodes.F_NEW;
        Object[] thislocal = fn.local.toArray(); // they are objects
        // NOTE DOUBLE and LONG are NOT followed by TOP
        int min = Math.min(lastLocalStack.length,thislocal.length);
        int match;
        for (match = 0; match < min;++match) {
            if (!lastLocalStack[match].equals(thislocal[match])) {
                break;
            }
        }
        thislocal = Arrays.copyOfRange(thislocal, match, thislocal.length);
        FrameTypeValue[] locals = FrameTypeValue.from(Stream.of(thislocal), this::getLabelName);
        FrameTypeValue[] stacks = FrameTypeValue.from(fn.stack.stream(), this::getLabelName);
        lb.append(Directive.dir_stack);
        if (match != 0) {
            lb.append(ReservedWord.res_use);
            if (match != lastLocalStack.length) {
                lb.append(match);
            }
            lb.append(ReservedWord.res_locals);
        }
        lb.nl();
        lastLocalStack = fn.local.toArray(); // they are objects
        for (FrameTypeValue ftv:locals) {
            lb.append(ReservedWord.res_locals).append(ftv.ft()).appendNonNull(ftv.value()).nl();
        }
        for (FrameTypeValue ftv:stacks) {
          lb.append(ReservedWord.res_stack).append(ftv.ft()).appendNonNull(ftv.value()).nl();
        }
        lb.append(Directive.end_stack).nl();
    }
    
    private void arg_dir(AbstractInsnNode in) {
        if (in instanceof LabelNode) {
            LabelNode ln = (LabelNode) in;
            String labelname = getLabelName(ln);
            lb.append(labelname + Line.LABEL_INDICATOR).nl();
        } else if (in instanceof LineNumberNode) {
            LineNumberNode lnn = (LineNumberNode) in;
            lb.append(Directive.dir_line).append(lnn.line).nl();
        } else if (in instanceof FrameNode) {
            frame(in);
        } else {
            // "unknown ASM Node %s in instruction list"
            throw new LogAssertionError(M909,in.getClass());
        }
    }
    
}

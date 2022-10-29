package asm2jynx;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

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

import jvm.AttributeName;
import jvm.ConstType;
import jvm.Feature;
import jvm.JvmVersion;
import jvm.NumType;
import jvm.OpArg;
import jynx.Directive;
import jynx.LogAssertionError;
import jynx.LogIllegalStateException;
import jynx.ReservedWord;
import jynx2asm.handles.HandlePart;
import jynx2asm.handles.MethodHandle;
import jynx2asm.Line;
import jynx2asm.NameDesc;
import jynx2asm.ops.JvmOp;

public class Insn2Jynx {
    
    private final JvmVersion jvmVersion;
    private final Object2String o2s;
    private final LineBuilder lb;

    private final Map<Label, String> labelMap;
    private Object[] lastLocalStack;
    private JvmOp asmop;
    
    public Insn2Jynx(LineBuilder lb, JvmVersion jvmVersion, Object2String o2s, Object[] initstack) {
        this.o2s = o2s;
        this.jvmVersion = jvmVersion;
        this.lb = lb;
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

    public String getLabelName(Label label) {
        int maxchain = 256;
        int ct = maxchain;
        while(ct-- > 0) {
            String labref = labelMap.get(label);
            if (labref != null) {
                return labref;
            } else if (label.info instanceof LabelNode) {
                label = ((LabelNode)label.info).getLabel();
            } else {
                labref = NameDesc.GENERATED_LABEL_MARKER + "L" + labelMap.size();
                labelMap.put(label,labref);
                return labref;
            }
        }
        // "length of label node chain exceeds %d"
        throw new LogIllegalStateException(M27, maxchain);
    }
    
    
    public void arg_atype(AbstractInsnNode in) {
        IntInsnNode iin = (IntInsnNode)in;
        NumType type = NumType.getInstance(iin.operand);
        lb.append(asmop).append(type).nl();
    }

    
    public void arg_byte(AbstractInsnNode in) {
        int i = ((IntInsnNode)in).operand;
        lb.append(asmop).append(i).nl();
    }

    
    public void arg_callsite(AbstractInsnNode in) {
        jvmVersion.checkSupports(Feature.invokeDynamic);
        InvokeDynamicInsnNode idn = (InvokeDynamicInsnNode)in;
        ConstantDynamic cd = new ConstantDynamic(idn.name,idn.desc, idn.bsm, idn.bsmArgs);
        lb.append(asmop).appendRaw(o2s.constDynamic2String(cd)).nl();
    }

    
    public void arg_class(AbstractInsnNode in) {
        TypeInsnNode tin = (TypeInsnNode)in;
        lb.append(asmop).append(tin.desc).nl();
    }

    
    public void arg_constant(AbstractInsnNode in) {
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

    
    public void arg_field(AbstractInsnNode in) {
        FieldInsnNode fin = (FieldInsnNode)in;
        String on = HandlePart.ownerName(fin.owner,fin.name);
        lb.append(asmop).append(on).append(fin.desc).nl();
   }

    
    public void arg_incr(AbstractInsnNode in) {
        IincInsnNode incn = (IincInsnNode)in;
        JvmOp jop = JvmOp.exactIncr(asmop,incn.var, incn.incr,jvmVersion);
        lb.append(jop).append(incn.var).append(incn.incr).nl();
    }

    
    public void arg_interface(AbstractInsnNode in) {
        MethodInsnNode min = (MethodInsnNode)in;
        MethodHandle mh = MethodHandle.of(min);
        lb.append(asmop).append(mh.iond()).nl();
    }

    
    public void arg_label(AbstractInsnNode in) {
        JumpInsnNode jin = (JumpInsnNode)in;
        String labelname = getLabelName(jin.label);
        lb.append(asmop).append(labelname).nl();
    }

    
    public void arg_lookupswitch(AbstractInsnNode in) {
        LookupSwitchInsnNode lsin = (LookupSwitchInsnNode)in;
        Map<Integer,String> labmap = new LinkedHashMap<>();
        int i = 0;
        for (LabelNode ln:lsin.labels) {
            String labelname = getLabelName(ln);
            String mustbenull = labmap.put(lsin.keys.get(i), labelname);
            if (mustbenull != null) {
                // "%s: key %d has duplicate entries %s and %s"
                LOG(M154,JvmOp.asm_lookupswitch,lsin.keys.get(i),labelname,mustbenull);
            }
            ++i;
        }
        String deflab = getLabelName(lsin.dflt);
        lb.append(asmop)
                .append(ReservedWord.res_default)
                .append(deflab)
                .append(ReservedWord.left_array);
        boolean first = true;
        for (Map.Entry<Integer,String> me:labmap.entrySet()) {
            if (first) {
                first = false;
            } else {
                lb.append(ReservedWord.comma);
            }
            lb.append(me.getKey()).append(ReservedWord.right_arrow).append(me.getValue());
        }
        lb.append(ReservedWord.right_array).nl();
    }

    public void arg_marray(AbstractInsnNode in) {
        MultiANewArrayInsnNode manan = (MultiANewArrayInsnNode)in;
        lb.append(asmop).append(manan.desc).append(manan.dims).nl();
    }

    
    public void arg_method(AbstractInsnNode in) {
        MethodInsnNode min = (MethodInsnNode)in;
        MethodHandle mh = MethodHandle.of(min);
        lb.append(asmop).append(mh.iond()).nl();
    }

    
    public void arg_none(AbstractInsnNode in) {
        lb.append(asmop).nl();
    }

    
    public void arg_short(AbstractInsnNode in) {
        int i = ((IntInsnNode)in).operand;
        lb.append(asmop).append(i).nl();
    }

    
    public void arg_tableswitch(AbstractInsnNode in) {
        TableSwitchInsnNode tsin = (TableSwitchInsnNode)in;
        String[] labels = new String[tsin.labels.size()];
        int i = 0;
        for (LabelNode ln:tsin.labels) {
            String labelname = getLabelName(ln);
            labels[i] = labelname;
            ++i;
        }
        String deflab = getLabelName(tsin.dflt);
        lb.append(JvmOp.asm_tableswitch)
                .append(tsin.min)
                .append(ReservedWord.res_default)
                .append(deflab)
                .append(ReservedWord.left_array);
        boolean first = true;
        for (String  label:labels) {
            if (first) {
                first = false;
            } else {
                lb.append(ReservedWord.comma);
            }
            lb.append(label);
        }
        lb.append(ReservedWord.right_array).nl();
    }
    
    public void arg_var(AbstractInsnNode in) {
        VarInsnNode varnode = (VarInsnNode)in;
        int v = varnode.var;
        JvmOp jop = JvmOp.exactVar(asmop,v,jvmVersion);
        if (jop.isImmediate()) {
            lb.append(jop).nl();
        } else {
            lb.append(jop).append(v).nl();
        }
    }
    
    public void arg_dir(AbstractInsnNode in) {
        if (in instanceof LabelNode) {
            LabelNode ln = (LabelNode) in;
            String labelname = getLabelName(ln);
            lb.append(labelname + Line.LABEL_INDICATOR).nl();
        } else if (in instanceof LineNumberNode) {
            LineNumberNode lnn = (LineNumberNode) in;
            lb.appendDirective(Directive.dir_line).append(lnn.line).nl();
        } else if (in instanceof FrameNode) {
            if (!jvmVersion.supports(AttributeName.StackMapTable)) {
                 // "Version %s does not support %s (supported %s)"
                LOG(M57,jvmVersion,AttributeName.StackMapTable,AttributeName.StackMapTable.range());
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
            FrameTypeValue[] locals = FrameTypeValue.fromList(Arrays.asList(thislocal), this::getLabelName);
            FrameTypeValue[] stacks = FrameTypeValue.fromList(fn.stack, this::getLabelName);
            lb.appendDirective(Directive.dir_stack);
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
            lb.appendDirective(Directive.end_stack).nl();
        } else {
            // "unknown ASM Node %s in instruction list"
            throw new LogAssertionError(M909,in.getClass());
        }
    }
    
}

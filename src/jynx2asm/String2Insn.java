package jynx2asm;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.SortedMap;
import java.util.TreeMap;

import org.objectweb.asm.ConstantDynamic;

import static jynx.Global.*;
import static jynx.GlobalOption.GENERATE_LINE_NUMBERS;
import static jynx.Message.*;
import static jynx.ReservedWord.*;
import static jynx2asm.NameDesc.*;

import asm.instruction.*;

import jvm.ConstantPoolType;
import jvm.ConstType;
import jvm.Context;
import jvm.HandleType;
import jvm.OpArg;
import jynx.GlobalOption;
import jynx.LogIllegalStateException;
import jynx2asm.handles.FieldHandle;
import jynx2asm.handles.MethodHandle;
import jynx2asm.ops.*;

public class String2Insn {

    private final JynxLabelMap labelMap;
    private final LabelStack labelStack;
    private final ClassChecker checker;
    private final JynxOps opmap;
    
    private Line line;
    private boolean multi;
    private int macroCount;
    private int indent;
    
    private String2Insn(JynxLabelMap labelmap, ClassChecker checker, JynxOps opmap) {
        this.labelMap = labelmap;
        this.labelStack = new LabelStack();
        this.checker = checker;
        this.opmap = opmap;
        this.indent = IndentType.BEGIN.after();
    }

    public static String2Insn getInstance(JynxLabelMap labmap, ClassChecker checker, JynxOps opmap) {
        return new String2Insn(labmap, checker, opmap);
    }

    public JynxLabelMap getLabelMap() {
        return labelMap;
    }

    public void getInsts(InstList instlist) {
        line = instlist.getLine();
        if (line.isLabel()) {
            String lab = line.firstToken().asLabel();
            addLabel(lab, instlist);
            return;
        }
        String tokenstr = line.firstToken().asString();
        JynxOp jynxop = opmap.get(tokenstr);
        if (jynxop == null) {
            if (opmap.isLabel(tokenstr)) {
                addLabel(tokenstr,instlist);
                return;
            }
            LOG(M86,tokenstr); // "invalid op - %s"
            line.skipTokens();
            return;
        }
        if (OPTION(GlobalOption.__WARN_INDENT)) {
            IndentType itype = jynxop.indentType();
            indent += itype.before();
            if (line.getIndent() != indent) {
                LOG(M228,line.getIndent(),indent); // "indent %d found but expected %d"
            }
            indent += itype.after();
        }
        add(jynxop, instlist);
    }

    private void addLabel(String lab, InstList instlist) {
        line.noMoreTokens();
        JynxLabel target = labelMap.defineJynxLabel(lab, line);
        instlist.add(new LabelInstruction(JvmOp.xxx_label,target));
    }
    
    public void add(JynxOp jynxop, InstList instlist) {
        line = instlist.getLine();
        multi = false;
        macroCount = 0;
        add(jynxop,new ArrayDeque<>(),macroCount,instlist);
        line.noMoreTokens();
    }
    
    private final static int MAX_MACROS_FOR_LINE = 64;
    
    private void add(JynxOp jop, Deque<MacroOp> macrostack, int macct, InstList instlist) {
        if (multi) {
            LOG(M254,jop); // "%s is used in a macro after a mulit-line op"
        }
        if (jop instanceof SelectOp) {
            SelectOp selector = (SelectOp)jop;
            add(selector.getOp(line,instlist),macrostack,macct,instlist);
        } else if (jop instanceof JvmOp) {
            JvmOp jvmop = (JvmOp)jop;
            addJvmOp(jvmop,instlist);
        } else if (jop instanceof DynamicOp) {
            DynamicOp dynamicop = (DynamicOp)jop;
            instlist.add(dynamicop.getInstruction(line,checker));
        } else if (jop instanceof LineOp) {
            LineOp lineop = (LineOp)jop;
            lineop.adjustLine(line, macct, macrostack.peekLast(),labelStack);
        } else if (jop instanceof MacroOp) {
            MacroOp macroop = (MacroOp) jop;
            macrostack.addLast(macroop);
            ++macroCount;
            if (macroCount > MAX_MACROS_FOR_LINE) {
                // "number of macro ops exceeds maximum of %d for %s"
                throw new LogIllegalStateException(M317, MAX_MACROS_FOR_LINE, macrostack.peekFirst());
            }
            macct = macroCount;
            for (JynxOp mjop:macroop.getJynxOps()) {
                add(mjop,macrostack,macct,instlist);
            }
            macrostack.removeLast();
        } else {
            throw new AssertionError();
        }
    }
    
    public void visitEnd() {
        if (!labelStack.isEmpty()) {
            LOG(M249,labelStack.size()); // "structured op(s) missing; level at end is %d"
        }
    }

    private void addJvmOp(JvmOp jvmop, InstList instlist) {
        Instruction insn;
        OpArg oparg = jvmop.args();
        switch(oparg) {
            case arg_atype:insn = arg_atype(jvmop);break;
            case arg_byte:insn = arg_byte(jvmop);break;
            case arg_callsite:insn = arg_callsite(jvmop);break;
            case arg_class:insn = arg_class(jvmop);break;
            case arg_constant:insn = arg_constant(jvmop);break;
            case arg_dir:insn = arg_dir(jvmop);break;
            case arg_field:insn = arg_field(jvmop);break;
            case arg_incr:insn = arg_incr(jvmop);break;
            case arg_label:insn = arg_label(jvmop,instlist.isUnreachable());break;
            case arg_marray:insn = arg_marray(jvmop);break;
            case arg_method:case arg_interface:insn = arg_method(jvmop);break;
            case arg_none:insn = arg_none(jvmop);break;
            case arg_short:insn = arg_short(jvmop);break;
            case arg_stack:insn = arg_stack(jvmop);break;
            case arg_switch:insn = arg_switch(jvmop);break;
            case arg_var:insn = arg_var(jvmop);break;
            default:
                throw new EnumConstantNotPresentException(oparg.getClass(), oparg.name());
        }
        if (insn == null) {
            return;
        }
        instlist.add(insn);
    }
    
    private Instruction arg_atype(JvmOp jvmop) {
        int atype = line.nextToken().asTypeCode();
        return new IntInstruction(jvmop,atype);
    }
    
    private Instruction arg_byte(JvmOp jvmop) {
        int v = line.nextToken().asByte();
        return new IntInstruction(jvmop,v);
    }
    
    private Instruction arg_callsite(JvmOp jvmop) {
        JynxConstantDynamic jcd = new JynxConstantDynamic(line, checker);
        ConstantDynamic cd = jcd.getConstantDynamic4Invoke();
        return new DynamicInstruction(jvmop,cd);
    }
    
    private Instruction arg_class(JvmOp jvmop) {
        String typeo = line.nextToken().asString();
        String type = TRANSLATE_TYPE(typeo, false);
        if (jvmop == JvmOp.asm_new) {
            CLASS_NAME.validate(type);
            checker.usedNew(type);
        } else {
            OBJECT_NAME.validate(type);
        }
        return new TypeInstruction(jvmop, type);
    }
  
    @SuppressWarnings("fallthrough")
    private Instruction simpleConstant(JvmOp jvmop) {
        Token token = line.nextToken();
        Object value = token.getConst();
        ConstType ct = ConstType.getFromASM(value,Context.JVMCONSTANT);
        boolean ldc2 = jvmop == JvmOp.opc_ldc2_w;
        switch (ct) {
            case ct_int:
                if (ldc2) {
                    value = ((Integer)value).longValue();
                    ct = ConstType.ct_long;
                }
                break;
            case ct_float:
                if (ldc2) {
                    value = ((Float)value).doubleValue();
                    ct = ConstType.ct_double;
                }
                break;
            case ct_long:
                if (!ldc2) {
                    jvmop = JvmOp.opc_ldc2_w;
                }
                break;
            case ct_double:
                if (!ldc2) {
                    jvmop = JvmOp.opc_ldc2_w;
                }
                break;
            case ct_method_handle:
                CHECK_CAN_LOAD(ConstantPoolType.CONSTANT_MethodHandle);
                checker.mayBeHandle(value, line);
                // fall through
            default:
                if (ldc2) {
                    LOG(M138,JvmOp.opc_ldc2_w,value);   // "%s cannot be used for constant - %s"
                }
                break;
        }
        return new LdcInstruction(jvmop,value,ct);
    }
    
    private Instruction dynamicConstant(JvmOp jvmop) {
        ConstType ct = ConstType.ct_const_dynamic;
        JynxConstantDynamic jcd = new JynxConstantDynamic(line, checker);
        ConstantDynamic dyn = jcd.getConstantDynamic4Load();
        String desc = dyn.getDescriptor();
        if (desc.length() == 1) {
            ct = ConstType.getFromDesc(desc,Context.JVMCONSTANT);
        }
        if (dyn.getSize() == 2) {
            jvmop = JvmOp.opc_ldc2_w;
        }
        return new LdcInstruction(jvmop,dyn,ct);
    }
    
    private Instruction arg_constant(JvmOp jvmop) {
        Token leftbrace = line.peekToken();
        if (leftbrace.is(left_brace)) {
            return dynamicConstant(jvmop);
        } else {
            return simpleConstant(jvmop);
        }
    }
    
    private Instruction arg_dir(JvmOp jvmop) {
        switch (jvmop) {
            case xxx_label:
                String labstr = line.nextToken().asString();
                JynxLabel target = labelMap.defineJynxLabel(labstr, line);
                return new LabelInstruction(jvmop,target);
            case xxx_label_weak:
                labstr = line.nextToken().asString();
                target = labelMap.defineWeakJynxLabel(labstr, line);
                return target == null?null:new LabelInstruction(jvmop,target);
            case xxx_line:
                int lineno = line.nextToken().asUnsignedShort();
                if (OPTION(GENERATE_LINE_NUMBERS)) {
                    LOG(M95,GENERATE_LINE_NUMBERS); // ".line directives ignored as %s specified"
                    return null;
                }
                return new LineInstruction(lineno, line);
            default:
                throw new EnumConstantNotPresentException(JvmOp.class, jvmop.externalName());
            }
    }
    
    private Instruction arg_field(JvmOp jvmop) {
        String fname = line.nextToken().asString();
        String desc = line.nextToken().asString();
        FieldHandle fh = FieldHandle.getInstance(fname, desc,HandleType.fromOp(jvmop, false));
        checker.usedField(fh);
        return new FieldInstruction(jvmop,fh);
    }
    
    private Instruction arg_incr(JvmOp jvmop) {
        Token vartoken = line.nextToken();
        int incr = line.nextToken().asShort();
        return new IncrInstruction(jvmop,vartoken, incr);
    }

    private Instruction arg_label(JvmOp jvmop, boolean unreachable) {
        Token label = line.nextToken();
        if (jvmop == JvmOp.xxx_goto_weak) {
            if (unreachable) {
                return null;
            }
            jvmop = JvmOp.asm_goto;
        }
        JynxLabel jlab = getJynxLabel(label);
        return new JumpInstruction(jvmop,jlab);
    }

    private Instruction arg_marray(JvmOp jvmop) {
        String desc = line.nextToken().asString();
        ARRAY_DESC.validate(desc);
        int lastbracket = desc.lastIndexOf('[') + 1;
        int dims = line.nextToken().asUnsignedByte();
        if (dims == 0 || dims > lastbracket) {
            LOG(M253,dims,lastbracket);  // "illegal number of dimensions %d; must be in range [0,%d]"
        }
        return new MarrayInstruction(jvmop, desc, dims);
    }

    private Instruction arg_method(JvmOp jvmop) {
        String mspec = line.nextToken().asString();
        MethodHandle mh = MethodHandle.getInstance(mspec,jvmop);
        checker.usedMethod(mh, jvmop, line);
        return new MethodInstruction(jvmop, mh);
    }

    private Instruction arg_none(JvmOp jvmop) {
        if (jvmop == JvmOp.opc_wide) {
            LOG(M210,JvmOp.opc_wide);    // "%s instruction ignored as not required"
            return null;
        }
        return Instruction.getInstance(jvmop);
    }

    private Instruction arg_short(JvmOp jvmop) {
        int v = line.nextToken().asShort();
        return new IntInstruction(jvmop,v);
    }
    
    private Instruction arg_stack(JvmOp jvmop) {
        return new StackInstruction(jvmop);
    }

    private Instruction arg_switch(JvmOp jvmop) {
        Token token1 = line.nextToken();
        int low = 0;
        if (jvmop == JvmOp.asm_tableswitch && !token1.mayBe(res_default).isPresent()) {
            // "%s will be parsed as %s (remove low and add <number> -> )"
            LOG(M330,JvmOp.asm_tableswitch, JvmOp.opc_switch);
            low = token1.asInt();
            token1 = line.nextToken();
        }
        token1.mustBe(res_default);
        JynxLabel dflt = getJynxLabel(line.nextToken());
        SortedMap<Integer,JynxLabel> swmap = new TreeMap<>();
        try (TokenArray dotarray = line.getTokenArray()) {
            multi |= dotarray.isMultiLine(); 
            while (true) {
                Token value = dotarray.firstToken();
                Token label;
                if (value.is(right_array)) {
                    return SwitchInstruction.getInstance(jvmop, dflt, swmap);
                }
                int key;
                if (jvmop == JvmOp.asm_tableswitch && !dotarray.peekToken().is(right_arrow)) {
                    if (swmap.isEmpty()) {
                        // "%s will be parsed as %s (remove low and add <number> -> )"
                        LOG(M330,JvmOp.asm_tableswitch, JvmOp.opc_switch);
                    }
                    key = low;
                    ++low;
                    label = value;
                } else {
                    key = value.asInt();
                    dotarray.nextToken().mustBe(right_arrow);
                    label = dotarray.nextToken();
                }
                JynxLabel target = getJynxLabel(label);
                JynxLabel mustbenull = swmap.put(key, target);
                if (mustbenull != null && !mustbenull.equals(target)) {
                    // "duplicate key %d; previous target = %s, current target = %s"
                    LOG(M229,key,mustbenull.name(),target.name());
                }
                dotarray.noMoreTokens();
            }
        }
    }

    private JynxLabel getJynxLabel(Token token) {
        String labstr = token.asString();
        if (OPTION(GlobalOption.__STRUCTURED_LABELS) && Character.isDigit(labstr.codePointAt(0))) {
            int index = token.asInt();
            labstr = labelStack.peek(index).asString();
        }
        return labelMap.codeUseOfJynxLabel(labstr, line);
    }

    private Instruction arg_var(JvmOp jvmop) {
        Token token;
        if (jvmop.isImmediate()) {
            String opname = jvmop.externalName();
            char suffix = opname.charAt(opname.length() - 1);
            token = Token.getInstance("" + suffix);
        } else {
            token = line.nextToken();
        }
        return new VarInstruction(jvmop, token);
    }

}

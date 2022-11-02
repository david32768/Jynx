package jynx2asm;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.objectweb.asm.ConstantDynamic;

import static jynx.Global.*;
import static jynx.GlobalOption.GENERATE_LINE_NUMBERS;
import static jynx.Message.*;
import static jynx.ReservedWord.*;
import static jynx2asm.NameDesc.*;

import asm.instruction.DynamicInstruction;
import asm.instruction.FieldInstruction;
import asm.instruction.IncrInstruction;
import asm.instruction.Instruction;
import asm.instruction.IntInstruction;
import asm.instruction.JumpInstruction;
import asm.instruction.LabelInstruction;
import asm.instruction.LdcInstruction;
import asm.instruction.LineInstruction;
import asm.instruction.LookupInstruction;
import asm.instruction.MarrayInstruction;
import asm.instruction.MethodInstruction;
import asm.instruction.StackInstruction;
import asm.instruction.TableInstruction;
import asm.instruction.TypeInstruction;
import asm.instruction.VarInstruction;

import jvm.ConstantPoolType;
import jvm.ConstType;
import jvm.Context;
import jvm.HandleType;
import jvm.OpArg;
import jynx.GlobalOption;
import jynx.LogIllegalStateException;
import jynx2asm.handles.FieldHandle;
import jynx2asm.handles.MethodHandle;
import jynx2asm.ops.DynamicOp;
import jynx2asm.ops.JvmOp;
import jynx2asm.ops.JynxOp;
import jynx2asm.ops.JynxOps;
import jynx2asm.ops.LineOp;
import jynx2asm.ops.MacroOp;
import jynx2asm.ops.SelectOp;

public class String2Insn {

    private static final int MAX_METHOD_SIZE = 2*Short.MAX_VALUE + 1;
    public static final int INDENT_LENGTH = 2;
    
    private final JynxScanner js;
    private final JynxLabelMap labmap;
    private final LabelStack labelStack;
    private final ClassChecker checker;
    private final JynxOps opmap;
    
    private Line line;
    private boolean multi;
    private int macroCount;
    private int indent;
    
    public String2Insn(JynxScanner js, JynxLabelMap labmap,
            ClassChecker checker, JynxOps opmap) {
        this.js = js;
        this.labmap = labmap;
        this.labelStack = new LabelStack();
        this.checker = checker;
        this.opmap = opmap;
        this.indent = INDENT_LENGTH;
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
            if (jynxop.reduceIndentBefore()) {
                indent -= INDENT_LENGTH;
            }
            if (line.getIndent() != indent) {
                LOG(M228,line.getIndent(),indent); // "indent %d found but expected %d"
            }
            if (jynxop.increaseIndentAfter()) {
                indent += INDENT_LENGTH;
            }
        }
        add(jynxop, instlist);
    }

    private void addLabel(String lab, InstList instlist) {
        line.noMoreTokens();
        JynxLabel target = labmap.defineJynxLabel(lab, line);
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
            switchArg(jvmop).ifPresent(instlist::add);
        } else if (jop instanceof DynamicOp) {
            DynamicOp dynamicop = (DynamicOp)jop;
            instlist.add(dynamicop.getInstruction(js,line,checker));
        } else if (jop instanceof LineOp) {
            LineOp lineop = (LineOp)jop;
            lineop.adjustLine(line, macct, labelStack);
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

    private Optional<Instruction> switchArg(JvmOp jvmop) {
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
            case arg_label:insn = arg_label(jvmop);break;
            case arg_lookupswitch:insn = arg_lookupswitch(jvmop);break;
            case arg_marray:insn = arg_marray(jvmop);break;
            case arg_method:case arg_interface:insn = arg_method(jvmop);break;
            case arg_none:insn = arg_none(jvmop);break;
            case arg_short:insn = arg_short(jvmop);break;
            case arg_stack:insn = arg_stack(jvmop);break;
            case arg_tableswitch:insn = arg_tableswitch(jvmop);break;
            case arg_var:insn = arg_var(jvmop);break;
            default:
                throw new EnumConstantNotPresentException(oparg.getClass(), oparg.name());
        }
        return insn == null?Optional.empty():Optional.of(insn);
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
        JynxConstantDynamic jcd = new JynxConstantDynamic(js, line, checker);
        ConstantDynamic cd = jcd.getConstantDynamic4Invoke();
        return new DynamicInstruction(jvmop,cd);
    }
    
    private Instruction arg_class(JvmOp jvmop) {
        String typeo = line.nextToken().asString();
        String type = TRANSLATE_OWNER(typeo);
        if (jvmop == JvmOp.asm_new) {
            CLASS_NAME.validate(type);
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
                    LOG(M113,JvmOp.opc_ldc2_w,value);   // "%s must be used for long constant - %s"
                }
                break;
            case ct_double:
                if (!ldc2) {
                    LOG(M117,JvmOp.opc_ldc2_w); // "%s must be used for double constants but assumed float required"
                    value = ((Double)value).floatValue();
                    ct = ConstType.ct_float;
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
        JynxConstantDynamic jcd = new JynxConstantDynamic(js, line, checker);
        ConstantDynamic dyn = jcd.getConstantDynamic4Load();
        String desc = dyn.getDescriptor();
        if (desc.length() == 1) {
            ct = ConstType.getFromDesc(desc,Context.JVMCONSTANT);
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
                JynxLabel target = labmap.defineJynxLabel(labstr, line);
                return new LabelInstruction(jvmop,target);
            case xxx_labelweak:
                labstr = line.nextToken().asString();
                target = labmap.defineWeakJynxLabel(labstr, line);
                return target == null?null:new LabelInstruction(jvmop,target);
            case xxx_line:
                int lineno = line.nextToken().asUnsignedShort();
                if (OPTION(GENERATE_LINE_NUMBERS)) {
                    LOG(M95,GENERATE_LINE_NUMBERS); // ".line directives ignored as %s specified"
                    return null;
                }
                return new LineInstruction(lineno, line);
            default:
                throw new EnumConstantNotPresentException(JvmOp.class, jvmop.toString());
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
        int var = line.nextToken().asUnsignedShort();
        int incr = line.nextToken().asShort();
        return new IncrInstruction(jvmop,var, incr);
    }

    private Instruction arg_label(JvmOp jvmop) {
        JynxLabel jlab = getJynxLabel(line.nextToken());
        return new JumpInstruction(jvmop,jlab);
    }

    private static final int LOOKUPSWITCH_OVERHEAD = 4 + 4 + 4 + 4 - 1;
    // default label on 4 byte boundary, default label, n, at least two returns, one return value may be pshed on stack
    private final static int MAX_LOOKUP_ENTRIES = (MAX_METHOD_SIZE - LOOKUPSWITCH_OVERHEAD)/8;

    private Instruction arg_lookupswitch(JvmOp jvmop) {
        line.nextToken().mustBe(res_default);
        JynxLabel dflt = getJynxLabel(line.nextToken());
        TokenArray dotarray = TokenArray.getInstance(js, line);
        multi |= dotarray.isMultiLine(); 
        Map<Integer,JynxLabel> swmap = new TreeMap<>();
        Integer lastkey = null;
        while (true) {
            Token token = dotarray.firstToken();
            if (token.is(right_array)) {
                break;
            }
            Integer key = token.asInt();
            dotarray.nextToken().mustBe(right_arrow);
            JynxLabel target = getJynxLabel(dotarray.nextToken());
            JynxLabel mustbenull = swmap.put(key, target);
            if (mustbenull != null) {
                LOG(M229,key,mustbenull.name(),target.name());   // "duplicate key %d; previous target = %s, current target = %s"
            } else if (lastkey != null && key < lastkey) {
                LOG(M230,key,lastkey);   // "keys must be in ascending order; key = %d; previous key = %s"
            }
            lastkey = key;
            dotarray.noMoreTokens();
        }
        if (swmap.size() > MAX_LOOKUP_ENTRIES) {
            LOG(M256,jvmop,swmap.size(),MAX_LOOKUP_ENTRIES); // "%s has %d entries, maximum possible is %d"
        }
        return new LookupInstruction(jvmop,dflt,swmap);
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

    private JynxLabel getJynxLabel(Token token) {
        String labstr = token.asString();
        if (OPTION(GlobalOption.__STRUCTURED_LABELS) && Character.isDigit(labstr.codePointAt(0))) {
            int index = token.asInt();
            labstr = labelStack.peek(index).asString();
        }
        return labmap.codeUseOfJynxLabel(labstr, line);
    }

    private static final int TABLESWITCH_OVERHEAD = 4 + 4 + 4 + 4 + 4 - 1;
    // default label on 4 byte boundary, default label, low, n, at least two returns, one return value may be pshed on stack
    private final static int MAX_TABLE_ENTRIES = (MAX_METHOD_SIZE - TABLESWITCH_OVERHEAD)/4;
    
    private Instruction arg_tableswitch(JvmOp jvmop) {
        int min = line.nextToken().asInt();
        line.nextToken().mustBe(res_default);
        JynxLabel dflt = getJynxLabel(line.nextToken());
        TokenArray dotarray = TokenArray.getInstance(js, line);
        multi |= dotarray.isMultiLine(); 
        List<JynxLabel> labellist = new ArrayList<>();
        int lct = 0;
        while (true) {
            Token token = dotarray.firstToken();
            if (token.is(right_array)) {
                break;
            }
            JynxLabel label = getJynxLabel(token);
            labellist.add(label);
            ++lct;
            dotarray.noMoreTokens();
        }
        if (lct == 0) {
            LOG(M224,jvmop,res_default); // "invalid %s as only has %s"
            labellist.add(dflt);
            ++lct;
        }
        if (lct > MAX_TABLE_ENTRIES) {
            LOG(M256,jvmop,lct,MAX_TABLE_ENTRIES); // "%s has %d entries, maximum possible is %d"
        }
        return new TableInstruction(jvmop, min, min + lct - 1,dflt,labellist);
    }
    
    private Instruction arg_var(JvmOp jvmop) {
        int v;
        if (jvmop.isImmediate()) {
            String opname = jvmop.toString();
            char suffix = opname.charAt(opname.length() - 1);
            v = Integer.valueOf("" + suffix);
        } else {
            v = line.nextToken().asUnsignedShort();
        }
        return new VarInstruction(jvmop, v);
    }

}

package jynx2asm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.objectweb.asm.ConstantDynamic;

import static jynx.Global.*;
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
import asm.instruction.TryInstruction;
import asm.instruction.TypeInstruction;
import asm.instruction.VarInstruction;

import jvm.AsmOp;
import jvm.ConstantPoolType;
import jvm.ConstType;
import jvm.Context;
import jvm.JvmOp;
import jvm.Op;
import jvm.OpArg;

import jynx.GlobalOption;
import jynx.LogAssertionError;
import jynx.LogIllegalArgumentException;
import jynx.ReservedWord;

import jynx2asm.ops.AliasOp;
import jynx2asm.ops.DynamicOp;
import jynx2asm.ops.JynxOp;
import jynx2asm.ops.JynxOps;
import jynx2asm.ops.LineOp;
import jynx2asm.ops.LineOps;
import jynx2asm.ops.MacroOp;
import jynx2asm.ops.StructuredOps;

public class String2Insn {

    private static final int MAX_METHOD_SIZE = 2*Short.MAX_VALUE + 1;
    public static final int INDENT_LENGTH = 2;
    
    private final JynxScanner js;
    private final JynxLabelMap labmap;
    private final LabelStack labelStack;
    private final ClassChecker checker;
    private final AsmOp returnop;
    private final String className;
    
    private List<Instruction> instructions;
    private Line line;
    private boolean multi;
    private int macroCount;
    private boolean addDotLine;
    private final boolean generateDotLine;
    
    public String2Insn(JynxScanner js, JynxLabelMap labmap,
            ClassChecker checker, AsmOp returnop, String mname) {
        this.js = js;
        this.labmap = labmap;
        this.labelStack = new LabelStack();
        this.checker = checker;
        this.returnop = returnop;
        this.className = checker.getClassName();
        this.generateDotLine = OPTION(GlobalOption.GENERATE_LINE_NUMBERS);
    }

    public List<Instruction> getInsts(Line linex) {
        line = linex;
        instructions = new ArrayList<>();
        if (line.isLabel()) {
            String lab = line.firstToken().asLabel();
            line.noMoreTokens();
            JynxLabel target = labmap.defineJynxLabel(lab, line);
            instructions.add(new LabelInstruction(AsmOp.xxx_label,target));
            return instructions;
        }
        String tokenstr = line.firstToken().asString();
        JynxOp jynxop = JynxOps.getInstance(tokenstr);
        if (jynxop == null) {
            LOG(M86,tokenstr); // "invalid op - %s"
            line.skipTokens();
            return instructions;
        }
        if (OPTION(GlobalOption.WARN_INDENT)) {
            int expected = INDENT_LENGTH * (labelStack.size() + 1);
            if (jynxop instanceof StructuredOps && ((StructuredOps)jynxop).reduceIndent()) {
                expected -= 2;
            }
            if (line.getIndent() != expected) {
                LOG(M228,line.getIndent(),expected); // "indent %d found but expected %d"
            }
        }
        macroCount = 0;
        addDotLine = false;
        multi = false;
        add(jynxop,macroCount);
        line.noMoreTokens();
        if (addDotLine) {
            instructions.add(0,new LineInstruction(null,line.getLinect()));
        }
        return instructions;
    }

    private void add(JynxOp jop, int macct) {
        if (multi) {
            LOG(M254,jop); // "%s is used in a macro after a mulit-line op"
        }
        if (jop instanceof AliasOp) {
            AliasOp alias = (AliasOp)jop;
            alias.getInst(line, returnop).ifPresent(instructions::add);
        } else if (jop instanceof JvmOp) {
            JvmOp jvmop = (JvmOp)jop;
            switchArg(jvmop).ifPresent(instructions::add);
        } else if (jop instanceof DynamicOp) {
            DynamicOp dynamicop = (DynamicOp)jop;
            instructions.add(dynamicop.getInstruction(js,line,checker));
        } else if (jop instanceof LineOp) {
            LineOp lineop = (LineOp)jop;
            lineop.adjustLine(line, macct, labelStack);
        } else if (jop instanceof MacroOp) {
            MacroOp macroop = (MacroOp) jop;
            ++macroCount;
            macct = macroCount;
            for (JynxOp mjop:macroop.getJynxOps()) {
                add(mjop,macct);
            }
        } else {
            throw new AssertionError();
        }
    }
    
    public void visitEnd() {
        if (!labelStack.isEmpty()) {
            LOG(M249,labelStack.size()); // "structured op(s) missing; level at end is %d"
        }
    }
    
    private Instruction wide() {
        line.noMoreTokens();
        LOG(M210,Op.opc_wide);    // "%s instruction ignored as never required"
        return null;
    }
    

    private Optional<Instruction> switchArg(JvmOp jvmop) {
        Instruction insn;
        OpArg oparg = jvmop.getBase().args();
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
            case arg_tableswitch:insn = arg_tableswitch(jvmop);break;
            case arg_var:insn = arg_var(jvmop);break;
            default:
                throw new EnumConstantNotPresentException(oparg.getClass(), oparg.name());
        }
        return insn == null?Optional.empty():Optional.of(insn);
    }
    
    private Instruction arg_atype(JvmOp jvmop) {
        addDotLine = generateDotLine;
        int atype = line.nextToken().asTypeCode();
        return new IntInstruction(jvmop,atype);
    }
    
    private Instruction arg_byte(JvmOp jvmop) {
        int v = line.nextToken().asByte();
        return new IntInstruction(jvmop,v);
    }
    
    private Instruction arg_callsite(JvmOp jvmop) {
        addDotLine = generateDotLine;
        JynxConstantDynamic jcd = new JynxConstantDynamic(js, line, checker);
        ConstantDynamic cd = jcd.getConstantDynamic4Invoke();
        return new DynamicInstruction(jvmop,cd);
    }
    
    private Instruction arg_class(JvmOp jvmop) {
        addDotLine = generateDotLine;
        String type = line.nextToken().asString();
        if (jvmop.getBase() == AsmOp.asm_new && OPTION(GlobalOption.PREPEND_CLASSNAME)) {
            if (type.equals("/")) {
                type = className;
                LOG(M255,jvmop); // "classname has been added to argument of some %s instruction(s)"
            }
            CLASS_NAME.validate(type);
        } else {
            OBJECT_NAME.validate(type);
        }
        return new TypeInstruction(jvmop, type);
    }
  
    private Instruction simpleConstant(JvmOp jvmop) {
        Token token = line.nextToken();
        Object value = token.getConst();
        ConstType ct = ConstType.getFromASM(value,Context.JVMCONSTANT);
        boolean ldc2 = jvmop == Op.opc_ldc2_w;
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
                    LOG(M113,Op.opc_ldc2_w,value);   // "%s must be used for long constant - %s"
                }
                break;
            case ct_double:
                if (!ldc2) {
                    LOG(M117,Op.opc_ldc2_w); // "%s must be used for double constants but assumed float required"
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
                    LOG(M138,Op.opc_ldc2_w,value);   // "%s cannot be used for constant - %s"
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
        if (jvmop == AsmOp.xxx_label) {
            String labstr = line.nextToken().asString();
            JynxLabel target = labmap.defineJynxLabel(labstr, line);
            return new LabelInstruction(jvmop,target);
        }
        if (jvmop == Op.opc_labelweak) {
            String labstr = line.nextToken().asString();
            JynxLabel target = labmap.defineWeakJynxLabel(labstr, line);
            return target == null?null:new LabelInstruction(jvmop,target);
        }
        if (jvmop == AsmOp.xxx_catch) {
            String fromname = line.nextToken().toString();
            String toname = line.nextToken().toString();
            String usingname = line.nextToken().toString();
            JynxCatch jcatch = JynxCatch.getInstance(line, fromname, toname, usingname, null, labmap);
            return new TryInstruction(AsmOp.xxx_catch,jcatch);
        }
        throw new AssertionError();
    }
    
    private Instruction arg_field(JvmOp jvmop) {
        String fname = line.nextToken().asString();
        String desc = line.nextToken().asString();
        FieldDesc fd = FieldDesc.getInstance(fname, desc,jvmop.getBase());
        checker.usedField(fd,jvmop);
        return new FieldInstruction(jvmop,fd);
    }
    
    private Instruction arg_incr(JvmOp jvmop) {
        int var = line.nextToken().asUnsignedShort();
        int incr = line.nextToken().asShort();
        return new IncrInstruction(jvmop,var, incr);
    }

    private Instruction arg_label(JvmOp jvmop) {
        String lab = line.nextToken().asString();
        JynxLabel jlab = labmap.codeUseOfJynxLabel(lab, line);
        return new JumpInstruction(jvmop,jlab);
    }

    private static final int LOOKUPSWITCH_OVERHEAD = 4 + 4 + 4 + 4 - 1;
    // default label on 4 byte boundary, default label, n, at least two returns, one return value may be pshed on stack
    private final static int MAX_LOOKUP_ENTRIES = (MAX_METHOD_SIZE - LOOKUPSWITCH_OVERHEAD)/8;

    private Instruction arg_lookupswitch(JvmOp jvmop) {
        ReservedWord rw = line.nextToken().mayBe(res_default);
        if (rw == null) {
            throw new LogIllegalArgumentException(M199,jvmop);  // "%s has now a different format from Jynx 2.4"
        }
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
            dotarray.nextToken().mustBe(colon);
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
        addDotLine = generateDotLine;
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
        addDotLine = generateDotLine;
        String mspec = line.nextToken().asString();
        OwnerNameDesc cmd = OwnerNameDesc.getOwnerMethodDescAndCheck(mspec,jvmop.getBase());
        checker.usedMethod(cmd, jvmop, line);
        return new MethodInstruction(jvmop, cmd);
    }

    private Instruction arg_none(JvmOp jvmop) {
        addDotLine = generateDotLine && (jvmop == AsmOp.asm_idiv  || jvmop == AsmOp.asm_ldiv);
        if (jvmop == Op.opc_wide) {
            return wide();
        }
        return jvmop.getBase().isStack()?new StackInstruction(jvmop):Instruction.getInstance(jvmop);
    }

    private Instruction arg_short(JvmOp jvmop) {
        int v = line.nextToken().asShort();
        return new IntInstruction(jvmop,v);
    }
    
    private JynxLabel getJynxLabel(Token token) {
        String labstr = token.asString();
        if (Character.isDigit(labstr.codePointAt(0))) {
            int index = token.asInt();
            labstr = LineOps.peekEndLabel(index, labelStack);
        }
        return labmap.codeUseOfJynxLabel(labstr, line);
    }

    private static final int TABLESWITCH_OVERHEAD = 4 + 4 + 4 + 4 + 4 - 1;
    // default label on 4 byte boundary, default label, low, n, at least two returns, one return value may be pshed on stack
    private final static int MAX_TABLE_ENTRIES = (MAX_METHOD_SIZE - TABLESWITCH_OVERHEAD)/4;
    
    private Instruction arg_tableswitch(JvmOp jvmop) {
        int min = line.nextToken().asInt();
        ReservedWord rw = line.nextToken().mayBe(res_default);
        if (rw == null) {
            throw new LogIllegalArgumentException(M199,jvmop);  // "%s has now a different format from Jynx 2.4"
        }
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

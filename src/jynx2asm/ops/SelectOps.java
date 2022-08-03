package jynx2asm.ops;

import java.util.stream.Stream;

import static jynx.Message.M278;
import static jynx.Message.M282;
import static jynx2asm.ops.JvmOp.*;

import jvm.NumType;
import jynx.LogIllegalArgumentException;
import jynx2asm.FrameElement;
import jynx2asm.InstList;
import jynx2asm.Line;
import jynx2asm.Token;

public enum SelectOps implements SelectOp {
    
    opc_ildc(Type.ILDC,
            asm_iconst_0,asm_iconst_1,asm_iconst_2,asm_iconst_3,asm_iconst_4,asm_iconst_5,
            asm_iconst_m1,asm_bipush,asm_sipush,asm_ldc),
    opc_lldc(Type.LLDC,asm_lconst_0,asm_lconst_1,opc_ldc2_w),
    opc_fldc(Type.FLDC,asm_fconst_0,asm_fconst_1,asm_fconst_2,asm_ldc,ExtendedOps.xxx_fraw),
    opc_dldc(Type.DLDC,asm_dconst_0,asm_dconst_1,opc_ldc2_w,ExtendedOps.xxx_draw),

    xxx_xreturn(Type.RETURN),
    
    xxx_xload_rel(Type.REL_LOCAL,asm_iload,asm_lload,asm_fload,asm_dload,asm_aload),
    xxx_xstore_rel(Type.REL_STACK,asm_istore,asm_lstore,asm_fstore,asm_dstore,asm_astore),
    ;
        
    private final Select selector;

    private SelectOps(SelectOps.Type type, JynxOp... ops) {
        this.selector = new Select(type, ops);
    }

    @Override
    public boolean isExternal() {
        return name().startsWith("opc_");
    }

    @Override
    public JynxOp getOp(Line line, InstList instlist) {
        return selector.getOp(line,instlist);
    }

    public static Stream<SelectOps> streamExternal() {
        return Stream.of(values())
            .filter(SelectOps::isExternal);
    }
    
    private static enum Type {
        STACK_LENGTH,
        STACK_TYPE,
        ILDC,
        LLDC,
        FLDC,
        DLDC,
        REL_STACK,
        REL_LOCAL,
        ABS_LOCAL,
        RETURN,
        ;
    }

    public static SelectOp of12(JynxOp oplen1, JynxOp oplen2) {
        return new Select(Type.STACK_LENGTH, oplen1, oplen2);
    }

    public static SelectOp ofIJFDA(JynxOp opi, JynxOp opj, JynxOp opf, JynxOp opd, JynxOp opa) {
        return new Select(Type.STACK_TYPE, opi, opj, opf, opd, opa);
    }

    public static SelectOp ofIJFD(JynxOp opi, JynxOp opj, JynxOp opf, JynxOp opd) {
        return new Select(Type.STACK_TYPE, opi, opj, opf, opd);
    }

    private static class Select implements SelectOp {

        private final Type type;
        private final JynxOp[] ops;

        private Select(SelectOps.Type type, JynxOp... ops) {
            this.type = type;
            this.ops = ops;
            assert type != Type.STACK_TYPE || ops.length == 4  || ops.length == 5;
            assert type != Type.STACK_LENGTH || ops.length == 2;
        }

        @Override
        public JynxOp getOp(Line line,InstList instlist) {
            int index;
            switch (type) {
                case RETURN:
                    return instlist.getReturnOp();
                case STACK_LENGTH:
                    index = getLength(line,instlist);
                    break;
                case STACK_TYPE:
                    index = getTypeIndex(instlist.peekTOS());
                    break;
                case ABS_LOCAL:
                    index = getAbsLocal(line,instlist);
                    break;
                case REL_STACK:
                    index = getRelStackType(line,instlist);
                    break;
                case REL_LOCAL:
                    index = getRelLocal(line,instlist);
                    break;
                case ILDC:
                    index = getIldc(line,instlist);
                    break;
                case LLDC:
                    index = getLldc(line,instlist);
                    break;
                case FLDC:
                    index = getFldc(line,instlist);
                    break;
                case DLDC:
                    index = getDldc(line,instlist);
                    break;
                default:
                    throw new EnumConstantNotPresentException(type.getClass(), type.name());
            }
            if (index < 0 || index >= ops.length) {
                throw new AssertionError();
            }
            return ops[index];
        }

        private int getLength(Line line,InstList instlist) {
            FrameElement fe = instlist.peekTOS();
            return fe.slots() - 1;
        }

        private final static String ILFDA = "ilfda";
        
        private int getTypeIndex(FrameElement fe) {
            char fetype = fe.instLetter();
            int index = ILFDA.indexOf(fetype);
            if (index < 0 || index >= ops.length) {
                // "element %s (%c) at top of stack is not one of %s"
                throw new LogIllegalArgumentException(M282,fe,fetype,ILFDA.substring(0,Math.min(ILFDA.length(),ops.length)));
            }
            assert ops[index].toString().charAt(0) == fetype:
                    String.format("%s %c", ops[index],fetype);
            return index;
        }
        
        private int getRelStackType(Line line,InstList instlist) {
            int num  = line.nextToken().asUnsignedShort();
            num = instlist.absolute(num);
            line.insert(Integer.toString(num));
            return getTypeIndex(instlist.peekTOS());
        }

        private int getAbsLocal(Line line,InstList instlist) {
            int num  = line.peekToken().asUnsignedShort();
            return getTypeIndex(instlist.peekVar(num));
        }

        private int getRelLocal(Line line,InstList instlist) {
            int num  = line.nextToken().asUnsignedShort();
            num = instlist.absolute(num);
            line.insert(Integer.toString(num));
            return getAbsLocal(line,instlist);
        }

        private int getIldc(Line line,InstList instlist) {
            Token token = line.nextToken();
            int ival = token.asInt();
            if (ival >= 0 && ival <= 5) {
                return ival;
            }
            if (ival == -1) {
                assert ops[6] == asm_iconst_m1;
                return 6;
            }
            line.insert(Integer.toString(ival));
            if (NumType.t_byte.isInRange(ival)) {
                assert ops[7] == asm_bipush;
                return 7;
            } else if (NumType.t_short.isInRange(ival)) {
                assert ops[8] == asm_sipush;
                return 8;
            } else {
                assert ops[9] == asm_ldc;
                return 9;
            }
        }

        private int getLldc(Line line,InstList instlist) {
            Token token = line.nextToken();
            long lval = token.asLong();
            if (lval == 0L) {
                assert ops[0] == asm_lconst_0;
                return 0;
            } else if (lval == 1L) {
                assert ops[1] == asm_lconst_1;
                return 1;
            } else {
                line.insert(Long.toString(lval) + 'L');
                assert ops[2] == opc_ldc2_w;
                return 2;
            }
        }

        private final static int F_NAN_PREFIX = 0x7f800000;
        private final static int F_NAN_CANONICAL = Float.floatToRawIntBits(Float.NaN);
        private final static int F_NAN_LIMIT = 1 << 23;
        
        private int getFldc(Line line,InstList instlist) {
            Token token = line.nextToken();
            String str = token.toString();
            if (str.equals("-nan") || str.equals("nan") || str.equals("+nan")) {
                str += ":" + Integer.toHexString(F_NAN_CANONICAL & ~F_NAN_PREFIX);
            }
            if (str.startsWith("nan:") || str.startsWith("-nan:")  || str.startsWith("+nan:")) {
                int num = Integer.valueOf(str.substring(str.indexOf(':') + 1),16);
                if (num <= 0 || num >= F_NAN_LIMIT) {
                    // "NaN type %#x is not in (0,%#x)"
                    throw new LogIllegalArgumentException(M278, num, F_NAN_LIMIT);
                }
                num |= F_NAN_PREFIX;
                if (str.charAt(0) == '-') {
                    num |= 1 << 31; // set sign bit
                }
                line.insert("0x" + Integer.toHexString(num));
                assert ops[4] == ExtendedOps.xxx_fraw;
                return 4;
            }
            if (str.equals("inf") || str.equals("+inf") || str.equals("-inf")) {
                if (str.equals("inf")) {
                    str = "+" + str;
                }
                line.insert(str.replace("inf","InfinityF"));
                assert ops[3] == asm_ldc;
                return 3;
            }
            float fval = token.asFloat();
            if (fval == 0.0F && 1/fval > 0.0F) { // +0.0f not -0.0F
                assert ops[0] == asm_fconst_0;
                return 0;
            } else if (fval == 1.0f) {
                assert ops[1] == asm_fconst_1;
                return 1;
            } else if (fval == 2.0f) {
                assert ops[2] == asm_fconst_2;
                return 2;
            } else {
                if (Float.isNaN(fval)) {
                    line.insert("+NaNF"); // ldc requires + sign
                } else if (Float.isInfinite(fval) && fval > 0) {
                    line.insert("+InfinityF");  // ldc requires + sign
                } else  {
                    line.insert(Float.toHexString(fval) + 'F');
                }
                assert ops[3] == asm_ldc;
                return 3;
            }
        }

        private final static long D_NAN_PREFIX = 0x7ff0000000000000L;
        private final static long D_NAN_CANONICAL = Double.doubleToRawLongBits(Double.NaN);
        private final static long D_NAN_LIMIT = 1L << 52;
        
        private int getDldc(Line line,InstList instlist) {
            Token token = line.nextToken();
            String str = token.toString();
            if (str.equals("-nan") || str.equals("nan") || str.equals("+nan")) {
                str += ":" + Long.toHexString(D_NAN_CANONICAL & ~D_NAN_PREFIX);
            }
            if (str.startsWith("nan:") || str.startsWith("-nan:")  || str.startsWith("+nan:")) {
                String hexstr = str.substring(str.indexOf(':') + 1);
                long num = Long.valueOf(hexstr,16);
                if (num <= 0 || num >= D_NAN_LIMIT) {
                    // "NaN type %#x is not in (0,%#x)"
                    throw new LogIllegalArgumentException(M278, num, D_NAN_LIMIT);
                }
                num |= D_NAN_PREFIX;
                if (str.charAt(0) == '-') {
                    num |= 1L << 63; // set sign bit 
                }
                line.insert("0x" + Long.toHexString(num) + "L");
                assert ops[3] == ExtendedOps.xxx_draw;
                return 3;
            }
            if (str.equals("inf") || str.equals("+inf") || str.equals("-inf")) {
                if (str.equals("inf")) {
                    str = "+" + str;
                }
                line.insert(str.replace("inf","InfinityF"));
                assert ops[2] == opc_ldc2_w;
                return 2;
            }
            double dval = token.asDouble();
            if (dval == 0.0 && 1/dval > 0.0) { // +0.0 not -0.0
                assert ops[0] == asm_dconst_0;
                return 0;
            } else if (dval == 1.0) {
                assert ops[1] == asm_dconst_1;
                return 1;
            } else {
                if (Double.isNaN(dval)) {
                    line.insert("+NaN"); // ldc requires + sign
                } else if (Double.isInfinite(dval) && dval > 0) {
                    line.insert("+Infinity"); // ldc requires + sign
                } else {
                    line.insert(Double.toHexString(dval));
                }
                assert ops[2] == opc_ldc2_w;
                return 2;
            }
        }
    
    }
    
}

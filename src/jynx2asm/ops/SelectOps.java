package jynx2asm.ops;

import java.util.stream.Stream;

import static jynx.Message.M278;
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
    
    xxx_dupn(Type.LENGTH,asm_dup, asm_dup2),
    xxx_popn(Type.LENGTH,asm_pop,asm_pop2),
    xxx_dupn_xn(Type.LENGTH,asm_dup_x1,asm_dup2_x2),
    
    xxx_xload(Type.ABS_LOCAL,asm_iload,asm_lload,asm_fload,asm_dload,asm_aload),
    xxx_xload_rel(Type.REL_LOCAL,asm_iload,asm_lload,asm_fload,asm_dload,asm_aload),
    xxx_xstore(Type.TYPE,asm_istore,asm_lstore,asm_fstore,asm_dstore,asm_astore),
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
        LENGTH,
        TYPE,
        ILDC,
        LLDC,
        FLDC,
        DLDC,
        REL_STACK,
        ABS_LOCAL,
        REL_LOCAL,
        RETURN,
        ;
    }

    public static SelectOp of12(JynxOp oplen1, JynxOp oplen2) {
        return new Select(Type.LENGTH, oplen1, oplen2);
    }

    public static SelectOp ofIJFDA(JynxOp opi, JynxOp opj, JynxOp opf, JynxOp opd, JynxOp opa) {
        return new Select(Type.TYPE, opi, opj, opf, opd, opa);
    }

    private static class Select implements SelectOp {

        private final Type type;
        private final JynxOp[] ops;

        private Select(SelectOps.Type type, JynxOp... ops) {
            this.type = type;
            this.ops = ops;
        }

        @Override
        public JynxOp getOp(Line line,InstList instlist) {
            int index;
            switch (type) {
                case RETURN:
                    return instlist.getReturnOp();
                case LENGTH:
                    index = getLength(line,instlist);
                    break;
                case TYPE:
                    index = getType(line,instlist);
                    break;
                case REL_STACK:
                    index = getRelStackType(line,instlist);
                    break;
                case ABS_LOCAL:
                    index = getAbsLocal(line,instlist);
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

        private int getType(Line line,InstList instlist) {
            FrameElement fe = instlist.peekTOS();
            return "IJFDA".indexOf(fe.typeLetter());
        }

        private int getRelStackType(Line line,InstList instlist) {
            int num  = line.nextToken().asUnsignedShort();
            num = instlist.absolute(num);
            line.insert(Integer.toString(num));
            return getType(line,instlist);
        }

        private int getAbsLocal(Line line,InstList instlist) {
            int num  = line.peekToken().asUnsignedShort();
            FrameElement fe = instlist.peekVar(num);
            return "IJFDA".indexOf(fe.typeLetter());
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
                return 6;
            }
            line.insert(token);
            if (NumType.t_byte.isInRange(ival)) {
                return 7;
            } else if (NumType.t_short.isInRange(ival)) {
                return 8;
            } else {
                return 9;
            }
        }

        private int getLldc(Line line,InstList instlist) {
            Token token = line.nextToken();
            long lval = token.asLong();
            if (lval == 0L) {
                return 0;
            } else if (lval == 1L) {
                return 1;
            } else {
                line.insert(Long.toString(lval) + 'L');
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
                str += ":" + Long.toHexString(D_NAN_CANONICAL & ~D_NAN_PREFIX);
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
                return 4;
            }
            if (str.equals("inf") || str.equals("+inf") || str.equals("-inf")) {
                if (str.equals("inf")) {
                    str = "+" + str;
                }
                line.insert(str.replace("inf","InfinityF"));
                return 3;
            }
            float fval = token.asFloat();
            if (fval == 0.0F && 1/fval > 0.0F) { // +0.0f not -0.0F
                return 0;
            } else if (fval == 1.0f) {
                return 1;
            } else if (fval == 2.0f) {
                return 2;
            } else {
                if (Float.isNaN(fval)) {
                    line.insert("+NaNF"); // ldc requires + sign
                } else if (Float.isInfinite(fval) && fval > 0) {
                    line.insert("+InfinityF");  // ldc requires + sign
                } else if (!token.asString().endsWith("F")) {
                    line.insert(token.asString() + 'F');
                } else {
                    line.insert(token);
                }
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
                return 3;
            }
            if (str.equals("inf") || str.equals("+inf") || str.equals("-inf")) {
                if (str.equals("inf")) {
                    str = "+" + str;
                }
                line.insert(str.replace("inf","InfinityF"));
                return 3;
            }
            double dval = token.asDouble();
            if (dval == 0.0 && 1/dval > 0.0) { // +0.0 not -0.0
                return 0;
            } else if (dval == 1.0) {
                return 1;
            } else {
                if (Double.isNaN(dval)) {
                    line.insert("+NaN"); // ldc requires + sign
                } else if (Double.isInfinite(dval) && dval > 0) {
                    line.insert("+Infinity"); // ldc requires + sign
                } else {
                    line.insert(token);
                }
                return 2;
            }
        }
    
    }
    
}

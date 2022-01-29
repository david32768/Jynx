package jynx2asm.ops;

import java.util.Objects;

import org.objectweb.asm.ConstantDynamic;

import static jynx.Message.M257;

import asm.instruction.DynamicInstruction;
import asm.instruction.Instruction;
import jvm.AsmOp;
import jynx.LogIllegalArgumentException;
import jynx2asm.ClassChecker;
import jynx2asm.JynxConstantDynamic;
import jynx2asm.JynxScanner;
import jynx2asm.Line;
import jynx2asm.NameDesc;

public class DynamicVarArgsOp implements DynamicOp {

    private final String name;
    private final String desctype;
    private final Integer ct;
    private final int maxct;
    private final String rettype;
    private final String boot;

    private DynamicVarArgsOp(String name, String desctype, Integer ct, String rettype, String boot) {
        this.name = name;
        this.desctype = desctype;
        this.ct = ct;
        this.maxct = Math.min(254,(Short.MAX_VALUE - rettype.length() - 2)/desctype.length());
        this.rettype = rettype;
        this.boot = boot;
        assert ct == null || ct >= 0 && ct <= maxct;
    }

    public static DynamicVarArgsOp getInstance(String name, Class<?> arrayclass, Integer ct,
            String bootclass,String bootmethod) {
        Objects.nonNull(name);
        Objects.nonNull(bootclass);
        Objects.nonNull(bootmethod);
        assert arrayclass.isArray();
        String desctype = arrayclass.getName().substring(1).replace(".", "/");
        assert NameDesc.CLASS_NAME.validate(bootclass);
        assert NameDesc.METHOD_ID.validate(bootmethod);
        String rettype = arrayclass.getName().replace('.', '/');
        String boot = bootclass + '/' + bootmethod;
        return new DynamicVarArgsOp(name, desctype,ct,rettype, boot);
    }

    @Override
    public Instruction getInstruction(JynxScanner js,Line line, ClassChecker checker) {
        String namex = name;
        int ctx;
        if (ct == null) {
            ctx = line.nextToken().asInt();
        } else {
            ctx = ct;
        }
        if (ctx < 0 || ctx > maxct) {
            throw new LogIllegalArgumentException(M257,ctx,maxct); // "argument count %d is not in range [0,%d]"
        }
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for (int i = 0; i < ctx;++i) {
            sb.append(desctype);
        }
        sb.append(")");
        sb.append(rettype);
        String descx = sb.toString();
        assert descx.length() <= Short.MAX_VALUE;
        JynxConstantDynamic jcd = new JynxConstantDynamic(js, line, checker);
        ConstantDynamic cd = jcd.getSimple(namex, descx, boot, "");
        return new DynamicInstruction(AsmOp.asm_invokedynamic, cd);
    }
    
    @Override
    public String toString() {
        return String.format("*DynamicVarArgs %s boot %s",name,boot);
    }
    
}

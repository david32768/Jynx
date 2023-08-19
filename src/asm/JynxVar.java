package asm;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import static jvm.StandardAttribute.LocalVariableTypeTable;
import static jvm.StandardAttribute.Signature;
import static jynx.Global.LOG;
import static jynx.Message.M217;
import static jynx.ReservedWord.res_from;
import static jynx.ReservedWord.res_is;
import static jynx.ReservedWord.res_signature;
import static jynx.ReservedWord.res_to;

import jynx.Global;
import jynx2asm.JynxLabel;
import jynx2asm.JynxLabelMap;
import jynx2asm.Line;
import jynx2asm.NameDesc;
import jynx2asm.Token;

public class JynxVar {
    
    private final int varnum;
    private final String name;
    private final String desc;
    private final String signature;
    private final JynxLabel fromref;
    private final JynxLabel toref;
    private final Line line;

    private JynxVar(int varnum, String name, String desc, String signature, JynxLabel fromref, JynxLabel toref, Line line) {
        this.varnum = varnum;
        this.name = name;
        this.desc = desc;
        this.signature = signature;
        this.fromref = fromref;
        this.toref = toref;
        this.line = line;
    }

    public int varnum() {
        return varnum;
    }

    public String desc() {
        return desc;
    }

    public Line getLine() {
        return line;
    }
    
    public static JynxVar getInstance(Line line, JynxLabelMap labelmap) {
        int varnum = line.nextToken().asUnsignedShort();
        String name = line.after(res_is);
        String desc = line.nextToken().asString();
        String vsignature = line.optAfter(res_signature);
        JynxLabel fromref = labelmap.startLabel();
        JynxLabel toref = labelmap.endLabel();
        Token token = line.peekToken();
        if (token != Token.END_TOKEN) {
            String fromname = line.after(res_from);
            fromref = labelmap.useOfJynxLabel(fromname, line);
            String toname = line.after(res_to);
            toref = labelmap.useOfJynxLabel(toname, line);
        } else {
            line.nextToken();
        }
        line.noMoreTokens();
        if (vsignature != null) {
            Global.CHECK_SUPPORTS(LocalVariableTypeTable);
            Global.CHECK_SUPPORTS(Signature);
            NameDesc.FIELD_SIGNATURE.validate(vsignature);
        }
        return new JynxVar(varnum,name, desc, vsignature, fromref, toref, line);
    }
    
    public void accept(MethodVisitor mv) {
        Label from = fromref.asmlabel();
        Label to = toref.asmlabel();
        if (fromref.isLessThan(toref)) {
            mv.visitLocalVariable(name, desc, signature, from, to, varnum);
        } else {
            LOG(line,M217,fromref.name(),toref.name()); //"from label %s is not before to label %s"
        }
    }
}

package jvm;

import java.util.EnumSet;
import java.util.stream.Stream;

import static org.objectweb.asm.Opcodes.*;

import org.objectweb.asm.Handle;

import static jvm.AsmOp.*;
import static jynx.Message.*;
import static jynx.Message.M101;
import static jynx.Message.M76;

import jynx.LogIllegalArgumentException;

public enum HandleType {
    
    // jvms 4.4.8
    REF_getField("GF",H_GETFIELD,1,asm_getfield,ConstantPoolType.CONSTANT_Fieldref),
    REF_getStatic("GS",H_GETSTATIC,2,asm_getstatic,ConstantPoolType.CONSTANT_Fieldref),
    REF_putField("PF",H_PUTFIELD,3,asm_putfield,ConstantPoolType.CONSTANT_Fieldref),
    REF_putStatic("PS",H_PUTSTATIC,4,asm_putstatic,ConstantPoolType.CONSTANT_Fieldref),
    REF_invokeVirtual("VL",H_INVOKEVIRTUAL,5,asm_invokevirtual,ConstantPoolType.CONSTANT_Methodref),
    REF_invokeStatic("ST",H_INVOKESTATIC,6,asm_invokestatic,ConstantPoolType.CONSTANT_Methodref,
            Feature.invokestatic_interface,ConstantPoolType.CONSTANT_InterfaceMethodref),
    REF_invokeSpecial("SP",H_INVOKESPECIAL,7,asm_invokespecial,ConstantPoolType.CONSTANT_Methodref,
            Feature.invokespecial_interface,ConstantPoolType.CONSTANT_InterfaceMethodref),
    REF_newInvokeSpecial("NW",H_NEWINVOKESPECIAL,8,asm_invokespecial,ConstantPoolType.CONSTANT_Methodref),
    REF_invokeInterface("IN",H_INVOKEINTERFACE,9,asm_invokeinterface,ConstantPoolType.CONSTANT_InterfaceMethodref),
    ;
    
    private final String mnemonic;
    private final int reftype;
    private final AsmOp op;
    private final ConstantPoolType maincpt;
    private final Feature altfeature;
    private final ConstantPoolType altcpt;
    
    private HandleType(String mnemonic,int reftype, int refnum, AsmOp op, ConstantPoolType maincpt) {
        this(mnemonic, reftype, refnum, op, maincpt, Feature.never, null);
    }
    
    private HandleType(String mnemonic,int reftype, int refnum, AsmOp op, ConstantPoolType maincpt,
            Feature altfeature, ConstantPoolType altcpt) {
        this.mnemonic = mnemonic;
        // "%s: asm value (%d) does not agree with jvm value(%d)"
        assert reftype == refnum:M161.format(name(),reftype,refnum);
        assert reftype == 1 + this.ordinal();
        this.reftype = reftype;
        this.op = op;
        this.maincpt = maincpt;
        this.altfeature = altfeature;
        this.altcpt = altcpt;
        assert ordinal() == reftype - 1;
    }

    private String getMnemonic() {
        return mnemonic;
    }

    public AsmOp op() {
        return op;
    }

    public int reftype() {
        return reftype;
    }

    public EnumSet<ConstantPoolType>  getValidCPT(JvmVersion jvmversion) {
        if (altcpt != null && jvmversion.supports(altfeature)) {
            return EnumSet.of(maincpt,altcpt);
        }
        return EnumSet.of(maincpt);
    }
    
    public boolean isField() {
        return maincpt == ConstantPoolType.CONSTANT_Fieldref;
    }
    
    @Override
    public String toString() {
        return name().replace("REF_","") + "(" + mnemonic + ")";
    }

    public static ConstantPoolType constantpool(Handle handle) {
        int typref = handle.getTag();
        HandleType ht = HandleType.getInstance(typref);
        ConstantPoolType cp;
        if (ht.isField()) {
            cp = ConstantPoolType.CONSTANT_Fieldref;
        } else if (handle.isInterface()) {
            cp = ConstantPoolType.CONSTANT_InterfaceMethodref;
        } else {
            cp = ConstantPoolType.CONSTANT_Methodref;
        }
        return cp;
    }
    
    public static HandleType getInstance(int reftype) {
        return Stream.of(values())
                .filter(ht -> ht.reftype == reftype)
                .findFirst()
                .orElseThrow(()-> new LogIllegalArgumentException(M76,reftype)); // "unknown handle tag: %d"
    }

    public static HandleType of(Handle handle) {
        int tag = handle.getTag();
        return getInstance(tag);
    }
    
    public static HandleType fromMnemonic(String mnemonic) {
        return Stream.of(values())
                .filter(ht -> ht.mnemonic.equals(mnemonic))
                .findFirst()
                .orElseThrow(()-> new LogIllegalArgumentException(M101,mnemonic)); // "unknown handle mnemonic: %s"
    }

    public static HandleType fromOp(AsmOp op, boolean init) {
        if (init && op == asm_invokespecial) {
            return REF_newInvokeSpecial;
        }
        return Stream.of(values())
                .filter(ht -> ht.op == op)
                .findFirst()
                .get();
    }

    public final static char SEP = ':';
    
    public static String getPrefix(int tag) {
        HandleType ht = getInstance(tag); 
        String htype = ht.getMnemonic();
        return htype + SEP;
    }

}

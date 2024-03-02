package jynx2asm.frame;

import java.util.BitSet;
import java.util.List;

import org.objectweb.asm.tree.ParameterNode;

import jvm.FrameType;
import jynx2asm.handles.JynxHandle;
import jynx2asm.handles.LocalMethodHandle;
import jynx2asm.ops.JvmOp;

public class MethodParameters {
    
    private final ParameterNode[]  parameters;
    private final BitSet finalParms;
    private final List<Object> initFrame;
    private final JvmOp returnOp;
    private final boolean isStatic;

    private MethodParameters(ParameterNode[]  parameters, BitSet finalParms,
            boolean isStatic, List<Object> initFrame, JvmOp returnOp) {
        this.parameters = parameters;
        this.finalParms = finalParms;
        this.initFrame = initFrame;
        this.returnOp = returnOp;
        this.isStatic = isStatic;
    }

    public static MethodParameters getInstance(ParameterNode[] parameters, BitSet finalParms,
            LocalMethodHandle lmh, boolean isStatic, String className) {
        List<Object> initFrame = FrameType.getInitFrame(className, isStatic,  lmh);
        JvmOp returnOp = JynxHandle.getReturnOp(lmh);
        return new MethodParameters(parameters, finalParms, isStatic, initFrame, returnOp);
    }
    
    public BitSet getFinalParms() {
        return finalParms;
    }

    public ParameterNode[] getParameters() {
        return parameters;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public List<Object> getInitFrame() {
        return initFrame;
    }
    
    public JvmOp getReturnOp() {
        return  returnOp;
    }
    
}

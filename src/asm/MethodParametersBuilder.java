package asm;

import java.util.BitSet;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.ParameterNode;

import static jynx.Global.LOG;
import static jynx.Message.M187;
import static jynx.Message.M194;
import static jynx.Message.M234;
import static jynx.Message.M308;
import static jynx.Message.M309;
import static jynx.Message.M310;

import jvm.AccessFlag;
import jynx.Access;
import jynx.Directive;
import jynx2asm.frame.MethodParameters;
import jynx2asm.handles.LocalMethodHandle;

public class MethodParametersBuilder {
    
    private final ParameterNode[] parameters;
    private final BitSet finalParms;
    private final int numparms;
    
    private int visibleMaxParm;
    private int invisibleMaxParm;
    private int parmct;
    private int visibleDefault;
    private int invisibleDefault;
    
    
    public MethodParametersBuilder(int numparms) {
        this.numparms = numparms;
        this.parameters = new ParameterNode[numparms];
        this.visibleMaxParm = -1;
        this.invisibleMaxParm = -1;
        this.visibleDefault = numparms;
        this.invisibleDefault = numparms;
        this.finalParms = new BitSet(numparms);
    }

    public MethodParameters getMethodParameters(LocalMethodHandle lmh, boolean isstatic, String classname) {
        return MethodParameters.getInstance(parameters, finalParms, lmh, isstatic, classname);
    }
    
    public void visitParameter(int parmnum, Access accessname) {
        assert parmnum >= 0;
        String pname = accessname.name();
        int pflags = accessname.getAccess();
        
        if (parmnum >= numparms) {
            // "parameter number %d is not in range [0,%d]"
            LOG(M308,parmnum, numparms - 1);
            return;
        }
        if (parameters[parmnum] != null) {
            // "parameter %d has already been defined: %s"
            LOG(M310,parmnum,parameters[parmnum].name);
            return;
        }
        if (accessname.is(AccessFlag.acc_final)) {
            finalParms.set(parmnum);
        }
        ParameterNode pn = new ParameterNode(pname, pflags);
        parameters[parmnum] = pn;
        ++parmct;
    }
    
    private boolean hasVisibleMax() {
        return visibleMaxParm >= 0;
    }

    private boolean hasInvisibleMax() {
        return invisibleMaxParm >= 0;
    }

    public void checkParameterAnnotation(int parameter, boolean visible) {
        int mnodect = numparms;
        if (visible && hasVisibleMax()) {
            mnodect = visibleMaxParm;
        } else if (!visible && hasInvisibleMax()) {
            mnodect = invisibleMaxParm;
        } 
        if (parameter < 0 || parameter >= mnodect) {
            LOG(M234,parameter,mnodect); // "invalid parameter number %d; bounds are [0 - %d)"
            parameter = parameter < 0?0:mnodect;
        }
        if (visible) {
            visibleDefault = Integer.max(parameter + 1, visibleDefault);
        } else {
            invisibleDefault = Integer.max(parameter + 1, invisibleDefault);
        }
    }

    public void visitAnnotableCount(int count, boolean visible) {
        if (count < 1 || count > numparms) {
            LOG(M187,count,numparms);    // "annotation parameter count(%d) not in range[1,%d]"
            count = numparms;
        }
        boolean already = visible?hasVisibleMax():hasInvisibleMax();
        if (already) {
            LOG(M194);   // "annotation parameter count already been set"
        } else {
            if (visible) {
                visibleMaxParm = count;
            } else {
                invisibleMaxParm = count;
            }
        }
    }

    public void accept(MethodVisitor mv) {
        if (parmct > 0) {
            int parmnum = 0;
            for (ParameterNode pn: parameters) {
                if (pn == null) {
                    // "missing %s %d : null parameter added"
                    LOG(M309, Directive.dir_parameter, parmnum);
                    mv.visitParameter(null, 0);
                } else {
                    mv.visitParameter(pn.name, pn.access);
                }
                ++parmnum;
            }
        }
        visibleMaxParm = hasVisibleMax()?visibleMaxParm:visibleDefault;
        mv.visitAnnotableParameterCount(visibleMaxParm, true);
        invisibleMaxParm = hasInvisibleMax()?invisibleMaxParm:invisibleDefault;
        mv.visitAnnotableParameterCount(invisibleMaxParm, false);
    }
    
}

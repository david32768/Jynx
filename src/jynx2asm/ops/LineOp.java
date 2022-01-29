package jynx2asm.ops;

import jynx2asm.LabelStack;
import jynx2asm.Line;

public interface LineOp extends JynxOp {

    @Override
    default public Integer length(){
        return 0;
    }
    
    public void adjustLine(Line line, int macrolevel, LabelStack labelStack);

}

package jynx2asm;

import java.util.ArrayList;
import java.util.BitSet;

import static jynx.Message.M248;
import static jynx.Message.M264;
import static jynx.Message.M265;

import jynx.LogIllegalArgumentException;
import jynx.LogIllegalStateException;

public class LabelStack {

    private final ArrayList<Token> stack = new ArrayList<>();
    private final BitSet ifs = new BitSet();

    public LabelStack() {}

    public void push(Token element) {
        stack.add(element);
    }
    
    public void pushIf(Token element) {
        ifs.set(stack.size());
        stack.add(element);
    }
    
    public boolean isEmpty() {
        return stack.isEmpty();
    }
    
    public int size() {
        return stack.size();
    }
    
    private int last() {
        if (isEmpty()) {
            // "structured op mismatch: label stack is empty"
            throw new LogIllegalArgumentException(M265);
        }
        return stack.size() - 1;
    }
    
    public Token peek() {
        return stack.get(last());
    }
    
    public Token peekIf() {
        if (!ifs.get(stack.size() - 1)) {
            throw new LogIllegalStateException(M248); // "ELSE does not match an IF or TRY op"
        }
        ifs.clear(stack.size() - 1);
        return stack.get(last());
    }
    
    public Token pop() {
        ifs.clear(stack.size() - 1);
        return stack.remove(last());
    }
    
    public Token peek(int index) {
        int actual = last() - index;
        if (actual < 0 || index < 0) {
            // "structured op mismatch: index %d in label stack is not in  range [0,%d]"
            throw new LogIllegalArgumentException(M264,index,last());
        }
        return stack.get(actual);
    }
    
}

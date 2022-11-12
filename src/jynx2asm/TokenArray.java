package jynx2asm;

import java.util.LinkedHashMap;
import java.util.Map;

import static jynx.Global.LOG;
import static jynx.Message.M233;

import jynx.Directive;
import jynx.ReservedWord;

public interface TokenArray extends TokenDeque, AutoCloseable {

    @Override
    public void close();
    
    public boolean isMultiLine();
    
    public static TokenArray getInstance(JynxScanner js, Line line) {
        Token token = line.peekToken();
        boolean multiline = token.is(ReservedWord.dot_array);
        return multiline? new DotArray(js, line):new LineArray(line);
    }

    public static String[] arrayString(Directive dir, JynxScanner js, Line line, NameDesc nd) {
        Map<String,Line> modlist = new LinkedHashMap<>();
        try (TokenArray array = TokenArray.getInstance(js, line)) {
            while(true) {
                Token token = array.firstToken();
                if (token.is(ReservedWord.right_array)) {
                    break;
                }
                String mod = token.asString();
                boolean ok = nd.validate(mod);
                if (ok) {
                    Line previous = modlist.putIfAbsent(mod,line);
                    if (previous != null) {
                        LOG(M233,mod,dir,previous.getLinect()); // "Duplicate entry %s in %s: previous entry at line %d"
                    }
                }
                array.noMoreTokens();
            }
        }
        return modlist.keySet().toArray(new String[0]);
    }

}

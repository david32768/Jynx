package jynx2asm;

import java.util.function.Predicate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static jynx.Global.LOG;
import static jynx.Message.M233;

import jynx.Directive;
import jynx.ReservedWord;

public interface TokenArray extends TokenDeque, AutoCloseable {

    @Override
    public void close();
    
    public boolean isMultiLine();

    public Line line();
    
    public static TokenArray getInstance(JynxScanner js, Line line) {
        Token token = line.peekToken();
        if (token.is(ReservedWord.dot_array)) {
            return new DotArray(js, line);
        }
        if (token.asString().startsWith(ReservedWord.left_array.externalName())) {
            return new LineArray(line);
        }
        return new ElementArray(line);
    }

    public static String[] arrayString(Directive dir, Line line, NameDesc nd) {
        Map<String,Line> modlist = new LinkedHashMap<>();
        arrayString(modlist, dir, line, nd);
        return modlist.keySet().toArray(String[]::new);
    }

    public static List<String> listString(Directive dir, Line line, NameDesc nd) {
        Map<String,Line> modlist = new LinkedHashMap<>();
        arrayString(modlist, dir, line, nd);
        return List.copyOf(modlist.keySet());
    }

    public static List<String> listString(Directive dir, Line line, Predicate<String> checker) {
        Map<String,Line> modlist = new LinkedHashMap<>();
        arrayString(modlist, dir, line, checker);
        return List.copyOf(modlist.keySet());
    }

    private static void arrayString(Map<String,Line> modlist, Directive dir, Line line, NameDesc nd) {
        arrayString(modlist, dir, line, nd::validate);
    }

    private static void arrayString(Map<String,Line> modlist, Directive dir, Line line, Predicate<String> checker) {
        try (TokenArray array = line.getTokenArray()) {
            while(true) {
                Token token = array.firstToken();
                if (token.is(ReservedWord.right_array)) {
                    break;
                }
                String mod = token.asString();
                boolean ok = checker.test(mod);
                if (ok) {
                    line = array.line();
                    Line previous = modlist.putIfAbsent(mod,line);
                    if (previous != null) {
                        LOG(M233,mod,dir,previous.getLinect()); // "Duplicate entry %s in %s: previous entry at line %d"
                    }
                }
                array.noMoreTokens();
            }
        }
    }

    public static void debugString(StringBuilder sb, Line line) {
        try (TokenArray array = line.getTokenArray()) {
            while(true) {
                Token token = array.firstToken();
                if (token.is(ReservedWord.right_array)) {
                    break;
                }
                String str = token.asQuoted();
                sb.append(str);
                array.noMoreTokens();
            }
        }
    }

}

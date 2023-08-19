package checker;

import java.io.PrintWriter;

public class IndentPrinter {

    private final PrintWriter pw;
    private final String indent;

    public IndentPrinter(PrintWriter pw) {
        this.pw = pw;
        this.indent = "";
    }

    private IndentPrinter(PrintWriter pw, String indent) {
        this.pw = pw;
        this.indent = indent;
        if (indent.length() > MAXIMUM_LINE_LENGTH/2) {
            throw new IllegalArgumentException();
        }
    }

    private static final String INDENT_SHIFT = "  ";
    private static final String CONTINUATION = "+ ";
    private static final int MAXIMUM_LINE_LENGTH = 110;
    private static final int MAXIMUM_SEARCH_LENGTH = 20;
    private static final String NEWLINE = "\n";
    private static final String SPLIT_CHARS = " );/"; // in preferred order for splitting
    
    public IndentPrinter shift() {
        return new IndentPrinter(pw, indent + INDENT_SHIFT);
    }
    
    private int preferredSplit(String line, String splits, int max, int search) {
        assert line.length() > max && search > 0 && max/4 >= search;
        int last = splits.length();
        int split = max - 1;
        for (int i = split; i > max - search; --i) {
            char c = line.charAt(i);
            int index = splits.indexOf(c);
            if (index >= 0 && index < last) {
                last = index;
                split = i;
                if (index == 0) {
                    break;
                }
            }
        }
        return split + 1;
    }
    
    public void println(String format, Object... values) {
        String multiline = String.format(format,values) + NEWLINE;
        String[] lines = multiline.split(NEWLINE);
        boolean first = true;
        for (String line:lines) {
            String x; 
            if (first) {
                first = false;
                x = indent;
            } else {
                x = INDENT_SHIFT + indent;
            }
            line = x + line;
            while(line.length() > MAXIMUM_LINE_LENGTH) {
                int split = preferredSplit(line, SPLIT_CHARS, MAXIMUM_LINE_LENGTH, MAXIMUM_SEARCH_LENGTH);
                String head = line.substring(0,split);
                String tail = line.substring(split);
                pw.println(head);
                line = CONTINUATION + tail;
            }
            if (!line.equals(CONTINUATION)) {
                pw.println(line);
            }
        }
        pw.flush();
    }
    
}

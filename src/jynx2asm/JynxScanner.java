package jynx2asm;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Scanner;

import static jynx.Global.*;
import static jynx.Message.*;

import jynx.Directive;

public class JynxScanner implements Iterator<Line> {

    private int linect = 0;
    private Line line = Line.EMPTY;
    private boolean reread;

    private final Scanner lines;

    public JynxScanner(Scanner lines) {
        this.lines = lines;
    }

    public void skipLinesUntil(Directive enddir) {
        assert enddir.isEndDirective();
        Line current = line;
        while(nextLineNotEnd(enddir) != null) {
            skipTokens();
        }
        LOGGER().setLine(current.toString());
    }
    
    public Line getLine() {
        return line;
    }

    private void nextLine() {
        assert line != null:M79.format(); // "Trying to read beyond end of file"
        line.noMoreTokens();
        String linestr;
        do {
            if (!lines.hasNextLine()) {
                line = null;
                lines.close();
                return;
            }
            linestr = lines.nextLine();
            ++linect;
        } while (linestr.trim().length() == 0 || linestr.trim().startsWith(";")); // ignore empty lines and comments
        line = Line.tokenise(linestr, linect);
        LOGGER().setLine(line.toString());
    }

    @Override
    public boolean hasNext() {
        return line != null;
    }
    
    @Override
    public Line next() {
        if (line == null) {
            throw new NoSuchElementException();
        }
        if (reread) {
            LOGGER().setLine(line.toString());
            reread = false;
            return line;
        }
        nextLine();
        if (line == null) {
            return Line.tokenise(".end_class", Integer.MAX_VALUE);
        }
        return line;
    }
    
    public Line nextLineNotEnd(Directive enddir) {
        assert enddir.isEndDirective();
        nextLine();
        if (!line.isDirective()) {
            return line;
        }
        Token first = line.peekToken();
        Directive dir = first.asDirective();
        if (dir == enddir) {
            line.firstToken();
            line.noMoreTokens();
            return null;
        } else if (dir != Directive.end) {
            LOG(M127,dir,enddir);    // "directive %s reached before %s"
            reread = true;
            return null;
        }
        line.firstToken();
        String endof = line.nextToken().asString();
        String dirstr = "end_" + endof;
        Optional<Directive> dirend = Directive.getDirInstance(dirstr);
        if (dirend.isPresent() && dirend.get() == enddir) {
            // "this %s directive has been replaced by %s"
            LOG(M193,dir,enddir);
        } else {
            LOG(M127,dir,enddir);    // "directive %s reached before %s"
        }
        return null;
    }
    
    public void skipNested(Directive startdir,Directive enddir, Directive... allowarr) {
        assert enddir.isEndDirective();
        List<Directive> allow = Arrays.asList(allowarr);
        int nest = 1;
        while (nest > 0) {
            nextLine();
            if (!line.isDirective()) {
                skipTokens();
                continue;
            }
            Token first = line.firstToken();
            Directive dir = first.asDirective();
            if (allow.contains(dir)) {
                skipTokens();
                continue;
            }
            if (dir == startdir) {
                ++nest;
                skipTokens();
                continue;
            }
            if (dir != enddir) {
                LOG(M127,dir,enddir);    // "directive %s reached before %s"
                return;
            }
            line.noMoreTokens();
            --nest;
        }
    }
  
    public void skipTokens() {
        line.skipTokens();
    }
    
}

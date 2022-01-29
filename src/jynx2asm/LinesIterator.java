package jynx2asm;

import java.util.Iterator;
import java.util.NoSuchElementException;

import static jynx.Global.LOG;
import static jynx.Message.M119;
import jynx.Directive;

public class LinesIterator implements Iterator<Line>,AutoCloseable {

    private final JynxScanner js;
    private final Directive enddir;

    private Line line;
    private boolean finished;
    
    public LinesIterator(JynxScanner js, Directive enddir) {
        assert enddir.isEndDirective();
        this.js = js;
        this.enddir = enddir;
        this.line = null;
        this.finished = false;
    }

    @Override
    public boolean hasNext() {
        if (!finished && line == null) {
            line = js.nextLineNotEnd(enddir);
            finished = line == null;
        }
        return !finished;
    }

    @Override
    public Line next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        Line result = line;
        line = null;
        return result;
    }

    @Override
    public void close() throws Exception {
        if (hasNext()) {
            LOG(M119,enddir); // "this and following lines skipped until %s"
            while(js.nextLineNotEnd(enddir) != null);
        }
    }
    
}

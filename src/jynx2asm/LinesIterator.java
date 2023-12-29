package jynx2asm;

import java.util.Iterator;
import java.util.NoSuchElementException;

import static jynx.Global.LOG;
import static jynx.Global.LOGGER;
import static jynx.Message.M119;

import jynx.Directive;

public class LinesIterator implements Iterator<Line>,AutoCloseable {

    private final JynxScanner js;
    private final Directive enddir;

    private Line line;
    private boolean finished;
    private boolean hasNexted;
    
    public LinesIterator(JynxScanner js, Directive enddir) {
        assert enddir.isEndDirective();
        this.js = js;
        this.enddir = enddir;
        this.line = null;
        this.finished = false;
        this.hasNexted = false;
    }

    @Override
    public boolean hasNext() {
        if (!finished && !hasNexted) {
            line = js.nextLineNotEnd(enddir);
            hasNexted = true;
            finished = line == null;
        }
        return !finished;
    }

    @Override
    public Line next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        hasNexted = false;
        LOGGER().setLine(line.toString());
        return line;
    }

    @Override
    public void close() {
        if (finished) {
            return;
        }
        LOG(M119,enddir); // "this and following lines skipped until %s"
        LOGGER().pushCurrent();
        if (hasNext()) {
            next().skipTokens();;
            while(hasNext()) {
                next().skipTokens();;
            }
        }
        LOGGER().popCurrent();
    }
    
}

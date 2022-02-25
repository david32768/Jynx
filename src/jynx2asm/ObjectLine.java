package jynx2asm;

public class ObjectLine<T> {
    
    private final T object;
    private final Line line;

    public ObjectLine(T object, Line line) {
        this.object = object;
        this.line = line;
    }
    
    public T object() {
        return object;
    }
    
    public Line line() {
        return line;
    }

    @Override
    public String toString() {
        return String.format("%s %s",object,line);
    }
}

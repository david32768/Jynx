package jvm;

public interface JvmVersioned {

    public default JvmVersionRange range(){
        return JvmVersionRange.UNLIMITED;
    }
    
}

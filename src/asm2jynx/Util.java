package asm2jynx;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class Util {

    private Util(){}
    
    public static boolean isAbsent(Iterable<?> list) {
        return list == null || !list.iterator().hasNext();
    }
    
    public  static boolean isPresent(Iterable<?> list) {
        return !isAbsent(list);
    }

    public static <E> List<E> nonNullList(List<E> list) {
        return list == null ? Collections.emptyList() : list;
    }

    public static <E> Iterable<E> nonNullIterable(Iterable<E> list) {
        return list == null ? Collections.emptyList() : list;
    }

    public static boolean isAnyPresent(Iterable<?>... lists) {
        return Stream.of(lists)
                .filter(Util::isPresent)
                .findAny()
                .isPresent();
    }
}

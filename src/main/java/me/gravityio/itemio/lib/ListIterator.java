package me.gravityio.itemio.lib;

import java.util.Iterator;
import java.util.List;

public class ListIterator<T> implements Iterator<T> {

    private final List<T> list;
    private final int end;
    private final int step;
    private int current;

    private ListIterator(List<T> list, int start, int end, int step, boolean reverse) {
        if (reverse) {
            int tstart = start;
            start = end;
            end = tstart;
        }

        this.list = list;
        this.current = start;
        this.end = end;
        this.step = start < end ? step : -step;
    }

    @Override
    public boolean hasNext() {
        return step > 0 ? current <= end : current >= end;
    }

    @Override
    public T next() {
        int value = current;
        current += step;
        return list.get(value);
    }

    public static <T> Iterable<T> of(List<T> list, int start, int end, int step, boolean reverse) {
        return () -> new ListIterator<>(list,start, end, step, reverse);
    }

    /**
     *
     * @param list The list
     * @param reverse Whether to iterate in reverse
     */
    public static <T> Iterable<T> of(List<T> list, boolean reverse) {
        return of(list, 0, list.size() - 1, 1, reverse);
    }
}

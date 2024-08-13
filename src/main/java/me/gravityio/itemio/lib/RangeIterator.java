package me.gravityio.itemio.lib;

import java.util.Iterator;

public class RangeIterator implements Iterator<Integer> {
    private final int end;
    private final int step;
    private int current;

    private RangeIterator(int start, int end, int step, boolean reverse) {
        if (reverse) {
            int tStart = start;
            start = end;
            end = tStart;
        }

        this.current = start;
        this.end = end;
        this.step = start < this.end ? step : -step;
    }

    @Override
    public boolean hasNext() {
        return step > 0 ? current < end : current > end;
    }

    @Override
    public Integer next() {
        int value = current;
        current += step;
        return value;
    }

    public static Iterable<Integer> of(int start, int end, int step, boolean reverse) {
        return () -> new RangeIterator(start, end, step, reverse);
    }
}

package com.bookly.util;

public class TimeUtils {

    private TimeUtils() {}

    public static <T extends Comparable<T>> boolean overlaps(T start1, T end1, T start2, T end2) {
        return start1.compareTo(end2) < 0 && end1.compareTo(start2) > 0;
    }
}

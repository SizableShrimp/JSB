package me.sizableshrimp.jsb.util;

record TimedTuple<V>(V value, long timestamp) {
    TimedTuple(V value) {
        this(value, System.currentTimeMillis());
    }
}

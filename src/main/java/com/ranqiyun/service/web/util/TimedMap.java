package com.ranqiyun.service.web.util;

import java.util.*;

public class TimedMap<K, V> {

    private Map<K, V> map = new HashMap<>();
    private Map<K, Long> timerMap = new LinkedHashMap<>();
    private long TIMEOUT_IN_MILLIS;

    public TimedMap() {
        TIMEOUT_IN_MILLIS = 10000L; // 10 秒
    }

    public TimedMap(long timeout_ms) {
        TIMEOUT_IN_MILLIS = timeout_ms;
    }

    public void addEntry(K key, V value) {
        checkTimeout();
        map.put(key, value);
        timerMap.remove(key);
        timerMap.put(key, System.currentTimeMillis());
    }

    public V getEntry(K key) {
        checkTimeout();
        return map.get(key);
    }

    private void checkTimeout() {
        List<K> removals = new LinkedList<>();
        long now = System.currentTimeMillis();

        for (K key : map.keySet()) {
            if ((now - timerMap.get(key)) > TIMEOUT_IN_MILLIS) {
                removals.add(key);
            } else {
                break;
            }
        }
        for (K removal : removals) {
            actionAfterTimeout(removal);
        }
    }

    private void actionAfterTimeout(K key) {
        //do something
        map.remove(key);
    }
}

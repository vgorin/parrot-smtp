package com.parrotsmtp.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A fixed size ordered hash map that gets
 * purged automatically on key insertions.<br/>
 * A map that automatically removes least recently used keys.<br/>
 * See <a href="http://amix.dk/blog/post/19465">http://amix.dk/blog/post/19465</a>
 *
 * @author vgorin (Vasiliy.Gorin)
 *         file created: 17.05.12 14:27
 */
public class LRUMap<K, V> extends LinkedHashMap<K, V> {
    private int max_cap;

    public LRUMap(int initial_cap, int max_cap) {
        super(initial_cap, 0.75f, true);
        this.max_cap = max_cap;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
        return size() > this.max_cap;
    }
}

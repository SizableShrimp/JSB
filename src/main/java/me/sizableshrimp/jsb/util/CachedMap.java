/*
 * Copyright (c) 2021 SizableShrimp
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.sizableshrimp.jsb.util;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A Map-like data structure that retrieves data from a {@link Function} and
 * caches it until it has been cached longer than the set expiration.
 *
 * @param <K> the type of keys
 * @param <V> the type of values
 */
public class CachedMap<K, V> {
    private final Map<K, Tuple> map = new HashMap<>();
    private Duration expiration;
    private long cachedExpiration;

    /**
     * Creates a {@link CachedMap} that defaults to expire data after 10 minutes.
     */
    public CachedMap() {
        this(Duration.ofMinutes(10));
    }

    /**
     * Creates a {@link CachedMap} that expires after the given expiration.
     *
     * @param expiration The {@link Duration} until data is expired and retrieved again.
     */
    public CachedMap(Duration expiration) {
        this.expiration = expiration;
    }

    /**
     * Get the cached value in the Map if it exists and has not passed expiration,
     * otherwise retrieve the value from the {@link Supplier}.
     *
     * @param key The key to lookup in the Map.
     * @param retrieve The {@link Function} called if the data is not cached or has expired.
     * @return The result cached or retrieved from the {@link Function}.
     */
    public V getOrRetrieve(K key, Function<K, V> retrieve) {
        Tuple data = this.map.get(key);
        if (data == null || System.currentTimeMillis() + this.cachedExpiration > data.timestamp) {
            V value = retrieve.apply(key);
            this.map.put(key, new Tuple(value));
            return value;
        }

        return data.value;
    }

    public Duration getExpiration() {
        return this.expiration;
    }

    public void setExpiration(Duration expiration) {
        this.expiration = expiration;
        this.cachedExpiration = expiration.toMillis();
    }

    private final class Tuple {
        final V value;
        final long timestamp;

        public Tuple(V value) {
            this.value = value;
            this.timestamp = System.currentTimeMillis();
        }
    }
}

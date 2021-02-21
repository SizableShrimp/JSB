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
import java.util.function.Supplier;

/**
 * A data structure that retrieves data from a {@link Supplier} and
 * caches it until it has been cached longer than the set expiration.
 *
 * @param <V> the type of the value to store
 */
public class CachedData<V> {
    private TimedTuple<V> data;
    private Duration expiration;
    private long cachedExpiration;

    /**
     * Creates a {@link CachedData} that defaults to expire data after 10 minutes.
     */
    public CachedData() {
        this(Duration.ofMinutes(10));
    }

    /**
     * Creates a {@link CachedData} that expires after the given expiration.
     *
     * @param expiration The {@link Duration} until data is expired and retrieved again.
     */
    public CachedData(Duration expiration) {
        this.expiration = expiration;
        this.cachedExpiration = expiration.toMillis();
    }

    /**
     * Get the cached value if it exists and has not passed expiration,
     * otherwise retrieve the value from the {@link Supplier}.
     *
     * @param retrieve The {@link Supplier} called if the data is not cached or has expired.
     * @return The result cached or retrieved from the {@link Supplier}.
     */
    public V getOrRetrieve(Supplier<V> retrieve) {
        if (this.data == null || this.data.timestamp() + this.cachedExpiration < System.currentTimeMillis()) {
            V value = retrieve.get();
            this.data = new TimedTuple<>(value);
            return value;
        }

        return this.data.value();
    }

    public Duration getExpiration() {
        return this.expiration;
    }

    public void setExpiration(Duration expiration) {
        this.expiration = expiration;
        this.cachedExpiration = expiration.toMillis();
    }
}

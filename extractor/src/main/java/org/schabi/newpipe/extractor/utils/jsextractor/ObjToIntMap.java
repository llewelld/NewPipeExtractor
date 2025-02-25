/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

// See https://github.com/mozilla/rhino/blob/22557a6c3ea3ad9addda5b0de01d2d720bd291d3/src/org/mozilla/javascript/ObjToIntMap.java

package org.schabi.newpipe.extractor.utils.jsextractor;

import java.io.Serializable;
import org.schabi.newpipe.extractor.utils.jsextractor.Kit;
import org.schabi.newpipe.extractor.utils.jsextractor.UniqueTag;

class ObjToIntMap implements Serializable {

    public ObjToIntMap(int keyCountHint) {
        if (keyCountHint < 0) Kit.codeBug();
        // Table grow when number of stored keys >= 3/4 of max capacity
        int minimalCapacity = keyCountHint * 4 / 3;
        int i;
        for (i = 2; (1 << i) < minimalCapacity; ++i) {}
        power = i;
    }

    /**
     * If table already contains a key that equals to keyArg, return that key while setting its
     * value to zero, otherwise add keyArg with 0 value to the table and return it.
     */
    public Object intern(Object keyArg) {
        boolean nullKey = false;
        if (keyArg == null) {
            nullKey = true;
            keyArg = UniqueTag.NULL_VALUE;
        }
        int index = ensureIndex(keyArg);
        values[index] = 0;
        return (nullKey) ? null : keys[index];
    }

    // Ensure key index creating one if necessary
    private int ensureIndex(Object key) {
        int hash = key.hashCode();
        int index = -1;
        int firstDeleted = -1;
        if (keys != null) {
            int fraction = hash * A;
            index = fraction >>> (32 - power);
            Object test = keys[index];
            if (test != null) {
                int N = 1 << power;
                if (test == key || (values[N + index] == hash && test.equals(key))) {
                    return index;
                }
                if (test == DELETED) {
                    firstDeleted = index;
                }

                // Search in table after first failed attempt
                int mask = N - 1;
                int step = tableLookupStep(fraction, mask, power);
                int n = 0;
                for (; ; ) {
                    index = (index + step) & mask;
                    test = keys[index];
                    if (test == null) {
                        break;
                    }
                    if (test == key || (values[N + index] == hash && test.equals(key))) {
                        return index;
                    }
                    if (test == DELETED && firstDeleted < 0) {
                        firstDeleted = index;
                    }
                }
            }
        }
        // Inserting of new key
        if (firstDeleted >= 0) {
            index = firstDeleted;
        } else {
            // Need to consume empty entry: check occupation level
            if (keys == null || occupiedCount * 4 >= (1 << power) * 3) {
                // Too litle unused entries: rehash
                rehashTable();
                return insertNewKey(key, hash);
            }
            ++occupiedCount;
        }
        keys[index] = key;
        values[(1 << power) + index] = hash;
        ++keyCount;
        return index;
    }

    private static int tableLookupStep(int fraction, int mask, int power) {
        int shift = 32 - 2 * power;
        if (shift >= 0) {
            return ((fraction >>> shift) & mask) | 1;
        }
        return (fraction & (mask >>> -shift)) | 1;
    }


    private void rehashTable() {
        if (keys == null) {
            int N = 1 << power;
            keys = new Object[N];
            values = new int[2 * N];
        } else {
            // Check if removing deleted entries would free enough space
            if (keyCount * 2 >= occupiedCount) {
                // Need to grow: less then half of deleted entries
                ++power;
            }
            int N = 1 << power;
            Object[] oldKeys = keys;
            int[] oldValues = values;
            int oldN = oldKeys.length;
            keys = new Object[N];
            values = new int[2 * N];

            int remaining = keyCount;
            occupiedCount = keyCount = 0;
            for (int i = 0; remaining != 0; ++i) {
                Object key = oldKeys[i];
                if (key != null && key != DELETED) {
                    int keyHash = oldValues[oldN + i];
                    int index = insertNewKey(key, keyHash);
                    values[index] = oldValues[i];
                    --remaining;
                }
            }
        }
    }

    // Insert key that is not present to table without deleted entries
    // and enough free space
    private int insertNewKey(Object key, int hash) {
        int fraction = hash * A;
        int index = fraction >>> (32 - power);
        int N = 1 << power;
        if (keys[index] != null) {
            int mask = N - 1;
            int step = tableLookupStep(fraction, mask, power);
            int firstIndex = index;
            do {
                index = (index + step) & mask;
            } while (keys[index] != null);
        }
        keys[index] = key;
        values[N + index] = hash;
        ++occupiedCount;
        ++keyCount;

        return index;
    }

    private transient Object[] keys;
    private transient int[] values;

    private int power;
    private int keyCount;
    private transient int occupiedCount; // == keyCount + deleted_count


    // A == golden_ratio * (1 << 32) = ((sqrt(5) - 1) / 2) * (1 << 32)
    // See Knuth etc.
    private static final int A = 0x9e3779b9;

    private static final Object DELETED = new Object();
}


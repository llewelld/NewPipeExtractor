/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

// See https://github.com/mozilla/rhino/blob/22557a6c3ea3ad9addda5b0de01d2d720bd291d3/src/org/mozilla/javascript/Kit.java

package org.schabi.newpipe.extractor.utils.jsextractor;

/** From org.mozilla.javascript.Kit */
class Kit {
    /**
     * If character <code>c</code> is a hexadecimal digit, return <code>accumulator</code> * 16 plus
     * corresponding number. Otherise return -1.
     */
    public static int xDigitToInt(int c, int accumulator) {
        check:
        {
            // Use 0..9 < A..Z < a..z
            if (c <= '9') {
                c -= '0';
                if (0 <= c) {
                    break check;
                }
            } else if (c <= 'F') {
                if ('A' <= c) {
                    c -= ('A' - 10);
                    break check;
                }
            } else if (c <= 'f') {
                if ('a' <= c) {
                    c -= ('a' - 10);
                    break check;
                }
            }
            return -1;
        }
        return (accumulator << 4) | c;
    }

    /** From org.mozilla.javascript.Kit */
    /**
     * Throws RuntimeException to indicate failed assertion. The function never returns and its
     * return type is RuntimeException only to be able to write <code>throw Kit.codeBug()</code> if
     * plain <code>Kit.codeBug()</code> triggers unreachable code error.
     */
    public static RuntimeException codeBug() throws RuntimeException {
        RuntimeException ex = new IllegalStateException("FAILED ASSERTION");
        // Print stack trace ASAP
        ex.printStackTrace(System.err);
        throw ex;
    }
}

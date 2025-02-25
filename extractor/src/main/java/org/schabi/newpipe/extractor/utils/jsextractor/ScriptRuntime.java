/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

// SEe https://github.com/mozilla/rhino/blob/22557a6c3ea3ad9addda5b0de01d2d720bd291d3/src/org/mozilla/javascript/ScriptRuntime.java

package org.schabi.newpipe.extractor.utils.jsextractor;

class ScriptRuntime {
    /** From org.mozilla.javascript.ScriptRuntime */
    // It is public so NativeRegExp can access it.
    public static boolean isJSLineTerminator(int c) {
        // Optimization for faster check for eol character:
        // they do not have 0xDFD0 bits set
        if ((c & 0xDFD0) != 0) {
            return false;
        }
        return c == '\n' || c == '\r' || c == 0x2028 || c == 0x2029;
    }
}

/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

// See https://github.com/mozilla/rhino/blob/22557a6c3ea3ad9addda5b0de01d2d720bd291d3/src/org/mozilla/javascript/UniqueTag.java

package org.schabi.newpipe.extractor.utils.jsextractor;

import java.io.Serializable;

public final class UniqueTag implements Serializable {
    private static final int ID_NULL_VALUE = 2;
    public static final UniqueTag NULL_VALUE = new UniqueTag(ID_NULL_VALUE);        

    private final int tagId;

    private UniqueTag(int tagId) {
        this.tagId = tagId;
    }
}



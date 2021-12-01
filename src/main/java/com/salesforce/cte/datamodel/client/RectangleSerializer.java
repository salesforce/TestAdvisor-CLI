/*
 * Copyright (c) 2021, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.cte.datamodel.client;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.github.romankh3.image.comparison.model.Rectangle;

public class RectangleSerializer extends StdSerializer<Rectangle> {
    public RectangleSerializer() {
        this(null);
    }
  
    public RectangleSerializer(Class<Rectangle> t) {
        super(t);
    }

    @Override
    public void serialize(Rectangle value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeNumberField("minX", (int)value.getMinPoint().getX());
        gen.writeNumberField("minY", (int)value.getMinPoint().getY());
        gen.writeNumberField("maxX", (int)value.getMaxPoint().getX());
        gen.writeNumberField("maxY", (int)value.getMaxPoint().getY());
        gen.writeEndObject();
    } 
}

package com.salesforce.cte.datamodel.client;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.github.romankh3.image.comparison.model.Rectangle;

public class RectangleDeserializer extends StdDeserializer<Rectangle> {

    public RectangleDeserializer() { 
        this(null); 
    } 

    public RectangleDeserializer(Class<?> vc) { 
        super(vc); 
    }

    @Override
    public Rectangle deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        int minX = (Integer) (node.get("minX")).numberValue();
        int minY = (Integer) (node.get("minY")).numberValue();
        int maxX = (Integer) (node.get("maxX")).numberValue();
        int maxY = (Integer) (node.get("maxY")).numberValue();
                
        return new Rectangle(minX,minY,maxX,maxY);
    }
    
}

package com.github.sftwnd.crayfish.examples.feign.authorization.format;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class StructuredBalanceInfoJsonDeserializer extends JsonDeserializer<StructuredBalanceInfo> {

    @Override
    public StructuredBalanceInfo deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return new ObjectMapper().reader().readValue(p,StructuredBalanceInfoImpl.class).immutable();
    }

}

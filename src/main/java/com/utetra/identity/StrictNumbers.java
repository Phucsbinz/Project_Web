package com.utetra.identity;
import java.io.IOException;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
/** Identity IDs/versions never truncate a JSON decimal or coerce a string into a number. */
public final class StrictNumbers {
    private StrictNumbers() {}
    public static class LongValue extends JsonDeserializer<Long> {
        public Long deserialize(JsonParser parser,DeserializationContext context) throws IOException {
            if(parser.currentToken()!=JsonToken.VALUE_NUMBER_INT) return (Long)context.handleUnexpectedToken(Long.class,parser);
            return parser.getLongValue();
        }
    }
    public static class IntValue extends JsonDeserializer<Integer> {
        public Integer deserialize(JsonParser parser,DeserializationContext context) throws IOException {
            if(parser.currentToken()!=JsonToken.VALUE_NUMBER_INT) return (Integer)context.handleUnexpectedToken(Integer.class,parser);
            return parser.getIntValue();
        }
    }
    public static class BooleanValue extends JsonDeserializer<Boolean> {
        public Boolean deserialize(JsonParser parser,DeserializationContext context) throws IOException {
            if(parser.currentToken()==JsonToken.VALUE_TRUE) return true;
            if(parser.currentToken()==JsonToken.VALUE_FALSE) return false;
            return (Boolean)context.handleUnexpectedToken(Boolean.class,parser);
        }
    }
}

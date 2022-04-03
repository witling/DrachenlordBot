package de.eliaspr.json;

import java.util.function.Function;
import java.util.function.Predicate;

public class JSONValue {

    private JSONValueType type;
    private JSONArray v_array;
    private boolean v_boolean;
    private float v_float;
    private long v_number;
    private JSONObject v_object;
    private String v_string;

    private JSONValue() {
    }

    public static JSONValue fromObject(JSONObject value) {
        JSONValue jv = new JSONValue();
        jv.type = JSONValueType.OBJECT;
        jv.v_object = value;
        return jv;
    }

    public static JSONValue fromArray(JSONArray value) {
        JSONValue jv = new JSONValue();
        jv.type = JSONValueType.ARRAY;
        jv.v_array = value;
        return jv;
    }

    public static JSONValue fromFloat(float value) {
        JSONValue jv = new JSONValue();
        jv.type = JSONValueType.FLOAT;
        jv.v_float = value;
        return jv;
    }

    public static JSONValue fromInteger(int value) {
        JSONValue jv = new JSONValue();
        jv.type = JSONValueType.INTEGER;
        jv.v_number = value;
        return jv;
    }

    public static JSONValue fromLong(long value) {
        JSONValue jv = new JSONValue();
        jv.type = JSONValueType.INTEGER;
        jv.v_number = value;
        return jv;
    }

    public static JSONValue fromBoolean(boolean value) {
        JSONValue jv = new JSONValue();
        jv.type = JSONValueType.BOOLEAN;
        jv.v_boolean = value;
        return jv;
    }

    public static JSONValue fromString(String value) {
        JSONValue jv = new JSONValue();
        jv.type = JSONValueType.STRING;
        jv.v_string = value;
        return jv;
    }

    public static JSONValue fromNull() {
        JSONValue jv = new JSONValue();
        jv.type = JSONValueType.NULL;
        return jv;
    }

    public static JSONValue from(Object object) {
        if (object == null)
            return fromNull();
        if (object instanceof Float)
            return fromFloat((Float) object);
        if (object instanceof Integer)
            return fromInteger((Integer) object);
        if (object instanceof Long)
            return fromLong((Long) object);
        if (object instanceof Boolean)
            return fromBoolean((Boolean) object);
        if (object instanceof JSONObject)
            return fromObject((JSONObject) object);
        if (object instanceof JSONArray)
            return fromArray((JSONArray) object);
        return fromString(object.toString());
    }

    public static Predicate<JSONValue> acceptType(JSONValueType requiredType) {
        return value -> value.type == requiredType;
    }

    public static Predicate<JSONValue> acceptAny() {
        return value -> true;
    }

    public JSONArray getArray() {
        if (type != JSONValueType.ARRAY) {
            throw new JSONException("can't convert " + type.toString() + " to ARRAY");
        }
        return v_array;
    }

    public boolean getBoolean() {
        switch (type) {
            case BOOLEAN:
                return v_boolean;
            case INTEGER:
                return v_number != 0;
        }
        throw new JSONException("can't convert " + type.toString() + " to BOOLEAN");
    }

    public float getFloat() {
        switch (type) {
            case INTEGER:
                return (float) v_number;
            case FLOAT:
                return v_float;
            case BOOLEAN:
                return v_boolean ? 1f : 0f;
            default:
                throw new JSONException("can't convert " + type.toString() + " to INTEGER");
        }
    }

    public int getInteger() {
        switch (type) {
            case INTEGER:
                return (int) v_number;
            case FLOAT:
                return (int) v_float;
            case BOOLEAN:
                return v_boolean ? 1 : 0;
            default:
                throw new JSONException("can't convert " + type.toString() + " to INTEGER");
        }
    }

    public long getLong() {
        switch (type) {
            case INTEGER:
                return v_number;
            case FLOAT:
                return (long) v_float;
            case BOOLEAN:
                return v_boolean ? 1L : 0L;
            default:
                throw new JSONException("can't convert " + type.toString() + " to LONG");
        }
    }

    public JSONObject getObject() {
        if (type != JSONValueType.OBJECT) {
            throw new JSONException("can't convert " + type.toString() + " to OBJECT");
        }
        return v_object;
    }

    public String getString() {
        if (type == JSONValueType.STRING) {
            return v_string;
        }
        throw new JSONException("can't convert" + type.toString() + " to STRING ");
    }

    public JSONValueType getType() {
        return type;
    }

    public <T> T getValueAs(JSONValueType requiredType, Function<JSONValue, T> jsonToType, T alternative) {
        return this.type == requiredType ? jsonToType.apply(this) : alternative;
    }

    @Override
    public String toString() {
        switch (type) {
            case OBJECT:
                return "JSONObject";
            case ARRAY:
                return "JSONArray";
            case INTEGER:
                return String.valueOf(v_number);
            case FLOAT:
                return String.valueOf(v_float);
            case STRING:
                return v_string;
            case BOOLEAN:
                return String.valueOf(v_boolean);
            case NULL:
                return "JSONNull";
        }
        return null;
    }

}

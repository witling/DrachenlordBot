package de.eliaspr.json;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class JSONObject implements Iterable<Map.Entry<String, JSONValue>> {

    private final Map<String, JSONValue> values;
    int lineNumber = -1;

    public JSONObject() {
        this.values = new HashMap<>();
    }

    public JSONArray getArray(String key) {
        return getValue(key).getArray();
    }

    public boolean getBoolean(String key) {
        return getValue(key).getBoolean();
    }

    public float getFloat(String key) {
        return getValue(key).getFloat();
    }

    public int getInteger(String key) {
        return getValue(key).getInteger();
    }

    public long getLong(String key) {
        return getValue(key).getLong();
    }

    public JSONObject getObject(String key) {
        return getValue(key).getObject();
    }

    public String getString(String key) {
        return getValue(key).getString();
    }

    public JSONValue getValue(String key) {
        JSONValue val = values.get(key);
        if (val == null) {
            throw new JSONException("could not find value with key '" + key + "'" + (lineNumber == -1 ? "" : " [line " + lineNumber + "]"));
        } else {
            return val;
        }
    }

    public void getValueAndExecute(String valueName, Predicate<JSONValue> validator, Consumer<JSONValue> action, Consumer<String> alternative) {
        JSONValue value = this.values.get(valueName);
        if (value != null && validator.test(value))
            action.accept(value);
        else
            alternative.accept(valueName);
    }

    public void getValueAndExecute(String valueName, Predicate<JSONValue> validator, Consumer<JSONValue> action) {
        JSONValue value = this.values.get(valueName);
        if (value != null && validator.test(value))
            action.accept(value);
    }

    public void getValueAndExecute(String valueName, Consumer<JSONValue> action, Consumer<String> alternative) {
        JSONValue value = this.values.get(valueName);
        if (value != null)
            action.accept(value);
        else
            alternative.accept(valueName);
    }

    public void getValueAndExecute(String valueName, Consumer<JSONValue> action) {
        JSONValue value = this.values.get(valueName);
        if (value != null)
            action.accept(value);
    }

    public <T> T getValueAsIfExistent(String valueName, Predicate<JSONValue> validator, Function<JSONValue, T> jsonToType, T alternative) {
        JSONValue value = this.values.get(valueName);
        if (value != null && validator.test(value))
            return jsonToType.apply(value);
        else
            return alternative;
    }

    public int getValueCount() {
        return values.size();
    }

    public JSONValueType getValueType(String key) {
        return getValue(key).getType();
    }

    public boolean hasValue(String key) {
        return values.containsKey(key);
    }

    public boolean hasValue(String key, JSONValueType type) {
        JSONValue value = values.get(key);
        return value != null && value.getType() == type;
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    @Override
    public @NotNull Iterator<Map.Entry<String, JSONValue>> iterator() {
        return values.entrySet().iterator();
    }

    public void putArray(String key, JSONArray value) {
        putValue(key, JSONValue.fromArray(value));
    }

    public void putBoolean(String key, boolean value) {
        putValue(key, JSONValue.fromBoolean(value));
    }

    public void putFloat(String key, float value) {
        putValue(key, JSONValue.fromFloat(value));
    }

    public void putInteger(String key, int value) {
        putValue(key, JSONValue.fromInteger(value));
    }

    public void putInteger(String key, long value) {
        putValue(key, JSONValue.fromLong(value));
    }

    public void putObject(String key, JSONObject value) {
        putValue(key, JSONValue.fromObject(value));
    }

    public void putString(String key, String value) {
        putValue(key, JSONValue.fromString(value));
    }

    public void putValue(String key, JSONValue value) {
        values.put(key, value);
    }

    public void removeValue(String key) {
        values.remove(key);
    }

    public int size() {
        return values.size();
    }

    public Stream<Map.Entry<String, JSONValue>> stream() {
        return values.entrySet().stream();
    }

}
package de.eliaspr.json;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class JSONArray implements Iterable<JSONValue> {

    private final JSONValue[] values;
    int lineNumber = -1;

    public JSONArray(int length) {
        values = new JSONValue[length];
    }

    public JSONArray(JSONValue... values) {
        this.values = new JSONValue[values.length];
        System.arraycopy(values, 0, this.values, 0, values.length);
    }

    public JSONArray(Object... values) {
        this.values = new JSONValue[values.length];
        for (int i = 0; i < values.length; i++)
            this.values[i] = JSONValue.from(values[i]);
    }

    public JSONArray(List<JSONValue> valueList) {
        this.values = new JSONValue[valueList.size()];
        for (int i = 0; i < valueList.size(); i++)
            this.values[i] = valueList.get(i);
    }

    public ForEachAction forEach() {
        return new ForEachAction();
    }

    public JSONArray getArray(int index) {
        return getValue(index).getArray();
    }

    public boolean getBoolean(int index) {
        return getValue(index).getBoolean();
    }

    public float getFloat(int index) {
        return getValue(index).getFloat();
    }

    public int getInteger(int index) {
        return getValue(index).getInteger();
    }

    public long getLong(int index) {
        return getValue(index).getLong();
    }

    public JSONObject getObject(int index) {
        return getValue(index).getObject();
    }

    public String getString(int index) {
        return getValue(index).getString();
    }

    public JSONValue getValue(int index) {
        if (index < 0 || index >= values.length) {
            throw new JSONException("index out of bounds (index=" + index + ", length=" + values.length + ")" + (lineNumber == -1 ? "" : " [line " + lineNumber + "]"));
        }
        return values[index];
    }

    public JSONValueType getValueType(int index) {
        return getValue(index).getType();
    }

    public boolean isEmpty() {
        return values.length == 0;
    }

    @Override
    public @NotNull Iterator<JSONValue> iterator() {
        return new Iterator<JSONValue>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < JSONArray.this.values.length;
            }

            @Override
            public JSONValue next() {
                return JSONArray.this.values[index++];
            }
        };
    }

    public int length() {
        return values.length;
    }

    public void setArray(int index, JSONArray value) {
        setValue(index, JSONValue.fromArray(value));
    }

    public void setBoolean(int index, boolean value) {
        setValue(index, JSONValue.fromBoolean(value));
    }

    public void setFloat(int index, float value) {
        setValue(index, JSONValue.fromFloat(value));
    }

    public void setInteger(int index, int value) {
        setValue(index, JSONValue.fromInteger(value));
    }

    public void setInteger(int index, long value) {
        setValue(index, JSONValue.fromLong(value));
    }

    public void setObject(int index, JSONObject value) {
        setValue(index, JSONValue.fromObject(value));
    }

    public void setString(int index, String value) {
        setValue(index, JSONValue.fromString(value));
    }

    public void setValue(int index, JSONValue value) {
        if (index < 0 || index >= values.length) {
            throw new JSONException("index out of bounds (index=" + index + ", length=" + values.length + ")" + (lineNumber == -1 ? "" : " [line " + lineNumber + "]"));
        }
        values[index] = value;
    }

    public Stream<JSONValue> stream() {
        Stream.Builder<JSONValue> streamBuilder = Stream.builder();
        for (JSONValue value : values)
            streamBuilder.add(value);
        return streamBuilder.build();
    }

    public class ForEachAction {

        private Consumer<JSONValue> action = value -> {
        };
        private Consumer<JSONValue> alternative = null;
        private int endIndex = -1;
        private int startIndex = 0;
        private Predicate<JSONValue> validator = value -> true;

        public ForEachAction action(Consumer<JSONValue> action) {
            this.action = action;
            return this;
        }

        public ForEachAction alternative(Consumer<JSONValue> alternative) {
            this.alternative = alternative;
            return this;
        }

        public ForEachAction endIndex(int endIndex) {
            this.endIndex = endIndex;
            return this;
        }

        public void run() {
            if (action == null)
                return;

            if (startIndex < 0)
                startIndex = 0;

            int len = length();
            if (endIndex < 0)
                endIndex = len;
            else if (endIndex > len)
                endIndex = len;

            if (alternative == null) {
                for (int i = startIndex; i < endIndex; i++) {
                    JSONValue element = values[i];
                    if (validator.test(element))
                        action.accept(element);
                }
            } else {
                for (int i = startIndex; i <= endIndex; i++) {
                    if (i >= length())
                        return;
                    JSONValue element = values[i];
                    if (validator.test(element))
                        action.accept(element);
                    else
                        alternative.accept(element);
                }
            }
        }

        public ForEachAction startIndex(int startIndex) {
            this.startIndex = startIndex;
            return this;
        }

        public ForEachAction validator(Predicate<JSONValue> validator) {
            this.validator = validator;
            return this;
        }

    }

}

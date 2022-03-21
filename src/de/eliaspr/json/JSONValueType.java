package de.eliaspr.json;

import java.util.function.Predicate;

public enum JSONValueType {

    OBJECT,
    ARRAY,
    INTEGER,
    FLOAT,
    STRING,
    BOOLEAN,
    NULL;

    public Predicate<JSONValue> predicate() {
        return value -> value.getType() == this;
    }

}


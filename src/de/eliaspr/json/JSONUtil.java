package de.eliaspr.json;

public class JSONUtil {

    public static String escapeString(String input) {
        return input.replace("\\", "\\\\")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\f", "\\f")
                .replace("\b", "\\b")
                .replace("\t", "\\t")
                .replace("\"", "\\\"");

    }

}

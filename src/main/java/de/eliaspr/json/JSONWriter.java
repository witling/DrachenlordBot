package de.eliaspr.json;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class JSONWriter {

    private int indents = 0;
    private boolean inlineArrays = true;
    private boolean lineBreaks = true;
    private String lineSeparator = "\n";
    private Writer output;
    private boolean saveSpaces = false;
    private int tabSize = 4;

    public JSONWriter() {
    }

    public JSONWriter enableMinifyJSON() {
        tabSize = 0;
        lineBreaks = false;
        saveSpaces = true;
        inlineArrays = true;
        return this;
    }

    private String escapeString(String str) {
        str = str.replace("\"", "\\\"");
        str = str.replace("\\", "\\\\");
        str = str.replace("\b", "\\b");
        str = str.replace("\f", "\\f");
        str = str.replace("\n", "\\n");
        str = str.replace("\r", "\\r");
        str = str.replace("\t", "\\t");

        return str;
    }

    private void lineBreak() throws IOException {
        if (lineBreaks) {
            output.write(lineSeparator);
            for (int i = 0; i < indents; i++) output.write(' ');
        }
    }

    public JSONWriter setInlineArrays(boolean inlineArrays) {
        this.inlineArrays = inlineArrays;
        return this;
    }

    public JSONWriter setLineBreaks(boolean lineBreaks) {
        this.lineBreaks = lineBreaks;
        return this;
    }

    public JSONWriter setLineSeparator(String lineSeparator) {
        this.lineSeparator = lineSeparator;
        return this;
    }

    public JSONWriter setSaveSpaces(boolean saveSpaces) {
        this.saveSpaces = saveSpaces;
        return this;
    }

    public JSONWriter setTabSize(int tabSize) {
        this.tabSize = tabSize;
        return this;
    }

    private String space() {
        return saveSpaces ? "" : " ";
    }

    private void writeArray(JSONArray array) throws IOException {
        output.write("[");
        indents += tabSize;

        JSONValueType lastType = JSONValueType.NULL;
        for (int i = 0; i < array.length(); i++) {
            JSONValue value = array.getValue(i);
            if (!inlineArrays) lineBreak();
            lastType = value.getType();
            writeValue(value);

            if (i < array.length() - 1) {
                output.write(",");
                if (inlineArrays) {
                    if (!saveSpaces) output.write(" ");
                }
            }
        }

        indents -= tabSize;
        if (lastType == JSONValueType.OBJECT || !inlineArrays) lineBreak();
        output.write("]");
    }

    public String writeJSON(JSONObject object) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            writeJSON(outputStream, object);
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            throw new JSONException("unexpected exception during writing", e);
        }
        byte[] bytes = outputStream.toByteArray();
        //noinspection StringOperationCanBeSimplified
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public void writeJSON(OutputStream output, JSONObject object) throws IOException {
        writeJSON(new OutputStreamWriter(output, StandardCharsets.UTF_8), object);
        output.flush();
    }

    public void writeJSON(File file, JSONObject object) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        writeJSON(fos, object);
        fos.close();
    }

    public void writeJSON(Writer output, JSONObject object) throws IOException {
        if (this.output != null)
            throw new JSONException("JSONWriter is already working");
        this.output = output;

        writeObject(object);
        output.flush();

        this.output = null;
    }

    private void writeObject(JSONObject object) throws IOException {
        output.write("{");
        indents += tabSize;

        boolean valueBefore = false;
        for (Map.Entry<String, JSONValue> objectEntry : object) {
            if (valueBefore) {
                output.write(",");
            }

            lineBreak();
            output.write("\"" + escapeString(objectEntry.getKey()).trim() + "\":" + space());

            JSONValue value = objectEntry.getValue();
            writeValue(value);

            valueBefore = true;
        }

        indents -= tabSize;
        lineBreak();
        output.write("}");
    }

    private void writeValue(JSONValue value) throws IOException {
        switch (value.getType()) {
            case INTEGER:
                output.write(String.valueOf(value.getLong()));
                break;
            case FLOAT:
                output.write(String.valueOf(value.getFloat()));
                break;
            case STRING:
                output.write("\"" + escapeString(value.getString()).trim() + "\"");
                break;
            case BOOLEAN:
                output.write(value.getBoolean() ? "true" : "false");
                break;
            case NULL:
                output.write("null");
                break;
            case OBJECT:
                writeObject(value.getObject());
                break;
            case ARRAY:
                writeArray(value.getArray());
                break;
        }
    }

}

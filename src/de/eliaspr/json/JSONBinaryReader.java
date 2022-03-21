package de.eliaspr.json;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class JSONBinaryReader {

    private final byte[] buffer = new byte[JSONBinaryWriter.MAX_SIZE];
    private InputStream input;

    private void error(String s) {
        throw new JSONException("invalid json binary file" + (s.length() == 0 ? "" : ": " + s));
    }

    public JSONObject readBinaryJSON(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        JSONObject obj = readBinaryJSON(fis);
        fis.close();
        return obj;
    }

    private JSONObject readBinaryJSON(InputStream input) throws IOException {
        if (this.input != null) throw new JSONException("JSONBinaryReader already working");
        this.input = input;

        byte[] header = JSONBinaryWriter.JSON_HEADER.getBytes();
        int read = input.read(buffer, 0, header.length);
        if (read < header.length) error("");
        for (int i = 0; i < header.length; i++)
            if (header[i] != buffer[i]) error("");

        if (input.read(buffer, 0, 1) == 0) error("");
        byte nextType = buffer[0];
        if (nextType != JSONBinaryWriter.JSON_TYPE_OBJECT) error("");

        JSONObject object = readObject();
        this.input = null;

        return object;
    }

    private byte readByte() throws IOException {
        if (input.read(buffer, 0, 1) == 0) error("unexpected end");
        return buffer[0];
    }

    private float readFloat() throws IOException {
        if (input.read(buffer, 0, 4) < 4) error("unexpected end");
        return ByteBuffer.wrap(buffer, 0, 4).getFloat();
    }

    private int readInt() throws IOException {
        if (input.read(buffer, 0, 4) < 4) error("unexpected end");
        return ByteBuffer.wrap(buffer, 0, 4).getInt();
    }

    private long readLong() throws IOException {
        if (input.read(buffer, 0, 8) < 8) error("unexpected end");
        return ByteBuffer.wrap(buffer, 0, 8).getLong();
    }

    private JSONObject readObject() throws IOException {
        short size = readShort();
        JSONObject object = new JSONObject();
        for (short i = 0; i < size; i++) {
            String key = readString();
            JSONValue value = readValue();
            object.putValue(key, value);
        }
        return object;
    }

    private short readShort() throws IOException {
        if (input.read(buffer, 0, 2) < 2) error("unexpected end");
        return ByteBuffer.wrap(buffer, 0, 2).getShort();
    }

    private String readString() throws IOException {
        short size = readShort();
        if (input.read(buffer, 0, size) < size) error("unexpected end");
        return new String(buffer, 0, size);
    }

    private JSONValue readValue() throws IOException {
        if (input.read(buffer, 0, 1) == 0) error("unexpected end");
        byte type = buffer[0];
        switch (type) {
            case JSONBinaryWriter.JSON_TYPE_INTEGER_1B: {
                return JSONValue.fromInteger(readByte());
            }
            case JSONBinaryWriter.JSON_TYPE_INTEGER_2B: {
                return JSONValue.fromInteger(readShort());
            }
            case JSONBinaryWriter.JSON_TYPE_INTEGER_4B: {
                return JSONValue.fromInteger(readInt());
            }
            case JSONBinaryWriter.JSON_TYPE_INTEGER_8B: {
                return JSONValue.fromLong(readLong());
            }
            case JSONBinaryWriter.JSON_TYPE_FLOAT: {
                return JSONValue.fromFloat(readFloat());
            }
            case JSONBinaryWriter.JSON_TYPE_STRING: {
                return JSONValue.fromString(readString());
            }
            case JSONBinaryWriter.JSON_TYPE_BOOLEAN: {
                char ch = (char) readByte();
                switch (ch) {
                    case '0':
                        return JSONValue.fromBoolean(false);
                    case '1':
                        return JSONValue.fromBoolean(true);
                    default:
                        error("invalid value: '" + ch + "' for boolean");
                }
            }
            case JSONBinaryWriter.JSON_TYPE_NULL: {
                return JSONValue.fromNull();
            }
            case JSONBinaryWriter.JSON_TYPE_OBJECT: {
                return JSONValue.fromObject(readObject());
            }
            case JSONBinaryWriter.JSON_TYPE_ARRAY: {
                short size = readShort();
                JSONArray array = new JSONArray(size);
                for (short i = 0; i < size; i++) {
                    JSONValue value = readValue();
                    array.setValue(i, value);
                }
                return JSONValue.fromArray(array);
            }
            default: {
                error("unknown type '" + (char) type + "'");
                return null;
            }
        }
    }

}

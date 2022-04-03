package de.eliaspr.json;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public class JSONBinaryWriter {

    public static final String JSON_HEADER = "aJbIn";
    public static final int MAX_SIZE = Short.MAX_VALUE;

    public static final byte JSON_TYPE_INTEGER_1B = (byte) ('1');
    public static final byte JSON_TYPE_INTEGER_2B = (byte) ('2');
    public static final byte JSON_TYPE_INTEGER_4B = (byte) ('4');
    public static final byte JSON_TYPE_INTEGER_8B = (byte) ('8');
    public static final byte JSON_TYPE_FLOAT = (byte) ('F');
    public static final byte JSON_TYPE_STRING = (byte) ('S');
    public static final byte JSON_TYPE_BOOLEAN = (byte) ('B');
    public static final byte JSON_TYPE_NULL = (byte) ('N');
    public static final byte JSON_TYPE_OBJECT = (byte) ('O');
    public static final byte JSON_TYPE_ARRAY = (byte) ('A');
    private final byte[] buffer = new byte[8];
    private OutputStream output;

    private void writeFloat(float val) throws IOException {
        int data = Float.floatToIntBits(val);
        writeInt(data);
    }

    private void writeInt(int val) throws IOException {
        buffer[0] = (byte) ((val >> 24) & 0xFF);
        buffer[1] = (byte) ((val >> 16) & 0xFF);
        buffer[2] = (byte) ((val >> 8) & 0xFF);
        buffer[3] = (byte) (val & 0xFF);
        output.write(buffer, 0, 4);
    }

    public void writeJSON(File file, JSONObject object) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        writeJSON(fos, object);
        fos.close();
    }

    public void writeJSON(OutputStream output, JSONObject object) throws IOException {
        if (this.output != null)
            throw new JSONException("JSONBinaryWriter is already working");
        this.output = output;

        output.write(JSON_HEADER.getBytes());
        writeObject(object);
        output.flush();

        this.output = null;
    }

    private void writeLong(long val) throws IOException {
        buffer[0] = (byte) ((val >> 56) & 0xFF);
        buffer[1] = (byte) ((val >> 48) & 0xFF);
        buffer[2] = (byte) ((val >> 40) & 0xFF);
        buffer[3] = (byte) ((val >> 32) & 0xFF);
        buffer[4] = (byte) ((val >> 24) & 0xFF);
        buffer[5] = (byte) ((val >> 16) & 0xFF);
        buffer[6] = (byte) ((val >> 8) & 0xFF);
        buffer[7] = (byte) (val & 0xFF);
        output.write(buffer, 0, 8);
    }

    private void writeObject(JSONObject object) throws IOException {
        output.write(JSON_TYPE_OBJECT);
        if (object.size() >= MAX_SIZE)
            throw new JSONException("object has too many children: " + object.size() + " > " + MAX_SIZE);
        writeShort((short) object.size());

        for (Map.Entry<String, JSONValue> objectEntry : object) {
            writeString(objectEntry.getKey());
            writeValue(objectEntry.getValue());
        }
    }

    private void writeShort(short val) throws IOException {
        buffer[0] = (byte) ((val >> 8) & 0xFF);
        buffer[1] = (byte) (val & 0xFF);
        output.write(buffer, 0, 2);
    }

    private void writeString(String val) throws IOException {
        byte[] b = val.getBytes();
        if (b.length > MAX_SIZE)
            throw new JSONException("string data too long: " + b.length + " bytes > " + MAX_SIZE + " bytes");
        writeShort((short) b.length);
        output.write(b);
    }

    private void writeValue(JSONValue value) throws IOException {
        switch (value.getType()) {
            case INTEGER: {
                long val = value.getLong();
                if ((val & 0xFFFFFF00) == 0) {
                    // number can be represented using one byte
                    output.write(JSON_TYPE_INTEGER_1B);
                    output.write((byte) val);
                } else if ((val & 0xFFFF0000) == 0) {
                    // number can be represented using two bytes
                    output.write(JSON_TYPE_INTEGER_2B);
                    writeShort((short) val);
                } else if ((val & 0xFF000000) == 0) {
                    // number can be represented using three byte
                    output.write(JSON_TYPE_INTEGER_4B);
                    writeInt((int) val);
                } else {
                    // number can be represented using four bytes
                    output.write(JSON_TYPE_INTEGER_8B);
                    writeLong(val);
                }
                break;
            }
            case FLOAT: {
                output.write(JSON_TYPE_FLOAT);
                writeFloat(value.getFloat());
                break;
            }
            case STRING: {
                output.write(JSON_TYPE_STRING);
                writeString(value.getString());
                break;
            }
            case BOOLEAN: {
                output.write(JSON_TYPE_BOOLEAN);
                output.write(value.getBoolean() ? (byte) '1' : (byte) '0');
                break;
            }
            case NULL: {
                output.write(JSON_TYPE_NULL);
                break;
            }
            case OBJECT: {
                writeObject(value.getObject());
                break;
            }
            case ARRAY: {
                JSONArray array = value.getArray();
                output.write(JSON_TYPE_ARRAY);
                if (array.length() >= MAX_SIZE)
                    throw new JSONException("array has too many elements: " + array.length() + " > " + MAX_SIZE);
                writeShort((short) array.length());
                for (int i = 0; i < array.length(); i++) {
                    JSONValue val = array.getValue(i);
                    writeValue(val);
                }
                break;
            }
        }
    }

}

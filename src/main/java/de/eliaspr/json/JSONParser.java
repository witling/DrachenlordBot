package de.eliaspr.json;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class JSONParser {

    private final char[] buf = new char[1024];
    private int bufpos = 0, buflen = 0;
    private int linenr = 0, colnr = 0;
    private Reader reader;
    private boolean streamFinished = false;

    private void error(String message, int column) {
        this.reader = null;
        throw new JSONSyntaxException(linenr + 1, column, message);
    }

    private void error(String message) {
        this.reader = null;
        throw new JSONSyntaxException(linenr + 1, colnr, message);
    }

    private char expectChar(char... options) throws IOException {
        char ch = nextChar();
        for (char l : options) {
            if (l == ch) return l;
        }

        StringBuilder expected = new StringBuilder();
        for (int i = 0; i < options.length; i++) {
            expected.append('\'').append(options[i]).append('\'');
            if (i < options.length - 1)
                expected.append(" or ");
        }

        String next;
        if (ch == '\b') {
            next = "\\b";
        } else if (ch == '\f') {
            next = "\\f";
        } else if (ch == '\n') {
            next = "\\n";
        } else if (ch == '\r') {
            next = "\\r";
        } else if (ch == '\t') {
            next = "\\t";
        } else {
            next = String.valueOf(ch);
        }

        error("expected " + expected.toString() + " but found '" + next + "'");
        return 0;
    }

    private char getChar() throws IOException {
        if (bufpos >= buflen) {
            if (streamFinished) {
                error("unexpected end of json");
            } else {
                readBuffer();
            }
        }

        return buf[bufpos];
    }

    private char nextChar() throws IOException {
        if (bufpos >= buflen) {
            if (streamFinished) {
                error("unexpected end of json");
            } else {
                readBuffer();
            }
        }

        char ch = buf[bufpos++];

        if (ch == '\n') {
            linenr++;
            colnr = 0;
        } else {
            colnr++;
        }

        return ch;
    }

    public JSONObject parseJSON(String json) {
        return parseJSON(json, false).getObject();
    }

    public JSONValue parseJSON(String json, boolean allowArray) {
        byte[] bytes = json.getBytes();
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        try {
            return parseJSON(bais, allowArray);
        } catch (IOException e) {
            this.reader = null;
            throw new JSONException("unexpected exception during parsing", e);
        }
    }

    public JSONObject parseJSON(File file) throws IOException {
        return parseJSON(file, false).getObject();
    }

    public JSONValue parseJSON(File file, boolean allowArray) throws IOException {
        try {
            return parseJSON(new FileInputStream(file), allowArray);
        } catch (FileNotFoundException e) {
            this.reader = null;
            throw e;
        }
    }

    public JSONObject parseJSON(InputStream input) throws IOException {
        return parseJSON(input, false).getObject();
    }

    public JSONValue parseJSON(InputStream input, boolean allowArray) throws IOException {
        return parseJSON(new InputStreamReader(input, StandardCharsets.UTF_8), allowArray);
    }

    public JSONObject parseJSON(Reader reader) throws IOException {
        return parseJSON(reader, false).getObject();
    }

    public JSONValue parseJSON(Reader reader, boolean allowArray) throws IOException {
        if (this.reader != null) {
            throw new JSONException("JSONParser is already working");
        }
        this.reader = reader;

        streamFinished = false;
        linenr = colnr = 0;
        JSONValue result;

        readBuffer();
        char first = getChar();
        if (first == '[') {
            if (allowArray)
                result = JSONValue.fromArray(readArray());
            else {
                expectChar('{');
                result = null;
            }
        } else if (first == '{') {
            result = JSONValue.fromObject(readObject());
        } else {
            expectChar('[', '{');
            result = null;
        }
        reader.close();

        this.reader = null;
        return result;
    }

    private JSONArray readArray() throws IOException {
        ArrayList<JSONValue> values = new ArrayList<>();
        int lineNumber = linenr + 1;

        skipSpaces();
        expectChar('[');

        if (getChar() == ']') {
            // empty array
            nextChar();
            return new JSONArray(0);
        }

        while (true) {
            skipSpaces();
            JSONValue value = readValue();
            values.add(value);

            skipSpaces();
            char ch = expectChar(']', ',');
            if (ch == ']')
                break;
        }

        JSONArray array = new JSONArray(values);
        array.lineNumber = lineNumber;
        return array;
    }

    private void readBuffer() throws IOException {
        buflen = reader.read(buf, 0, buf.length);
        if (buflen != buf.length) {
            streamFinished = true;
        }
        bufpos = 0;
    }

    private JSONObject readObject() throws IOException {
        JSONObject object = new JSONObject();
        object.lineNumber = linenr + 1;

        skipSpaces();
        expectChar('{');

        while (true) {
            skipSpaces();
            if (getChar() == '}') {
                // empty object
                nextChar();
                break;
            }
            String key = readString();

            skipSpaces();
            expectChar(':');

            skipSpaces();
            JSONValue value = readValue();
            object.putValue(key, value);

            skipSpaces();
            char ch = expectChar('}', ',');
            if (ch == '}')
                break;
        }

        return object;
    }

    private String readString() throws IOException {
        expectChar('"');

        StringBuilder key = new StringBuilder();
        char previous = 0;
        int colBefore = colnr;
        while (true) {
            char ch = nextChar();
            if (ch == '"' && previous != '\\') {
                break;
            } else {
                key.append(ch);
            }
            previous = ch;
        }

        StringBuilder result = new StringBuilder();
        char[] input = key.toString().toCharArray();
        for (int i = 0; i < input.length; i++) {
            char ch = input[i];
            if (ch == '\\') {
                if (i < input.length - 1) {
                    char next = input[i + 1];
                    switch (next) {
                        case '\"':
                            result.append('\"');
                            i++;
                            break;
                        case '\\':
                            result.append('\\');
                            i++;
                            break;
                        case '/':
                            result.append('/');
                            i++;
                            break;
                        case 'b':
                            result.append('\b');
                            i++;
                            break;
                        case 'f':
                            result.append('\f');
                            i++;
                            break;
                        case 'n':
                            result.append('\n');
                            i++;
                            break;
                        case 'r':
                            result.append('\r');
                            i++;
                            break;
                        case 't':
                            result.append('\t');
                            i++;
                            break;
                        case 'u': {
                            if (i < input.length - 5) {
                                String unicode = new String(input, i + 2, 4);
                                try {
                                    int hexcode = Integer.parseInt(unicode, 16);
                                    result.append((char) hexcode);
                                } catch (NumberFormatException e) {
                                    error("invalid unicode: '\\u" + unicode + "'", colBefore + i + 1);
                                }
                                i += 5;
                            } else {
                                result.append("\\u");
                                i++;
                            }
                            break;
                        }
                        default:
                            result.append('\\');
                            result.append(next);
                            i++;
                            break;
                    }
                } else {
                    result.append('\\');
                }
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    private String readUntilDelimiter() throws IOException {
        StringBuilder rs = new StringBuilder();
        while (true) {
            char ch = getChar();
            if (ch == ',' || ch == '}' || ch == ']' || ch == ' ' || ch == '\r' || ch == '\n') {
                break;
            } else {
                rs.append(ch);
                nextChar();
            }
        }
        return rs.toString();
    }

    private JSONValue readValue() throws IOException {
        char ch = getChar();
        int line = linenr + 1;

        if (ch == '\"') {
            String str = readString();
            return JSONValue.fromString(str)/*.setLineNumber(line)*/;
        } else if (ch == '{') {
            JSONObject obj = readObject();
            return JSONValue.fromObject(obj)/*.setLineNumber(line)*/;
        } else if (ch == '[') {
            JSONArray array = readArray();
            return JSONValue.fromArray(array)/*.setLineNumber(line)*/;
        } else if (Character.isDigit(ch) || ch == '-') {
            String nrstr = readUntilDelimiter();
            boolean isfloat = nrstr.contains(".");
            try {
                if (isfloat) {
                    float number = Float.parseFloat(nrstr);
                    return JSONValue.fromFloat(number)/*.setLineNumber(line)*/;
                } else {
                    long number = Long.parseLong(nrstr);
                    return JSONValue.fromLong(number)/*.setLineNumber(line)*/;
                }
            } catch (NumberFormatException e) {
                error("invalid number '" + nrstr + "': " + e.getMessage());
            }
        } else if (ch == ',' || ch == '}') {
            error("<value> expected but found '" + ch + "'");
        } else {
            String str = readUntilDelimiter();
            if (str.equalsIgnoreCase("true")) {
                return JSONValue.fromBoolean(true)/*.setLineNumber(line)*/;
            } else if (str.equalsIgnoreCase("false")) {
                return JSONValue.fromBoolean(false)/*.setLineNumber(line)*/;
            } else if (str.equalsIgnoreCase("null")) {
                return JSONValue.fromNull()/*.setLineNumber(line)*/;
            } else {
                error("invalid value '" + str + "'");
            }
        }

        return null;
    }

    private void skipSpaces() throws IOException {
        while (getChar() == ' ' || getChar() == '\t' || getChar() == '\r' || getChar() == '\n' || getChar() == '\f') {
            nextChar();
        }
    }

}

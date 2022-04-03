package de.eliaspr.json;

public class JSONSyntaxException extends RuntimeException {

    private final int line;
    private final int column;
    private final String message;

    public JSONSyntaxException(int line, int column, String message) {
        super("JSON syntax error in line " + line + " at column " + column + ": " + message);
        this.line = line;
        this.column = column;
        this.message = message;
    }

    public int getColumn() {
        return column;
    }

    public String getError() {
        return message;
    }

    public int getLine() {
        return line;
    }

}

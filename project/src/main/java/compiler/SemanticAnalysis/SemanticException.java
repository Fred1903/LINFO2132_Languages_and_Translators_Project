package compiler.SemanticAnalysis;

public class SemanticException extends RuntimeException {
    private final int line;
    private final int column;

    public SemanticException(String message, int line, int column, String keyword) {
        super("Semantic error : "+keyword+" at line " + line + ", column " + column + ": " + message);
        this.line = line;
        this.column = column;
    }

    public int getLine() { return line; }
    public int getColumn() { return column; }
}

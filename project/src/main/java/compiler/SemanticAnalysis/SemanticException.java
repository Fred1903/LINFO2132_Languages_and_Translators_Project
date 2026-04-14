package compiler.SemanticAnalysis;

public class SemanticException extends RuntimeException {
    private final int line;
    private final int column;
    private final String keyword;

    public SemanticException(String message, int line, int column, String keyword) {
        super("Semantic error : "+keyword+" at line " + line + ", column " + column + ": " + message);
        this.keyword = keyword;
        this.line = line;
        this.column = column;
    }
    public String getKeyword() { return keyword; }
    public int getLine() { return line; }
    public int getColumn() { return column; }
}

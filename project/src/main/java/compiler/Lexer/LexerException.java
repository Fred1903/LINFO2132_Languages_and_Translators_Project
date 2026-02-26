package compiler.Lexer;

/**
 * Exception thrown when a lexical error occurs.
 * Stores line and column information to provide precise error messages.
 */
public class LexerException extends RuntimeException{
    private final int line;
    private final int column;

    public LexerException(String message, int line, int column) {
        super("Lexer error at line " + line + ", column " + column + ": " + message);
        this.line = line;
        this.column = column;
    }

    public int getLine() { return line; }
    public int getColumn() { return column; }
}

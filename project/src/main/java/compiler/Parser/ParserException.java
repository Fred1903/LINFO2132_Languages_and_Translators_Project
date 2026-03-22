package compiler.Parser;

public class ParserException extends Exception {
    private final int line;
    private final int column;
    private final String token;

    public ParserException(String message, int line, String token) {
        super(message);
        this.line = line;
        this.column = -1;
        this.token = token;
    }

    @Override
    public String getMessage() {
        return String.format("Error at line %d%s: %s (Token: '%s')",
                line,
                column >= 0 ? ":" + column : "",
                super.getMessage(),
                token);
    }

}
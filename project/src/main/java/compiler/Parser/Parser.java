package compiler.Parser;

import compiler.Lexer.Lexer;
import compiler.Lexer.Symbol;

import java.util.HashSet;
import java.util.Set;

public class Parser {
    private final Lexer lexer;
    private Symbol lookahead;

    private final Set<String> recordTypes = new HashSet<>();

    public Parser(Lexer lexer) {
        this.lexer = lexer;
    }
    // Utility methods
    private void advance() {
        lookahead = lexer.getNextSymbol();
    }
    private void match(String expected) throws ParserException {
        if (!lookahead.getName().equals(expected)) {
            throw new ParserException(
                    "Expected '" + expected + "', found '" + lookahead.getName() + "'",
                    lexer.getLine(),
                    lookahead.getName()
            );
        }
        advance();
    }
}

package compiler.Lexer;
import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;

public class Lexer {
    public enum SymbolType {
        IDENTIFIER, COLLECTION_TYPE,KEYWORD, INTEGER, FLOAT, STRING, BOOLEAN, SYMBOL, EOF, REC, FIELD_OPERATOR, TYPE
    }

    private final PushbackReader reader;
    private int currentChar;
    private int line = 1;
    private int column = 0;
    private static final String[] BOOLEAN_VALUES = {"true", "false"};
    private final ArrayList<String> SYMBOLS = new ArrayList<>(Arrays.asList("=","+","-","*","/","%","==","=/=","<",">","<=",">=","(",")","{","}","[","]",".","&&","||", ";", ","));
    private final ArrayList<String> KEYWORDS = new ArrayList<>(Arrays.asList(
            "final", "coll", "def", "for", "while", "if", "else", "return", "not", "ARRAY"));

    private static final ArrayList<String> TYPES = new ArrayList<>(Arrays.asList(
            "INT", "FLOAT", "STRING", "BOOL"
    ));
    public Lexer(Reader input) {
        this.reader = new PushbackReader(input);
        advance();
    }
    private void advance() {
        try {
            currentChar = reader.read();
            column++;
            if (currentChar == '\n') {
                line++;
                column = 0;
            }
        } catch (IOException e) {
            currentChar = -1;
        }
    }

    private void skipWhitespace() {
        while (Character.isWhitespace(currentChar)) {
            advance();
        }
    }
    public Symbol getNextSymbol() {
        skipWhitespace();

        if (currentChar == -1) return new Symbol(SymbolType.EOF, "EOF");

        // Comments
        if (currentChar == '#') {
            while (currentChar != '\n' && currentChar != -1) advance();
            return getNextSymbol();
        }

        // Identifiers, keywords
        if (Character.isLetter(currentChar) || currentChar == '_') {
            StringBuilder sb = new StringBuilder();
            while (Character.isLetterOrDigit(currentChar) || currentChar == '_') {
                sb.append((char) currentChar);
                advance();
            }
            String word = sb.toString();

            if (KEYWORDS.contains(word)) return new Symbol(SymbolType.KEYWORD, word);
            if (TYPES.contains(word)) return new Symbol(SymbolType.TYPE, word);
            if (Arrays.asList(BOOLEAN_VALUES).contains(word)) return new Symbol(SymbolType.BOOLEAN, word);

            // Handle collections (start with uppercase)
            if (Character.isUpperCase(word.charAt(0))) {
                return new Symbol(SymbolType.COLLECTION_TYPE, word);
            }

            return new Symbol(SymbolType.IDENTIFIER, word);
        }
        return null;
    }
}

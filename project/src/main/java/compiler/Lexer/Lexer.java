package compiler.Lexer;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;

public class Lexer {
    public enum SymbolType {
        IDENTIFIER, COLLECTION_TYPE,KEYWORD, INTEGER, FLOAT, STRING, BOOLEAN, SYMBOL, EOF, REC, FIELD_OPERATOR, TYPE
    }

    private final Reader reader;
    private int currentChar;
    private int line = 1;
    private int column = 0;
    private int buffered = -2; // -2 means "nothing buffered"
    private static final String[] BOOLEAN_VALUES = {"true", "false"};
    private final ArrayList<String> KEYWORDS = new ArrayList<>(Arrays.asList(
            "final", "coll", "def", "for", "while", "if", "else", "return", "not", "ARRAY"));

    private static final ArrayList<String> TYPES = new ArrayList<>(Arrays.asList(
            "INT", "FLOAT", "STRING", "BOOL"
    ));

    /**
     * Creates a lexer that reads characters from the given Reader.
     * The constructor initializes the first character to start lexing immediately.
     */
    public Lexer(Reader input) {
        this.reader = input;
        advance();
    }

    /**
     * Reads the next character from the input stream.
     * Returns -1 if the end of file is reached or an I/O error occurs.
     */
    private int readChar() {
        try {
            return reader.read();
        } catch (IOException e) {
            return -1;
        }
    }

    /**
     * Consumes the current character and moves to the next one.
     * If a character has been pre-read using peek(), it is consumed first.
     * Also updates line and column counters for error reporting.
     */
    private void advance() {
        if (buffered != -2) {
            currentChar = buffered;
            buffered = -2;
        } else {
            currentChar = readChar();
        }
        column++;
        if (currentChar == '\n') {
            line++;
            column = 0;
        }
    }

    /**
     * Returns the next character without consuming it.
     * The character is buffered so that a subsequent advance() will read it.
     */
    private int peek() {
        if (buffered == -2) buffered = readChar();
        return buffered;
    }

    private void skipWhitespace() {
        while (Character.isWhitespace(currentChar)) {
            advance();
        }
    }


    private void lexerError(String message) {
        throw new LexerException(message, line, column);
    }

    /**
     * Returns the next token from the input stream.
     * This method handles:
     * - whitespace skipping
     * - comment removal
     * - string literals
     * - numeric literals (integers and floats)
     * - identifiers, keywords, and booleans
     * - operators and special symbols
     *
     * Returns EOF when the end of the input is reached.
     */
    public Symbol getNextSymbol() {
        skipWhitespace();

        // End of input
        if (currentChar == -1) return new Symbol(SymbolType.EOF, "EOF");

        if (currentChar == '#') {
            while (currentChar != '\n' && currentChar != -1) advance();
            return getNextSymbol();
        }
        if (currentChar == '"') return lexString();

        // A digit starts an integer or float, a dot can start a float like .234
        if (Character.isDigit(currentChar)) return lexNumber();
        if (currentChar == '.' && Character.isDigit(peek())) return lexNumber();

        if (Character.isLetter(currentChar) || currentChar == '_') return lexWord();

        // Operators and special symbols
        return lexSymbol();
    }

    /**
     * Lexes identifiers, keywords, boolean literals, and types.
     * A word is defined as a sequence of letters, digits, or underscores.
     * The method determines the token category based on reserved word tables.
     */
    private Symbol lexWord() {
        StringBuilder sb = new StringBuilder();

        while (currentChar != -1 && (Character.isLetterOrDigit(currentChar) || currentChar == '_')) {
            sb.append((char) currentChar);
            advance();
        }
        String word = sb.toString();

        if (KEYWORDS.contains(word)) return new Symbol(SymbolType.KEYWORD, word);
        if (TYPES.contains(word)) return new Symbol(SymbolType.TYPE, word);
        if (Arrays.asList(BOOLEAN_VALUES).contains(word)) return new Symbol(SymbolType.BOOLEAN, word);
        // Handle collections (start with uppercase)
        if (Character.isUpperCase(word.charAt(0))) return new Symbol(SymbolType.COLLECTION_TYPE, word);

        return new Symbol(SymbolType.IDENTIFIER, word);
    }


    /**
     * Lexes string literals enclosed in double quotes.
     * Supports escape sequences: \n, \\, and \".
     * Throws an error if the string is not properly terminated.
     */
    private Symbol lexString(){
        advance(); // consume opening '"'
        StringBuilder sb = new StringBuilder();

        while (currentChar != '"' && currentChar != -1) {
            if (currentChar == '\n') lexerError("Unterminated string literal (newline inside string)");
            if (currentChar == '\\') {
                advance(); // consume backslash
                switch (currentChar) {
                    case 'n'  -> sb.append('\n');
                    case '\\' -> sb.append('\\');
                    case '"'  -> sb.append('"');
                    default   -> lexerError("Unknown escape sequence: \\" + (char) currentChar);
                }
            }
            else sb.append((char) currentChar);
            advance();
        }
        if (currentChar == -1)lexerError("Unterminated string literal (reached end of file)");

        advance(); // consume closing '"'
        return new Symbol(SymbolType.STRING, sb.toString());
    }

    /**
     * Lexes numeric literals (integers and floats).
     * Supports:
     * - integers with optional leading zeros
     * - floats with decimal point (e.g., 3.14)
     * - floats starting with a dot (e.g., .234 interpreted as 0.234)
     *
     * The method distinguishes between float literals and the dot operator.
     */
    private Symbol lexNumber() {
        StringBuilder sb = new StringBuilder();
        boolean isFloat = false;

        // starts with . --> must be float like .234
        if (currentChar == '.') {
            advance();
            sb.append("0.");
            isFloat = true;
            while (currentChar != -1 && Character.isDigit(currentChar)) { //-1 means EOF thus currentchar should be normal digit
                sb.append((char) currentChar);
                advance();
            }
            return new Symbol(SymbolType.FLOAT, sb.toString());
        }

        // Regular integer but may become float if there is a dot
        while (currentChar != -1 && Character.isDigit(currentChar)) {
            sb.append((char) currentChar);
            advance();
        }

        // Check if this becomes a float
        // Could be float or could be integer followed by dot operator.
        // We consume the dot and peek at the next character.
        if (currentChar == '.' && Character.isDigit(peek())) {
            sb.append('.');
            isFloat = true;
            advance(); // consume '.'
            while (currentChar != -1 && Character.isDigit(currentChar)) {
                sb.append((char) currentChar);
                advance();
            }
        }

        if (isFloat) return new Symbol(SymbolType.FLOAT, sb.toString());

        // Strip leading zeros for integers: "00342" → "342", but keep "0" as "0"
        String intValue = sb.toString().replaceFirst("^0+(?!$)", "");
        return new Symbol(SymbolType.INTEGER, intValue);
    }

    /**
     * Lexes operators and special symbols, including multi-character operators.
     * Handles cases such as ==, =/=, <=, >=, &&, and ||.
     * Throws an error if an invalid symbol sequence is encountered.
     */
    private Symbol lexSymbol() {
        int c = currentChar;
        advance();

        switch (c) {
            // Unambiguous single-character symbols
            case '+' -> {
                if (currentChar == '=') { advance(); return new Symbol(SymbolType.SYMBOL, "+="); }
                return new Symbol(SymbolType.SYMBOL, "+");
            }
            // '-' or '->'
            case '-' -> {
                while (Character.isWhitespace(currentChar)) advance();
                if (currentChar == '=') { advance(); return new Symbol(SymbolType.SYMBOL, "-=");}
                if (currentChar == '>') {
                    advance();
                    return new Symbol(SymbolType.SYMBOL, "->");
                }
                return new Symbol(SymbolType.SYMBOL, "-");
            }
            case '*' -> {
                if (currentChar == '=') { advance(); return new Symbol(SymbolType.SYMBOL, "*=");}
                return new Symbol(SymbolType.SYMBOL, "*");
            }
            case '/' -> {
                if (currentChar == '=') { advance(); return new Symbol(SymbolType.SYMBOL, "/=");}
                return new Symbol(SymbolType.SYMBOL, "/");
            }
            case '%' -> { return new Symbol(SymbolType.SYMBOL, "%"); }
            case '(' -> { return new Symbol(SymbolType.SYMBOL, "("); }
            case ')' -> { return new Symbol(SymbolType.SYMBOL, ")"); }
            case '{' -> { return new Symbol(SymbolType.SYMBOL, "{"); }
            case '}' -> { return new Symbol(SymbolType.SYMBOL, "}"); }
            case '[' -> { return new Symbol(SymbolType.SYMBOL, "["); }
            case ']' -> { return new Symbol(SymbolType.SYMBOL, "]"); }
            case ';' -> { return new Symbol(SymbolType.SYMBOL, ";"); }
            case ',' -> { return new Symbol(SymbolType.SYMBOL, ","); }
            case '.' -> { return new Symbol(SymbolType.SYMBOL, "."); }

            // '=' or '==' or '=/='
            case '=' -> {
                if (currentChar == '=') {
                    advance();
                    return new Symbol(SymbolType.SYMBOL, "==");
                }
                if (currentChar == '/') {
                    advance(); // consume '/'
                    if (currentChar == '=') {
                        advance(); // consume final '='
                        return new Symbol(SymbolType.SYMBOL, "=/=");
                    }
                    lexerError("Expected '=/=' but got '=/" + (char) currentChar + "'");
                }
                return new Symbol(SymbolType.SYMBOL, "=");
            }

            // '<' or '<='
            case '<' -> {
                if (currentChar == '=') {
                    advance();
                    return new Symbol(SymbolType.SYMBOL, "<=");
                }
                return new Symbol(SymbolType.SYMBOL, "<");
            }

            // '>' or '>='
            case '>' -> {
                if (currentChar == '=') {
                    advance();
                    return new Symbol(SymbolType.SYMBOL, ">=");
                }
                return new Symbol(SymbolType.SYMBOL, ">");
            }

            // '&&'
            case '&' -> {
                if (currentChar == '&') {
                    advance();
                    return new Symbol(SymbolType.SYMBOL, "&&");
                }
                lexerError("Expected '&&' but got '&" + (char) currentChar + "'");
            }

            // '||'
            case '|' -> {
                if (currentChar == '|') {
                    advance();
                    return new Symbol(SymbolType.SYMBOL, "||");
                }
                lexerError("Expected '||' but got '|" + (char) currentChar + "'");
            }

            default -> lexerError("Unrecognized character '" + (char) c + "'");
        }

        // Unreachable as we have a case for everything, but required by Java else it will say no return value..
        throw new RuntimeException("Unreachable");
    }
    public int getLine(){
        return line;
    }
    public int getColumn(){
        return column;
    }
}

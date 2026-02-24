package compiler.Lexer;

public class Symbol {
    private final Lexer.SymbolType type;
    private final String name;

    public Symbol(Lexer.SymbolType type, String value) {
        this.type = type;
        this.name = value;
    }

    public Lexer.SymbolType getType() {
        return type;
    }

    public String getName() {
        return name;
    }
    @Override
    public String toString() {
        return "<" + type + "," + name +">";
    }
}

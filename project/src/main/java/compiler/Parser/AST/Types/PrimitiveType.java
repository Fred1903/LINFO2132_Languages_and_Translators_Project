package compiler.Parser.AST.Types;

/**
 * Les 4 types primitifs du langage : INT, FLOAT, STRING, BOOL.
 * Implementes comme enum car il n'en existe qu'un nombre fixe et connu.
 * fromString() est utilise par le parser quand il lit un token TYPE
 * pour obtenir l'enum correspondant.
 */
public enum PrimitiveType implements Type {
    INT, FLOAT, STRING, BOOL;

    public static PrimitiveType fromString(String name) {
        return switch (name) {
            case "INT"    -> INT;
            case "FLOAT"  -> FLOAT;
            case "STRING" -> STRING;
            case "BOOL"   -> BOOL;
            default -> throw new IllegalArgumentException("Unknown primitive type: " + name);
        };
    }

    @Override
    public String getName() {
        return this.name();
    }
}

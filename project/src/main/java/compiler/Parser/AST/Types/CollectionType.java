package compiler.Parser.AST.Types;

/**
 * Represente un type deéfini par l'utilisateur via coll
 * Ex : "coll Point { INT x; INT y; }" cree un CollectionType("Point")
 * Utilise comme type de variable "Point p = Point(3, 7);"
 */
public class CollectionType implements Type {
    private final String name;

    public CollectionType(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}

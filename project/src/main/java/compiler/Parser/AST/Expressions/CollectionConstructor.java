package compiler.Parser.AST.Expressions;

import compiler.Parser.AST.ASTNode;
import compiler.Parser.AST.ASTVisitor;
import java.util.List;

/**
 * Creation d'une instance de collection.
 * Ex : "Point(3, 7)" -> CollectionConstructor("Point", [Literal(3), Literal(7)])
 * Distingue de FunctionCall par la majuscule initiale du nom.
 */
public class CollectionConstructor implements ASTNode {
    private final String name;
    private final List<ASTNode> arguments;

    public CollectionConstructor(String name, List<ASTNode> arguments) {
        this.name = name;
        this.arguments = arguments;
    }

    public String getName()              { return name; }
    public List<ASTNode> getArguments() { return arguments; }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

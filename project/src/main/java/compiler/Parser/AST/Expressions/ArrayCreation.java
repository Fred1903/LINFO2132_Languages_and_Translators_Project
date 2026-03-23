package compiler.Parser.AST.Expressions;

import compiler.Parser.AST.ASTNode;
import compiler.Parser.AST.ASTVisitor;
import compiler.Parser.AST.Types.Type;
import java.util.List;

/**
 * Allocation d'un nouveau tableau avec le mot-cle ARRAY.
 * Ex : "INT ARRAY[5]" -> ArrayCreation(PrimitiveType.INT, [Literal(5)])
 *      "INT ARRAY[5][10]" -> ArrayCreation(PrimitiveType.INT, [Literal(5), Literal(10)])
 * sizes contient les expressions de taille pour chaque dimension.
 */
public class ArrayCreation implements ASTNode {
    private final Type elementType;
    private final List<ASTNode> sizes;

    public ArrayCreation(Type elementType, List<ASTNode> sizes) {
        this.elementType = elementType;
        this.sizes = sizes;
    }

    public Type getElementType()    { return elementType; }
    public List<ASTNode> getSizes() { return sizes; }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

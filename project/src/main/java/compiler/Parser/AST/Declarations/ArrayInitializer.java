package compiler.Parser.AST.Declarations;

import compiler.Parser.AST.ASTNode;
import compiler.Parser.AST.ASTVisitor;
import java.util.List;

/**
 * Initialisation d'un tableau par une liste litterale d'elements.
 * Ex : "{1, 2, 3}" -> ArrayInitializer([Literal(1), Literal(2), Literal(3)])
 * Les elements peuvent eux-memes etre des ArrayInitializer pour les tableaux 2D :
 * "{{1, 2}, {3, 4}}" -> ArrayInitializer([ArrayInitializer([1,2]), ArrayInitializer([3,4])])
 */
public class ArrayInitializer implements ASTNode {
    private final List<ASTNode> elements;

    public ArrayInitializer(List<ASTNode> elements) {
        this.elements = elements;
    }

    public List<ASTNode> getElements() { return elements; }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

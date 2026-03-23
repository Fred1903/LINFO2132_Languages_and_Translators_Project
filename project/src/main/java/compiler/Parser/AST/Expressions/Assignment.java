package compiler.Parser.AST.Expressions;

import compiler.Parser.AST.ASTNode;
import compiler.Parser.AST.ASTVisitor;

/**
 * Assignation d'une valeur a une variable existante.
 * Distinct de VariableDeclaration : ici la variable existe deja.
 * Ex : "x = 5" -> Assignment(Identifier(x), Literal(5))
 *      "a[i] = 3" -> Assignment(ArrayAccess(a, i), Literal(3))
 */
public class Assignment implements ASTNode {
    private final ASTNode left;
    private final ASTNode right;

    public Assignment(ASTNode left, ASTNode right) {
        this.left = left;
        this.right = right;
    }

    public ASTNode getLeft()  { return left; }
    public ASTNode getRight() { return right; }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

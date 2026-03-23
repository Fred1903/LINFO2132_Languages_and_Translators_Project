package compiler.Parser.AST.Expressions;

import compiler.Parser.AST.ASTNode;
import compiler.Parser.AST.ASTVisitor;

/**
 * Expression avec deux operandes et un operateur.
 * Couvre : arithmetique (+, -, *, /, %), comparaison (==, =/=, <, >, <=, >=),
 * logique (&&, ||).
 * Ex : "a + b" -> BinaryExpression(Identifier(a), "+", Identifier(b))
 */
public class BinaryExpression implements ASTNode {
    private final ASTNode left;
    private final String operator;
    private final ASTNode right;

    public BinaryExpression(ASTNode left, String operator, ASTNode right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    public ASTNode getLeft()     { return left; }
    public String getOperator()  { return operator; }
    public ASTNode getRight()    { return right; }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

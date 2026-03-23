package compiler.Parser.AST.Statements;

import compiler.Parser.AST.ASTNode;
import compiler.Parser.AST.ASTVisitor;

/**
 * Instruction return dans une fonction.
 * Ex : "return v*v;" -> ReturnStatement(BinaryExpression(v, *, v)) (ex return 3+x)..
 *      "return;" -> ReturnStatement(null)  pour les fonctions void
 */
public class ReturnStatement implements ASTNode {
    private final ASTNode value; 

    public ReturnStatement(ASTNode value) {
        this.value = value;
    }

    public ASTNode getValue() { return value; }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

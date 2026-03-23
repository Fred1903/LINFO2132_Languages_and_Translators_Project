package compiler.Parser.AST.Statements;

import compiler.Parser.AST.ASTNode;
import compiler.Parser.AST.ASTVisitor;

/**
 * Branchement conditionnel if / if-else.
 * Ex : "if (x > 0) { ... }" -> IfStatement(condition, thenBlock, null)
 *      "if (x > 0) { ... } else { ... }" -> IfStatement(condition, thenBlock, elseBlock)
 */
public class IfStatement implements ASTNode {
    private final ASTNode condition;
    private final ASTNode thenBlock;
    private final ASTNode elseBlock; // null si pas de else

    public IfStatement(ASTNode condition, ASTNode thenBlock, ASTNode elseBlock) {
        this.condition = condition;
        this.thenBlock = thenBlock;
        this.elseBlock = elseBlock;
    }

    public ASTNode getCondition() { return condition; }
    public ASTNode getThenBlock() { return thenBlock; }
    public ASTNode getElseBlock() { return elseBlock; }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

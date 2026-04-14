package compiler.Parser.AST.Expressions;

import compiler.Parser.AST.ASTNode;
import compiler.Parser.AST.ASTNodeBase;
import compiler.Parser.AST.ASTVisitor;

/**
 * Expression avec un seul operande.
 * Ex : "-x" -> UnaryExpression("-", Identifier(x))
 *      "not(b)" sera gere comme FunctionCall, pas UnaryExpression.
 */
public class UnaryExpression extends ASTNodeBase {
    private final String operator;
    private final ASTNode operand;

    public UnaryExpression(String operator, ASTNode operand) {
        this.operator = operator;
        this.operand = operand;
    }

    public String getOperator() { return operator; }
    public ASTNode getOperand() { return operand; }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

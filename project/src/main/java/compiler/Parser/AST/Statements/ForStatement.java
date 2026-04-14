package compiler.Parser.AST.Statements;

import compiler.Parser.AST.ASTNode;
import compiler.Parser.AST.ASTNodeBase;
import compiler.Parser.AST.ASTVisitor;

/**
 * For loop with variable, start point and end point, and et update expression for each increment
 * Ex : "for (INT i; 1 -> 100; i+1) { ... }"  -->  VariableDeclaration
 * Ex : "for (i; 1 -> 100; i+1) { ... }" --> variable is an Identifier (var already declared)
 *   variable : VariableDeclaration(INT, "i", false, null)  or  Identifier("i")
 *   from     : Literal("1")
 *   to       : Literal("100")
 *   update   : BinaryExpression(Identifier(i), +, Literal(1))
 *   body     : BlockStatement(...)
 */
public class ForStatement extends ASTNodeBase {
    private final ASTNode variable;
    private final ASTNode from;
    private final ASTNode to;
    private final ASTNode update;
    private final ASTNode body;

    public ForStatement(ASTNode variable, ASTNode from, ASTNode to, ASTNode update, ASTNode body) {
        this.variable = variable;
        this.from = from;
        this.to = to;
        this.update = update;
        this.body = body;
    }

    public ASTNode getVariable() { return variable; }
    public ASTNode getFrom() { return from; }
    public ASTNode getTo() { return to; }
    public ASTNode getUpdate() { return update; }
    public ASTNode getBody() { return body; }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

package compiler.Parser.AST.Statements;

import compiler.Parser.AST.ASTNode;
import compiler.Parser.AST.ASTVisitor;
import compiler.Parser.AST.Declarations.VariableDeclaration;

/**
 * Boucle for avec une variable, une plage et une expression de mise a jour.
 * Ex : "for (INT i; 1 -> 100; i+1) { ... }"
 *   variable : VariableDeclaration(INT, "i", false, null)
 *   from     : Literal("1")
 *   to       : Literal("100")
 *   update   : BinaryExpression(Identifier(i), +, Literal(1))
 *   body     : BlockStatement(...)
 */
public class ForStatement implements ASTNode {
    private final VariableDeclaration variable;
    private final ASTNode from;
    private final ASTNode to;
    private final ASTNode update;
    private final ASTNode body;

    public ForStatement(VariableDeclaration variable, ASTNode from, ASTNode to, ASTNode update, ASTNode body) {
        this.variable = variable;
        this.from = from;
        this.to = to;
        this.update = update;
        this.body = body;
    }

    public VariableDeclaration getVariable() { return variable; }
    public ASTNode getFrom()                 { return from; }
    public ASTNode getTo()                   { return to; }
    public ASTNode getUpdate()               { return update; }
    public ASTNode getBody()                 { return body; }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

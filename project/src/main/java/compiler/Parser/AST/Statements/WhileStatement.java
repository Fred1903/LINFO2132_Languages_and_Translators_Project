package compiler.Parser.AST.Statements;

import compiler.Parser.AST.ASTNode;
import compiler.Parser.AST.ASTNodeBase;
import compiler.Parser.AST.ASTVisitor;

public class WhileStatement extends ASTNodeBase {
    private final ASTNode condition;
    private final ASTNode whileBlock;
    
    public WhileStatement(ASTNode condition, ASTNode whileBlock) {
        this.condition = condition;
        this.whileBlock = whileBlock;
        
    }

    public ASTNode getCondition() { return condition; }
    public ASTNode getWhileBlock() { return whileBlock; }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

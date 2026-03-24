package compiler.Parser.AST.Expressions;

import compiler.Parser.AST.ASTNode;
import compiler.Parser.AST.ASTVisitor;

public class FieldAccess implements ASTNode {
    private final ASTNode object;
    private final String fieldName;
    
    public FieldAccess(ASTNode object, String fieldName) {
        this.object = object;
        this.fieldName = fieldName;
    }

    public ASTNode getObject() {
        return object;
    }

    public String getFieldName() {
        return fieldName;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
}

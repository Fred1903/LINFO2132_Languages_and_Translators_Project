package compiler.Parser.AST.Expressions;

import compiler.Parser.AST.ASTNodeBase;
import compiler.Parser.AST.ASTVisitor;

public class BooleanTerm extends ASTNodeBase {
    private Object value;
    private String type;
    public BooleanTerm(Object value, String type) {
        this.value = value;
        this.type = type;
    }
    public Object getValue() { return value; }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

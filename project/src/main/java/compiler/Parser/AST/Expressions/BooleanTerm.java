package compiler.Parser.AST.Expressions;

import compiler.Parser.AST.ASTNode;
import compiler.Parser.AST.ASTVisitor;

public class BooleanTerm  implements ASTNode {
    private Object value;
    private String type;
    public BooleanTerm(Object value, String type) {
        this.value = value;
        this.type = type;
    }
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

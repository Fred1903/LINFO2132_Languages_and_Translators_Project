package compiler.Parser.AST.Expressions;

import compiler.Parser.AST.ASTNodeBase;
import compiler.Parser.AST.ASTVisitor;

public class Literal extends ASTNodeBase {
    private final String value;
    private final String type;

    public Literal(String value, String type) {
        this.value = value;
        this.type = type;
    }

    public String getValue() { return value; }
    public String getType()  { return type; }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

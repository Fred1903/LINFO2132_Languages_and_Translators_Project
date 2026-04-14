package compiler.Parser.AST.Expressions;

import compiler.Parser.AST.ASTNodeBase;
import compiler.Parser.AST.ASTVisitor;

public class FloatLiteral extends ASTNodeBase {
    private final String value;
    private final String type;

    public FloatLiteral(String value, String type) {
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

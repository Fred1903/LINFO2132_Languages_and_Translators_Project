package compiler.Parser.AST.Declarations;

import compiler.Parser.AST.ASTNodeBase;
import compiler.Parser.AST.ASTVisitor;
import compiler.Parser.AST.Types.Type;

public class FunctionParameter extends ASTNodeBase {
    private final Type type;
    private final String name;
    
    public FunctionParameter(Type type, String name) {
        this.type = type;
        this.name = name;
    }

    public Type getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
}

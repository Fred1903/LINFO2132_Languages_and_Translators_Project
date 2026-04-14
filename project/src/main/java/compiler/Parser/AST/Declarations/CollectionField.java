package compiler.Parser.AST.Declarations;

import compiler.Parser.AST.Types.Type;

import compiler.Parser.AST.ASTNodeBase;
import compiler.Parser.AST.ASTVisitor;

public class CollectionField extends ASTNodeBase {
    private final Type type;
    private final String name;

    
    public CollectionField(Type type, String name) {
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

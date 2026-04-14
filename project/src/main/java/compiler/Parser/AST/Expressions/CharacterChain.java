package compiler.Parser.AST.Expressions;

import compiler.Parser.AST.ASTNodeBase;
import compiler.Parser.AST.ASTVisitor;

public class CharacterChain extends ASTNodeBase {
    private final Object chain;
    private final  String type;
    public CharacterChain(Object chain, String type) {
        this.chain = chain;
        this.type = type;
    }

    public Object getChain() { return chain; }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

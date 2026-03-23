package compiler.Parser.AST.Expressions;

import compiler.Parser.AST.ASTNode;
import compiler.Parser.AST.ASTVisitor;

public class CharacterChain implements ASTNode {
    private final Object chain;
    private final  String type;
    public CharacterChain(Object chain, String type) {
        this.chain = chain;
        this.type = type;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

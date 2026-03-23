package compiler.Parser.AST.Expressions;

import compiler.Parser.AST.ASTNode;
import compiler.Parser.AST.ASTVisitor;

/**
 * Reference a une variable par son nom.
 * Ex : dans "x + 1", le "x" devient un Identifier("x")
 */
public class Identifier implements ASTNode {
    private final String name;

    public Identifier(String name) {
        this.name = name;
    }

    public String getName() { return name; }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

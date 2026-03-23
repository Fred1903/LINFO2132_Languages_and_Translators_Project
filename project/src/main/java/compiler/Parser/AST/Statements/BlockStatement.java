package compiler.Parser.AST.Statements;

import compiler.Parser.AST.ASTNode;
import compiler.Parser.AST.ASTVisitor;
import java.util.ArrayList;

/**
 * Bloc d'instructions delimitees par des accolades.
 * Ex : "{ INT x = 5; println(x); }"
 * Utilise comme corps des fonctions, des if/else, des while et des for
 */
public class BlockStatement implements ASTNode {
    private final ArrayList<ASTNode> statements;

    public BlockStatement(ArrayList<ASTNode> statements) {
        this.statements = statements;
    }

    public ArrayList<ASTNode> getStatements() { return statements; }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

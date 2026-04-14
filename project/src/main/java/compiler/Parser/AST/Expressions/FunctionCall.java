package compiler.Parser.AST.Expressions;

import compiler.Parser.AST.ASTNode;
import compiler.Parser.AST.ASTNodeBase;
import compiler.Parser.AST.ASTVisitor;
import java.util.List;

/**
 * Appel d'une fonction definie par l'utilisateur ou built-in.
 * Ex : "square(5)" -> FunctionCall("square", [Literal(5)])
 *      "println(x)" -> FunctionCall("println", [Identifier(x)])
 * Distinct de CollectionConstructor : le nom commence par une minuscule.
 */
public class FunctionCall extends ASTNodeBase {
    private final String name;
    private final List<ASTNode> arguments;

    public FunctionCall(String name, List<ASTNode> arguments) {
        this.name = name;
        this.arguments = arguments;
    }

    public String getName()              { return name; }
    public List<ASTNode> getArguments() { return arguments; }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

package compiler.Parser.AST.Declarations;

import compiler.Parser.AST.ASTNode;
import compiler.Parser.AST.ASTVisitor;
import compiler.Parser.AST.Types.Type;

/**
 * Declaration d'une variable (avec ou sans valeur initiale).
 * Ex : "INT x = 5;" -> VariableDeclaration(INT, "x", false, Literal(5))
 *      "final FLOAT y = 3.14;" -> VariableDeclaration(FLOAT, "y", true, FloatLiteral(3.14))
 *      "INT z;" -> VariableDeclaration(INT, "z", false, null)
 * isFinal = true interdit toute reassignation future (phase 3).
 */
public class VariableDeclaration implements ASTNode {
    private final Type type;
    private final String name;
    private final boolean isFinal;
    private final ASTNode value; // peut être null

    public VariableDeclaration(Type type, String name, boolean isFinal, ASTNode value) {
        this.type = type;
        this.name = name;
        this.isFinal = isFinal;
        this.value = value;
    }

    public Type getType()     { return type; }
    public String getName()   { return name; }
    public boolean isFinal()  { return isFinal; }
    public ASTNode getValue() { return value; }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

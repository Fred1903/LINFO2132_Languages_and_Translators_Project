package compiler.Parser.AST;

import java.util.ArrayList;

/**
 * Racine de l'AST. Represente l'integralite du fichier source.
 * Contient la liste ordonnee de tous les statements de haut niveau :
 * declarations de constantes, de variables globales, de collections, de fonctions.
 */
public class ProgramNode extends ASTNodeBase {
    private final ArrayList<ASTNode> statements;

    public ProgramNode(ArrayList<ASTNode> statements) {
        this.statements = statements;
    }

    public ArrayList<ASTNode> getStatements() { return statements; }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

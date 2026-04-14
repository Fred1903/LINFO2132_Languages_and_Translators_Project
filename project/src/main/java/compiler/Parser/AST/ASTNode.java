package compiler.Parser.AST;

/**
 * Interface de base pour tous les noeuds de l'AST.
 * accept() fait partie du design pattern visiteur, elle permet
 * a ASTVisitor de traiter ce noeud sans modifier sa classe
 */
public interface ASTNode {
    <T> T accept(ASTVisitor<T> visitor);
    int getLine();
    int getColumn();
}

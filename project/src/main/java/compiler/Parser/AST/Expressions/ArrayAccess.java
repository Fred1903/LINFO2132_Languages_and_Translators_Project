package compiler.Parser.AST.Expressions;

import compiler.Parser.AST.ASTNode;
import compiler.Parser.AST.ASTNodeBase;
import compiler.Parser.AST.ASTVisitor;

/**
 * Acces a un element d'un tableau par son indice.
 * Ex : "tab[i]" -> ArrayAccess(Identifier(tab), Identifier(i))
 *      "tab[9*x]" -> ArrayAccess(Identifier(tab), BinaryExpression(9, *, x))
 * Peut etre imbrique : "matrix[i][j]" -> ArrayAccess(ArrayAccess(matrix, i), j)
 */
public class ArrayAccess extends ASTNodeBase {
    private final ASTNode array;
    private final ASTNode index;

    public ArrayAccess(ASTNode array, ASTNode index) {
        this.array = array;
        this.index = index;
    }

    public ASTNode getArray() { return array; }
    public ASTNode getIndex() { return index; }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

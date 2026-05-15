package compiler.Parser.AST.Expressions;

import compiler.Parser.AST.ASTNodeBase;
import compiler.Parser.AST.ASTVisitor;

/**
 * Reference a une variable par son nom.
 * Ex : dans "x + 1", le "x" devient un Identifier("x")
 */
public class Identifier extends ASTNodeBase {
    private final String name;
    private Object value;
    public Identifier(String name) {
        this.name = name;
    }
    public Object getConstantValue() {return value;}
    public void setConstantValue(Object value) {this.value = value;}
    public boolean hasConstantValue(){return value != null;}
    public String getName() { return name; }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

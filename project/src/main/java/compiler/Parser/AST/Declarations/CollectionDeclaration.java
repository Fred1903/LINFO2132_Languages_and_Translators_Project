package compiler.Parser.AST.Declarations;

import java.util.List;

import compiler.Parser.AST.ASTNode;
import compiler.Parser.AST.ASTVisitor;

public class CollectionDeclaration implements ASTNode{
    private final String name;
    private final List<CollectionField> fields;

    

    public CollectionDeclaration(String name, List<CollectionField> fields) {
        this.name = name;
        this.fields = fields;
    }



    public String getName() {
        return name;
    }



    public List<CollectionField> getFields() {
        return fields;
    }



    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
}

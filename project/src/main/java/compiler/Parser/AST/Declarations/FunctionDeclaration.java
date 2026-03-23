package compiler.Parser.AST.Declarations;

import java.util.List;

import compiler.Parser.AST.ASTNode;
import compiler.Parser.AST.ASTVisitor;
import compiler.Parser.AST.Types.Type;

public class FunctionDeclaration implements ASTNode{
    private final Type returnType;
    private final String name;
    private final List<FunctionParameter> parameters;
    private final ASTNode body; //block/body of function

    
    public FunctionDeclaration(Type returnType, String name, List<FunctionParameter> parameters, ASTNode body) {
        this.returnType = returnType;
        this.name = name;
        this.parameters = parameters;
        this.body = body;
    }


    public Type getReturnType() {
        return returnType;
    }


    public String getName() {
        return name;
    }


    public List<FunctionParameter> getParameters() {
        return parameters;
    }


    public ASTNode getBody() {
        return body;
    }


    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }


}

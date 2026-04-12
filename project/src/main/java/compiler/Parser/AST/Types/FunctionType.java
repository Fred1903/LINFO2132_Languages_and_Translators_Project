package compiler.Parser.AST.Types;

import java.util.List;

public class FunctionType implements Type{
    private final Type returnType;
    private final List<Type> parametersType;

    
    
    public FunctionType(Type returnType, List<Type> parametersType) {
        this.returnType = returnType;
        this.parametersType = parametersType;
    }
    


    public Type getReturnType() {
        return returnType;
    }
    public List<Type> getParametersType() {
        return parametersType;
    }


    @Override
    public String getName() {
        return "FUNCTION";
    }
}

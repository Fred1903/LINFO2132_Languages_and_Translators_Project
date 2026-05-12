package compiler.Parser.AST;

import compiler.Parser.AST.Types.Type;

public abstract class ASTNodeBase implements ASTNode {
    private int line;
    private int column;
    private Type resolvedType; 

    public void setResolvedType(Type type) { 
        this.resolvedType = type; 
    }

    public Type getResolvedType() {
        return resolvedType; 
    }
    
    public void setLine(int line) { 
        this.line = line; 
    }
    
    public void setColumn(int column) { 
        this.column = column; 
    }
    
    @Override
    public int getLine() { 
        return line; 
    }

    @Override
    public int getColumn() { 
        return column;
    }
}

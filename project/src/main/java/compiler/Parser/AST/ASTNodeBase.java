package compiler.Parser.AST;

public abstract class ASTNodeBase implements ASTNode {
    private int line;
    private int column;
    
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

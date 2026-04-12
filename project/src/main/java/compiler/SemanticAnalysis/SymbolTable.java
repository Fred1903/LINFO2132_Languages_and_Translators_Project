package compiler.SemanticAnalysis;

import java.util.HashMap;

import compiler.Parser.AST.Types.Type;

public class SymbolTable {
    SymbolTable previousTable; // link to previous table
    HashMap<String,Type> entries;

    SymbolTable(SymbolTable previousTable) { 
        this.previousTable = previousTable; 
        entries = new HashMap<>();
    }

    void putToScope(String name, Type type){
        if(entries.containsKey(name)){
            throw new SemanticException(name+" : already declared in this scope", 0, 0, "ScopeError");
        }
        entries.put(name, type);
    }

    Type getType(String name){
        if(entries.containsKey(name))return entries.get(name);
        if(previousTable!=null) return previousTable.getType(name);
        throw new SemanticException("Undeclared identifier "+name, 0, 0, "ScopeError");
    }
}

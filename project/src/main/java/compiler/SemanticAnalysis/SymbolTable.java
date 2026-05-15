package compiler.SemanticAnalysis;

import java.util.HashMap;

import compiler.Parser.AST.Types.Type;

public class SymbolTable {
    static class SymbolEntry {
        Type type;
        boolean isFinal;
        boolean isUsed = false;
        int line;
        Object value;
        SymbolEntry(Type type, boolean isFinal,int line,Object value) {
            this.type = type;
            this.isFinal = isFinal;
            this.line = line;
            this.value = value;
        }
    }
    SymbolTable previousTable; // link to previous table
    HashMap<String,SymbolEntry> entries;

    SymbolTable(SymbolTable previousTable) { 
        this.previousTable = previousTable; 
        entries = new HashMap<>();
    }

    void putToScope(String name, Type type, boolean isFinal, int line, Object value) {
        if(entries.containsKey(name)){
            throw new SemanticException(name+" : already declared in this scope", line, 0, "ScopeError");
        }
        entries.put(name, new SymbolEntry(type, isFinal, line, value));
    }

    void putToScope(String name, Type type, boolean isFinal, int line) {
        putToScope(name, type, isFinal, line, null);
    }

    void putToScope(String name, Type type) {
        putToScope(name, type, false, 0, null);
    }

    SymbolEntry getEntry(String name) {
        if(entries.containsKey(name)) return entries.get(name);
        if(previousTable != null) return previousTable.getEntry(name);
        return null;
    }

    Type getType(String name){
        SymbolEntry entry = getEntry(name);
        if(entry != null) return entry.type;
        throw new SemanticException("Undeclared identifier " + name, 0, 0, "ScopeError");
    }
    public void markAsUsed(String name) {
        SymbolEntry entry = entries.get(name);
        if (entry != null) {
            entry.isUsed = true;
        } else if (previousTable != null) {
            previousTable.markAsUsed(name);
        }
    }
}

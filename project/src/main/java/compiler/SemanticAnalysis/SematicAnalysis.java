package compiler.SemanticAnalysis;

import java.util.HashMap;
import java.util.List;

import compiler.Parser.AST.ASTVisitor;
import compiler.Parser.AST.ProgramNode;
import compiler.Parser.AST.Declarations.ArrayInitializer;
import compiler.Parser.AST.Declarations.CollectionDeclaration;
import compiler.Parser.AST.Declarations.CollectionField;
import compiler.Parser.AST.Declarations.FunctionDeclaration;
import compiler.Parser.AST.Declarations.FunctionParameter;
import compiler.Parser.AST.Declarations.VariableDeclaration;
import compiler.Parser.AST.Expressions.ArrayAccess;
import compiler.Parser.AST.Expressions.ArrayCreation;
import compiler.Parser.AST.Expressions.Assignment;
import compiler.Parser.AST.Expressions.BinaryExpression;
import compiler.Parser.AST.Expressions.BooleanTerm;
import compiler.Parser.AST.Expressions.CharacterChain;
import compiler.Parser.AST.Expressions.CollectionConstructor;
import compiler.Parser.AST.Expressions.FieldAccess;
import compiler.Parser.AST.Expressions.FloatLiteral;
import compiler.Parser.AST.Expressions.FunctionCall;
import compiler.Parser.AST.Expressions.Identifier;
import compiler.Parser.AST.Expressions.Literal;
import compiler.Parser.AST.Expressions.UnaryExpression;
import compiler.Parser.AST.Statements.BlockStatement;
import compiler.Parser.AST.Statements.ForStatement;
import compiler.Parser.AST.Statements.IfStatement;
import compiler.Parser.AST.Statements.ReturnStatement;
import compiler.Parser.AST.Statements.WhileStatement;
import compiler.Parser.AST.Types.FunctionType;
import compiler.Parser.AST.Types.PrimitiveType;
import compiler.Parser.AST.Types.Type;

public class SematicAnalysis implements ASTVisitor<Type>{
    private SymbolTable currentSymbolTable;
    private Type currentReturnType = null;
    private final HashMap<String, List<CollectionField>> collectionFields = new HashMap<>();


    public SematicAnalysis() {
        currentSymbolTable = new SymbolTable(null);
    }


    void analyze(ProgramNode root){
        // Add built-in functions 
        currentSymbolTable.putToScope("not", new FunctionType(PrimitiveType.BOOL, List.of(PrimitiveType.BOOL)));
        currentSymbolTable.putToScope("str", new FunctionType(PrimitiveType.STRING, List.of(PrimitiveType.INT)));
        currentSymbolTable.putToScope("length", new FunctionType(PrimitiveType.INT, List.of()));
        currentSymbolTable.putToScope("floor", new FunctionType(PrimitiveType.INT, List.of(PrimitiveType.FLOAT)));
        currentSymbolTable.putToScope("ceil", new FunctionType(PrimitiveType.INT, List.of(PrimitiveType.FLOAT)));
        currentSymbolTable.putToScope("read_INT", new FunctionType(PrimitiveType.INT, List.of()));
        currentSymbolTable.putToScope("read_FLOAT", new FunctionType(PrimitiveType.FLOAT, List.of()));
        currentSymbolTable.putToScope("read_STRING", new FunctionType(PrimitiveType.STRING, List.of()));
        currentSymbolTable.putToScope("print_INT",   new FunctionType(null, List.of(PrimitiveType.INT)));
        currentSymbolTable.putToScope("print_FLOAT", new FunctionType(null, List.of(PrimitiveType.FLOAT)));
        currentSymbolTable.putToScope("print",   new FunctionType(null, List.of()));
        currentSymbolTable.putToScope("println", new FunctionType(null, List.of()));
        root.accept(this);
    }


    @Override
    public Type visit(Assignment node) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public Type visit(BinaryExpression node) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public Type visit(UnaryExpression node) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public Type visit(Literal node) {
        return PrimitiveType.INT;
    }

    @Override
    public Type visit(FloatLiteral node) {
        return PrimitiveType.FLOAT;
    }

    @Override
    public Type visit(BooleanTerm node) {
        return PrimitiveType.BOOL;
    }

    @Override
    public Type visit(CharacterChain node) {
        return PrimitiveType.STRING;
    }

    @Override
    public Type visit(Identifier node) {
        return currentSymbolTable.getType(node.getName());
    }

    @Override
    public Type visit(FunctionCall node) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public Type visit(CollectionConstructor node) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public Type visit(ArrayAccess node) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public Type visit(ArrayCreation node) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public Type visit(FieldAccess node) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public Type visit(VariableDeclaration node) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public Type visit(ArrayInitializer node) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public Type visit(CollectionField node) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public Type visit(CollectionDeclaration node) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public Type visit(FunctionParameter node) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public Type visit(FunctionDeclaration node) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public Type visit(BlockStatement node) {
        for(var statement: node.getStatements()){
            statement.accept(this);
        }
        return null;
    }

    @Override
    public Type visit(IfStatement node) {
        if(node.getCondition().accept(this) != PrimitiveType.BOOL){
            throw new SemanticException("if condition must be BOOL", 0, 0, ";MissingConditionError");
        }
        node.getThenBlock().accept(this);
        if(node.getElseBlock()!=null)node.getElseBlock().accept(this);
        return null;
    }

    @Override
    public Type visit(WhileStatement node) {
        if (node.getCondition().accept(this) != PrimitiveType.BOOL){
            throw new SemanticException("While condition must be BOOL", 0, 0, "MissingConditionError");
        }
        node.getWhileBlock().accept(this);
        return null;
    }

    @Override
    public Type visit(ForStatement node) {
        node.getVariable().accept(this);
        if (node.getFrom().accept(this) != PrimitiveType.INT || node.getTo().accept(this) != PrimitiveType.INT){
            throw new SemanticException("for range must be INT", 0, 0, "TypeError");
        }
        node.getUpdate().accept(this);
        node.getBody().accept(this);
        return null;
    }

    @Override
    public Type visit(ReturnStatement node) {
        Type returnType; 
        if (node.getValue() != null) {
            returnType = node.getValue().accept(this);
        } else {
            returnType = null; //=void
        }
        if (!isCompatible(currentReturnType, returnType)){
            throw new SemanticException("Expected " + currentReturnType + " but got " + returnType, 0, 0, "ReturnError");
        }
        return returnType;
    }

    @Override
    public Type visit(ProgramNode node) {
        for(var statement: node.getStatements()){
            statement.accept(this);
        }
        return null;
    }

    private boolean isCompatible(Type expectedReturnType, Type returnType) {
        if (expectedReturnType==null && returnType==null)return true;
        if (expectedReturnType==null || returnType==null)return false;
        if (expectedReturnType==returnType)return true;
        if (expectedReturnType==PrimitiveType.FLOAT && returnType==PrimitiveType.INT)return true;
        return expectedReturnType.getName().equals(returnType.getName());
    }

    

    
}

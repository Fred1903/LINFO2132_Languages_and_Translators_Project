package compiler.Parser;

import compiler.Parser.AST.ASTVisitor;
import compiler.Parser.AST.ProgramNode;
import compiler.Parser.AST.Declarations.*;
import compiler.Parser.AST.Expressions.*;
import compiler.Parser.AST.Statements.*;

public class PrintVisitor implements ASTVisitor<Void> {
    private int indentCounter = 0;

    private void print(String text) {
        System.out.println("    ".repeat(indentCounter) + text);
    }


    @Override
    public Void visit(ProgramNode node) {
        print("Program");
        indentCounter++;
        for (var statement : node.getStatements())statement.accept(this);
        indentCounter--;
        return null;
    }

    ////////// Declarations ////////// 

    @Override
    public Void visit(VariableDeclaration node) {
        String prefix = node.isFinal() ? "FinalDeclaration" : "VariableDeclaration";
        print(prefix + ": " + node.getName() + " (" + node.getType().getName() + ")");
        if (node.getValue() != null) {
            indentCounter++;
            node.getValue().accept(this);
            indentCounter--;
        }
        return null;
    }

    @Override
    public Void visit(ArrayInitializer node) {
        print("ArrayInitializer");
        indentCounter++;
        for (var element : node.getElements()) element.accept(this);
        indentCounter--;
        return null;
    }

    @Override
    public Void visit(CollectionDeclaration node) {
        print("CollectionDeclaration: " + node.getName());
        indentCounter++;
        for (var field : node.getFields()) field.accept(this);
        indentCounter--;
        return null;
    }

    @Override
    public Void visit(CollectionField node) {
        print("Field: " + node.getName() + " (" + node.getType().getName() + ")");
        return null;
    }

    @Override
    public Void visit(FunctionDeclaration node) {
        String returnType = node.getReturnType() != null ? node.getReturnType().getName() : "void";
        print("FunctionDeclaration: " + node.getName() + " -> " + returnType);
        indentCounter++;
        for (var parameter : node.getParameters()) parameter.accept(this);
        node.getBody().accept(this);
        indentCounter--;
        return null;
    }

    @Override
    public Void visit(FunctionParameter node) {
        print("Parameter: " + node.getName() + " (" + node.getType().getName() + ")");
        return null;
    }

    ////////// Statements //////////
    

    @Override
    public Void visit(BlockStatement node) {
        print("Block");
        indentCounter++;
        for (var statement : node.getStatements()) statement.accept(this);
        indentCounter--;
        return null;
    }

    @Override
    public Void visit(IfStatement node) {
        print("IfStatement");
        indentCounter++;
        print("Condition:");
        indentCounter++;
        node.getCondition().accept(this);
        indentCounter--;
        print("Then:");
        indentCounter++;
        node.getThenBlock().accept(this);
        indentCounter--;
        if (node.getElseBlock() != null) {
            print("Else:");
            indentCounter++;
            node.getElseBlock().accept(this);
            indentCounter--;
        }
        indentCounter--;
        return null;
    }

    @Override
    public Void visit(WhileStatement node) {
        print("WhileStatement");
        indentCounter++;
        print("Condition:");
        indentCounter++;
        node.getCondition().accept(this);
        indentCounter--;
        print("Body:");
        indentCounter++;
        node.getWhileBlock().accept(this);
        indentCounter--;
        indentCounter--;
        return null;
    }

    @Override
    public Void visit(ForStatement node) {
        print("ForStatement");
        indentCounter++;
        print("Variable:");
        indentCounter++;
        node.getVariable().accept(this);
        indentCounter--;
        print("From:");
        indentCounter++;
        node.getFrom().accept(this);
        indentCounter--;
        print("To:");
        indentCounter++;
        node.getTo().accept(this);
        indentCounter--;
        print("Update:");
        indentCounter++;
        node.getUpdate().accept(this);
        indentCounter--;
        print("Body:");
        indentCounter++;
        node.getBody().accept(this);
        indentCounter--;
        indentCounter--;
        return null;
    }

    @Override
    public Void visit(ReturnStatement node) {
        print("ReturnStatement");
        if (node.getValue() != null) {
            indentCounter++;
            node.getValue().accept(this);
            indentCounter--;
        }
        return null;
    }

    ////////// Expressions ////////// 

    @Override
    public Void visit(Assignment node) {
        print("Assignment");
        indentCounter++;
        node.getLeft().accept(this);
        node.getRight().accept(this);
        indentCounter--;
        return null;
    }

    @Override
    public Void visit(BinaryExpression node) {
        print("BinaryExpression: " + node.getOperator());
        indentCounter++;
        node.getLeft().accept(this);
        node.getRight().accept(this);
        indentCounter--;
        return null;
    }

    @Override
    public Void visit(UnaryExpression node) {
        print("UnaryExpression: " + node.getOperator());
        indentCounter++;
        node.getOperand().accept(this);
        indentCounter--;
        return null;
    }

    @Override
    public Void visit(Literal node) {
        print("Literal: " + node.getValue());
        return null;
    }

    @Override
    public Void visit(FloatLiteral node) {
        print("FloatLiteral: " + node.getValue());
        return null;
    }

    @Override
    public Void visit(BooleanTerm node) {
        print("BooleanTerm: " + node.getValue());
        return null;
    }

    @Override
    public Void visit(CharacterChain node) {
        print("String: \"" + node.getChain() + "\"");
        return null;
    }

    @Override
    public Void visit(Identifier node) {
        print("Identifier: " + node.getName());
        return null;
    }

    @Override
    public Void visit(FunctionCall node) {
        print("FunctionCall: " + node.getName());
        indentCounter++;
        for (var arg : node.getArguments()) arg.accept(this);
        indentCounter--;
        return null;
    }

    @Override
    public Void visit(CollectionConstructor node) {
        print("CollectionConstructor: " + node.getName());
        indentCounter++;
        for (var argument : node.getArguments()) argument.accept(this);
        indentCounter--;
        return null;
    }

    @Override
    public Void visit(ArrayAccess node) {
        print("ArrayAccess");
        indentCounter++;
        node.getArray().accept(this);
        print("Index:");
        indentCounter++;
        node.getIndex().accept(this);
        indentCounter--;
        indentCounter--;
        return null;
    }

    @Override
    public Void visit(ArrayCreation node) {
        print("ArrayCreation: " + node.getElementType().getName());
        indentCounter++;
        for (var size : node.getSizes()) size.accept(this);
        indentCounter--;
        return null;
    }


    @Override
    public Void visit(FieldAccess node) {
        print("FieldAccess: ."+node.getFieldName());
        indentCounter++;
        node.getObject().accept(this); 
        indentCounter--;
        return null;
    }
}

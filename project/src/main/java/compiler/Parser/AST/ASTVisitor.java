package compiler.Parser.AST;

import compiler.Parser.AST.Declarations.ArrayInitializer;
import compiler.Parser.AST.Declarations.CollectionDeclaration;
import compiler.Parser.AST.Declarations.CollectionField;
import compiler.Parser.AST.Declarations.FunctionDeclaration;
import compiler.Parser.AST.Declarations.FunctionParameter;
import compiler.Parser.AST.Declarations.VariableDeclaration;
import compiler.Parser.AST.Expressions.*;
import compiler.Parser.AST.Statements.BlockStatement;
import compiler.Parser.AST.Statements.ForStatement;
import compiler.Parser.AST.Statements.IfStatement;
import compiler.Parser.AST.Statements.ReturnStatement;
import compiler.Parser.AST.Statements.WhileStatement;

/*
  Design pattern visiteur pour AST
  Chaque visit() correspond a un type de noeud different retournant T 
*/
public interface ASTVisitor<T> {
    // Expressions
    T visit(Assignment node);
    T visit(BinaryExpression node);
    T visit(UnaryExpression node);
    T visit(Literal node);
    T visit(FloatLiteral node);
    T visit(BooleanTerm node);
    T visit(CharacterChain node);
    T visit(Identifier node);
    T visit(FunctionCall node);
    T visit(CollectionConstructor node);
    T visit(ArrayAccess node);
    T visit(ArrayCreation node);

    // declarations
    T visit(VariableDeclaration node);
    T visit(ArrayInitializer node);
    T visit(CollectionField node);
    T visit(CollectionDeclaration node);
    T visit(FunctionParameter node);
    T visit(FunctionDeclaration node);

    // statements
    T visit(BlockStatement node);
    T visit(IfStatement node);
    T visit(WhileStatement node);
    T visit(ForStatement node);
    T visit(ReturnStatement node);

    // program
    T visit(ProgramNode node);
}

package compiler.SemanticAnalysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import compiler.Parser.AST.ASTNode;
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
import compiler.Parser.AST.Types.ArrayType;
import compiler.Parser.AST.Types.CollectionType;
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
        Type leftType  = node.getLeft().accept(this);
        Type rightType = node.getRight().accept(this);
        if (!isCompatible(leftType, rightType)){
            throw new SemanticException("Cannot assign " + rightType.getName() + " to " + leftType.getName()+", must be same type", node.getLine(), node.getColumn(), "TypeError");
        }
        return leftType;
    }

    @Override
    public Type visit(BinaryExpression node) {
        Type leftType  = node.getLeft().accept(this);
        Type rightType = node.getRight().accept(this);
        String operator  = node.getOperator();

        switch (operator) {
            case "+" -> {
                if (leftType == PrimitiveType.STRING && rightType == PrimitiveType.STRING) {
                    return PrimitiveType.STRING;
                }
                if (isNumeric(leftType) && isNumeric(rightType)) {
                    if (leftType == PrimitiveType.FLOAT || rightType == PrimitiveType.FLOAT) return PrimitiveType.FLOAT;
                    return PrimitiveType.INT;
                }
                throw new SemanticException("Operator '+' requires numeric types or two STRINGS", 0, 0, "OperatorError");
            }
            case "-", "*", "/" -> {
                if (!isNumeric(leftType) || !isNumeric(rightType)) {
                    throw new SemanticException("Operator '" + operator + "' requires numeric types", 0, 0, "OperatorError");
                }
                if (leftType == PrimitiveType.FLOAT || rightType == PrimitiveType.FLOAT) return PrimitiveType.FLOAT;
                return PrimitiveType.INT;

            }
            case "%" -> {
                if (leftType!=PrimitiveType.INT || rightType!=PrimitiveType.INT){
                    throw new SemanticException("Operator '%' requires INT types", node.getLine(), node.getColumn(), "OperatorError");
                }
                return PrimitiveType.INT;
            }
            case "==", "=/=" -> {
                if (!isCompatible(leftType, rightType)){
                    throw new SemanticException("Operator '" + operator + "' requires same types", node.getLine(), node.getColumn(), "OperatorError");
                }
                return PrimitiveType.BOOL;
            }
            case "<", ">", "<=", ">=" -> {
                if (!isNumeric(leftType) || !isNumeric(rightType))
                    throw new SemanticException("Operator '" + operator + "' requires numeric types", node.getLine(), node.getColumn(), "OperatorError");
                return PrimitiveType.BOOL;
            }
            case "&&", "||" -> {
                if (leftType!=PrimitiveType.BOOL || rightType!=PrimitiveType.BOOL)
                    throw new SemanticException("Operator '" + operator + "' requires BOOL types", node.getLine(), node.getColumn(), "OperatorError");
                return PrimitiveType.BOOL;
            }
            default -> throw new SemanticException("Unknown operator '" + operator + "'", node.getLine(), node.getColumn(), "OperatorError");
        }
    }

    @Override
    public Type visit(UnaryExpression node) {
        Type operand = node.getOperand().accept(this);
        String operator = node.getOperator();
        if (operator.equals("not")) {
            if (operand != PrimitiveType.BOOL) {
                throw new SemanticException("Operator '" + operator + "' requires a BOOL, but got " + operand, 0, 0, "TypeError");
            }
            return PrimitiveType.BOOL;
        }
        // -
        if (operator.equals("-")) {
            if (!isNumeric(operand)) {
                throw new SemanticException("Unary expression '-' requires numeric type", 0, 0, "OperatorError");
            }
            return operand;
        }
        if (operator.equals("++") || operator.equals("--")) {
            if (operand != PrimitiveType.INT) {
                throw new SemanticException("Operator '" + operator + " ' requires an INT", 0, 0, "OperatorError");
            }
            return PrimitiveType.INT;
        }

        throw new SemanticException("Unknown unary operator '" + operator + "'", 0, 0, "OperatorError");
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
        String name = node.getName();

        // Special cases accepting/returning multiple types
        if (name.equals("print") || name.equals("println")) {
            for (var argument : node.getArguments()) argument.accept(this);
            return null;
        }
        if (name.equals("length")) {
            if (node.getArguments().size() != 1)throw new SemanticException("length() expects one argument", 0, 0, "ArgumentError");
            Type argumentType = node.getArguments().get(0).accept(this);
            if (argumentType!=PrimitiveType.STRING && !(argumentType instanceof ArrayType)){
                throw new SemanticException("length() expects STRING or ARRAY type", 0, 0, "ArgumentError");
            }
            return PrimitiveType.INT;
        }

        //normal case :
        Type functionType = currentSymbolTable.getType(name);
        if (!(functionType instanceof FunctionType))throw new SemanticException(name + " is not a function", 0, 0, "ScopeError");
        FunctionType castedFunctionType = (FunctionType) functionType;
        List<ASTNode> arguments = node.getArguments();
        List<Type> parameterTypes = castedFunctionType.getParametersType();
        if (arguments.size() != parameterTypes.size()){
            throw new SemanticException("Wrong number of arguments for " + name, 0, 0, "ArgumentError");
        }
        for (int i = 0; i < arguments.size(); i++) {
            Type argumentType = arguments.get(i).accept(this);
            if (!isCompatible(parameterTypes.get(i), argumentType)){
                throw new SemanticException("Argument " + i + " of " + name + ": expected " + parameterTypes.get(i).getName() + " but received argument is : " + argumentType.getName(), 0, 0, "ArgumentError");
            }
                
        }
        return castedFunctionType.getReturnType();
    }

    @Override
    public Type visit(CollectionConstructor node) {
        String name = node.getName();
        List<CollectionField> fields = collectionFields.get(name);
        if (fields == null)throw new SemanticException("Unknown collection type: " + name, 0, 0, "CollectionError");
        if (node.getArguments().size() != fields.size())throw new SemanticException("Incorrect number of arguments for " + name, 0, 0, "ArgumentError");
        for (int i = 0; i < fields.size(); i++) {
            Type argumentType = node.getArguments().get(i).accept(this);
            if (!isCompatible(fields.get(i).getType(), argumentType)){
                throw new SemanticException("Field " + fields.get(i).getName() + ": expected " + fields.get(i).getType().getName() + " but received " + argumentType.getName(), 0, 0, "ArgumentError");
            }
        }        
        return new CollectionType(name);
    }

    @Override
    public Type visit(ArrayAccess node) {
        Type arrayType = node.getArray().accept(this);
        Type indexType = node.getIndex().accept(this);
        if (indexType!=PrimitiveType.INT)throw new SemanticException("Array index must be INT", 0, 0, "TypeError");
        if (arrayType instanceof ArrayType) {
            ArrayType castedArrayType = (ArrayType) arrayType;
            if (castedArrayType.getDimensions() > 1) return new ArrayType(castedArrayType.getElementType(), castedArrayType.getDimensions()-1);
            return castedArrayType.getElementType();
        }
        if (arrayType==PrimitiveType.STRING)return PrimitiveType.INT; 
        throw new SemanticException("Cannot index non array type", 0, 0, "TypeError");
    }

    @Override
    public Type visit(ArrayCreation node) {
        for (var size : node.getSizes()) {
            Type sizeType = size.accept(this);
            if (sizeType!=PrimitiveType.INT)throw new SemanticException("Array size must be INT", 0, 0, "TypeError");
        }
        return new ArrayType(node.getElementType(), node.getSizes().size());
    }

    @Override
    public Type visit(FieldAccess node) {
        Type objectType = node.getObject().accept(this);
        if (!(objectType instanceof CollectionType))throw new SemanticException("Field access on not collection type", 0, 0, "TypeError");
        List<CollectionField> fields = collectionFields.get(objectType.getName());
        if (fields == null)throw new SemanticException("Unknown collection: " + objectType.getName(), 0, 0, "CollectionError");
        for (var field : fields){
             if (field.getName().equals(node.getFieldName())) return field.getType();
        }
        throw new SemanticException("Field '" + node.getFieldName() + "' not found in " + objectType.getName(), 0, 0, "CollectionError");
    }

    @Override
    public Type visit(VariableDeclaration node) {
        Type declaredType = node.getType();
        // known collection type?
        if (declaredType instanceof CollectionType coll) {
            if (!collectionFields.containsKey(coll.getName())) {
                throw new SemanticException("Unknown collection type: " + coll.getName(), 0, 0, "TypeError");
            }
        }
        if (node.getValue() != null) {
            Type valueType = node.getValue().accept(this);
            if (!isCompatible(declaredType, valueType)){
                throw new SemanticException("Cannot assign " + valueType.getName() + " to " + declaredType.getName()+", must be same type", 0, 0, "TypeError");
            }
        }
        currentSymbolTable.putToScope(node.getName(), declaredType);
        return declaredType;
    }

    @Override
    public Type visit(ArrayInitializer node) {
         Type elementType = null;
        for (var element : node.getElements()) {
            Type type = element.accept(this);
            if (elementType == null) elementType = type;
            else if (!isCompatible(elementType, type))throw new SemanticException("Incorrect types in array initializer", 0, 0, "TypeError");
        }
        if (elementType!=null)return new ArrayType(elementType, 1);
        return new ArrayType(PrimitiveType.INT, 1);
    }

    @Override
    public Type visit(CollectionField node) {
        return node.getType();
    }

    @Override
    public Type visit(CollectionDeclaration node) {
        String collectionName = node.getName();
        if (!Character.isUpperCase(collectionName.charAt(0)))throw new SemanticException("Collection name should start with an uppercase: " + collectionName, 0, 0, "CollectionError");
        if (currentSymbolTable.entries.containsKey(collectionName))throw new SemanticException("Collection '" + collectionName + "' already exists", 0, 0, "CollectionError");
        collectionFields.put(collectionName, node.getFields());
        currentSymbolTable.putToScope(collectionName, new CollectionType(collectionName));
        return null;
    }

    @Override
    public Type visit(FunctionParameter node) {
        return node.getType();
    }

    private boolean alwaysReturns(ASTNode node) {
        if (node instanceof ReturnStatement) {
            return true;
        }
        if (node instanceof BlockStatement block) {
            for (var stmt : block.getStatements()) {
                if (alwaysReturns(stmt)) return true;
            }
        }
        if (node instanceof IfStatement ifStmt) {
            if (ifStmt.getElseBlock() == null) return false;
            return alwaysReturns(ifStmt.getThenBlock()) && alwaysReturns(ifStmt.getElseBlock());
        }
        return false;
    }
    @Override
    public Type visit(FunctionDeclaration node) {
        //function signature
        List<Type> paramTypes = node.getParameters().stream().map(FunctionParameter::getType).toList();
        currentSymbolTable.putToScope(node.getName(), new FunctionType(node.getReturnType(), paramTypes));

        // inside of the function in a new scope
        currentSymbolTable = new SymbolTable(currentSymbolTable);
        for (var parameter : node.getParameters()){
            currentSymbolTable.putToScope(parameter.getName(), parameter.getType());
        }
        Type savedReturnType = currentReturnType;
        currentReturnType = node.getReturnType();
        node.getBody().accept(this);
        if (currentReturnType != null) {
            if (!alwaysReturns(node.getBody())) {
                throw new SemanticException("Function '" + node.getName() + "' must return a value of type " + currentReturnType.getName(), 0, 0, "ReturnError");
            }
        }

        currentReturnType = savedReturnType;
        currentSymbolTable = currentSymbolTable.previousTable;
        return null;
    }

    @Override
    public Type visit(BlockStatement node) {
        currentSymbolTable = new SymbolTable(currentSymbolTable);
        for(var statement: node.getStatements()){
            statement.accept(this);
        }
        currentSymbolTable = currentSymbolTable.previousTable;
        return null;
    }

    @Override
    public Type visit(IfStatement node) {
        if(node.getCondition().accept(this) != PrimitiveType.BOOL){
            throw new SemanticException("if condition must be BOOL", 0, 0, "MissingConditionError");
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
        for(var statement : node.getStatements()) {
            if(statement instanceof FunctionDeclaration functionDeclaration) {
                List<Type> parameterTypes = new ArrayList<>();
                for (var parameter : functionDeclaration.getParameters()) {
                    parameterTypes.add(parameter.getType());
                }
                currentSymbolTable.putToScope(functionDeclaration.getName(),
                        new FunctionType(functionDeclaration.getReturnType(), parameterTypes));
            }
        }
        for(var statement : node.getStatements()) {
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

    private boolean isNumeric(Type t) {
        return t==PrimitiveType.INT || t==PrimitiveType.FLOAT;
    }

    
}

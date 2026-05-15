package compiler.SemanticAnalysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

public class SemanticAnalysis implements ASTVisitor<Type>{
    private SymbolTable currentSymbolTable;
    private Type currentReturnType = null;
    private final HashMap<String, List<CollectionField>> collectionFields = new HashMap<>();
    private static final Set<String> RESERVED_NAMES = new HashSet<>(Arrays.asList(
        "INT", "FLOAT", "BOOL", "STRING","while", "if", "else", "for", "def", 
            "coll", "return", "not", "final", "ARRAY"));


    public SemanticAnalysis() {
        currentSymbolTable = new SymbolTable(null);
    }


    public void analyze(ProgramNode root){
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
        if (node.getLeft() instanceof Identifier) {
            String name = ((Identifier) node.getLeft()).getName();
            SymbolTable.SymbolEntry entry = currentSymbolTable.getEntry(name);
            if (entry != null && entry.isFinal) {
                throw new SemanticException(
                        "Cannot reassign final variable: " + name,
                        node.getLine(), node.getColumn(),
                        "TypeError"
                );
            }
        }
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

        Type resultType = switch (operator) {
            case "+" -> {
                if (leftType == PrimitiveType.STRING && rightType == PrimitiveType.STRING)
                    yield PrimitiveType.STRING;
                if (!isNumeric(leftType) || !isNumeric(rightType))
                    throw new SemanticException("Operator '+' requires numeric types or two STRINGs", node.getLine(), node.getColumn(), "OperatorError");
                yield (leftType == PrimitiveType.FLOAT || rightType == PrimitiveType.FLOAT) ? PrimitiveType.FLOAT : PrimitiveType.INT;
            }
            case "-", "*", "/" -> {
                if (!isNumeric(leftType) || !isNumeric(rightType))
                    throw new SemanticException("Operator '" + operator + "' requires numeric types", node.getLine(), node.getColumn(), "OperatorError");
                yield (leftType == PrimitiveType.FLOAT || rightType == PrimitiveType.FLOAT) ? PrimitiveType.FLOAT : PrimitiveType.INT;
            }
            case "%" -> {
                if (leftType != PrimitiveType.INT || rightType != PrimitiveType.INT)
                    throw new SemanticException("Operator '%' requires INT types", node.getLine(), node.getColumn(), "OperatorError");
                yield PrimitiveType.INT;
            }
            case "==", "=/=" -> {
                if (!isCompatible(leftType, rightType))
                    throw new SemanticException("Operator '" + operator + "' requires same types", node.getLine(), node.getColumn(), "OperatorError");
                yield PrimitiveType.BOOL;
            }
            case "<", ">", "<=", ">=" -> {
                if (!isNumeric(leftType) || !isNumeric(rightType))
                    throw new SemanticException("Operator '" + operator + "' requires numeric types", node.getLine(), node.getColumn(), "OperatorError");
                yield PrimitiveType.BOOL;
            }
            case "&&", "||" -> {
                if (leftType != PrimitiveType.BOOL || rightType != PrimitiveType.BOOL)
                    throw new SemanticException("Operator '" + operator + "' requires BOOL types", node.getLine(), node.getColumn(), "OperatorError");
                yield PrimitiveType.BOOL;
            }
            default -> throw new SemanticException("Unknown operator '" + operator + "'", node.getLine(), node.getColumn(), "OperatorError");
        };
        node.setResolvedType(resultType);
        if (isNumeric(leftType) && isNumeric(rightType)) {
            foldArithmetic(node);
        }
        return resultType;
    }
    private Object getConstantValue(ASTNode node) {
        if (node == null) return null;

        if (node instanceof Literal lit) {
            try { return Integer.parseInt(lit.getValue()); }
            catch (NumberFormatException e) { return null; }
        }

        if (node instanceof BooleanTerm bool) {
            return bool.getValue();
        }

        if (node instanceof BinaryExpression bin && bin.hasConstantValue()) {
            return bin.getConstantValue();
        }

        if (node instanceof Identifier id && id.hasConstantValue()) {
            return id.getConstantValue();
        }

        return null;
    }
    private void foldArithmetic(BinaryExpression node) {
        String op = node.getOperator();
        Object v1 = getConstantValue(node.getLeft());
        Object v2 = getConstantValue(node.getRight());

        if (v1 == null || v2 == null) return;

        try {
            Object res = null;

            if (v1 instanceof Integer i1 && v2 instanceof Integer i2) {
                res = switch (op) {
                    case "+" -> i1 + i2;
                    case "-" -> i1 - i2;
                    case "*" -> i1 * i2;
                    case "/" -> {
                        if (i2 == 0) throw new SemanticException("Division by zero", node.getLine(), node.getColumn(), "OperatorError");
                        yield i1 / i2;
                    }
                    case "%" -> {
                        if (i2 == 0) throw new SemanticException("Division by zero", node.getLine(), node.getColumn(), "OperatorError");
                        yield i1 % i2;
                    }
                    case "==" -> i1.equals(i2);
                    case "=/=" -> !i1.equals(i2);
                    case "<" -> i1 < i2;
                    case ">" -> i1 > i2;
                    case "<=" -> i1 <= i2;
                    case ">=" -> i1 >= i2;
                    default -> null;
                };
            }
            else if (v1 instanceof Boolean b1 && v2 instanceof Boolean b2) {
                res = switch (op) {
                    case "&&" -> b1 && b2;
                    case "||" -> b1 || b2;
                    case "==" -> b1 == b2;
                    case "=/=" -> b1 != b2;
                    default -> null;
                };
            }

            if (res != null) {
                node.setConstantValue(res);
            }
        } catch (ArithmeticException e) {
            throw new SemanticException("Arithmetic error during folding", node.getLine(), node.getColumn(), "OperatorError");
        }
    }

    @Override
    public Type visit(UnaryExpression node) {
        Type operand = node.getOperand().accept(this);
        String operator = node.getOperator();
        Type resultType;
        if (operator.equals("not")) {
            if (operand != PrimitiveType.BOOL)
                throw new SemanticException("Operator '" + operator + "' requires a BOOL, but got " + operand, node.getLine(), node.getColumn(), "TypeError");
            resultType = PrimitiveType.BOOL;
        } else if (operator.equals("-")) {
            if (!isNumeric(operand))
                throw new SemanticException("Unary expression '-' requires numeric type", node.getLine(), node.getColumn(), "OperatorError");
            resultType = operand;
        } else if (operator.equals("++") || operator.equals("--")) {
            if (operand != PrimitiveType.INT)
                throw new SemanticException("Operator '" + operator + " ' requires an INT", node.getLine(), node.getColumn(), "OperatorError");
            resultType = PrimitiveType.INT;
        } else {
            throw new SemanticException("Unknown unary operator '" + operator + "'", node.getLine(), node.getColumn(), "OperatorError");
        }
        node.setResolvedType(resultType);
        return resultType;
    }

    @Override
    public Type visit(Literal node) {
        node.setResolvedType(PrimitiveType.INT);
        return PrimitiveType.INT;
    }

    @Override
    public Type visit(FloatLiteral node) {
        node.setResolvedType(PrimitiveType.FLOAT);
        return PrimitiveType.FLOAT;
    }

    @Override
    public Type visit(BooleanTerm node) {
        node.setResolvedType(PrimitiveType.BOOL);
        return PrimitiveType.BOOL;
    }

    @Override
    public Type visit(CharacterChain node) {
        node.setResolvedType(PrimitiveType.STRING);
        return PrimitiveType.STRING;
    }

    @Override
    public Type visit(Identifier node) {
        SymbolTable.SymbolEntry entry = currentSymbolTable.getEntry(node.getName());
        if (entry == null) throw new SemanticException("Undeclared identifier " + node.getName(), node.getLine(), node.getColumn(), "ScopeError");
        Type type = currentSymbolTable.getType(node.getName());
        currentSymbolTable.markAsUsed(node.getName());
        if (entry.isFinal && entry.value != null) {
            node.setConstantValue(entry.value);
        }
        node.setResolvedType(type);
        return type;
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
            if (node.getArguments().size() != 1)throw new SemanticException("length() expects one argument", node.getLine(), node.getColumn(), "ArgumentError");
            Type argumentType = node.getArguments().get(0).accept(this);
            if (argumentType!=PrimitiveType.STRING && !(argumentType instanceof ArrayType)){
                throw new SemanticException("length() expects STRING or ARRAY type", node.getLine(), node.getColumn(), "ArgumentError");
            }
            node.setResolvedType(PrimitiveType.INT);
            return PrimitiveType.INT;
        }

        //normal case :
        Type functionType = currentSymbolTable.getType(name);
        if (!(functionType instanceof FunctionType))throw new SemanticException(name + " is not a function", node.getLine(), node.getColumn(), "ScopeError");
        FunctionType castedFunctionType = (FunctionType) functionType;
        List<ASTNode> arguments = node.getArguments();
        List<Type> parameterTypes = castedFunctionType.getParametersType();
        if (arguments.size() != parameterTypes.size()){
            throw new SemanticException("Wrong number of arguments for " + name, node.getLine(), node.getColumn(), "ArgumentError");
        }
        for (int i = 0; i < arguments.size(); i++) {
            Type argumentType = arguments.get(i).accept(this);
            if (!isCompatible(parameterTypes.get(i), argumentType)){
                throw new SemanticException("Argument " + i + " of " + name + ": expected " + parameterTypes.get(i).getName() + " but received argument is : " + argumentType.getName(), node.getLine(), node.getColumn(), "ArgumentError");
            }

        }
        Type returnType = castedFunctionType.getReturnType();
        node.setResolvedType(returnType);
        return returnType;
    }

    @Override
    public Type visit(CollectionConstructor node) {
        String name = node.getName();
        List<CollectionField> fields = collectionFields.get(name);
        if (fields == null)throw new SemanticException("Unknown collection type: " + name, node.getLine(), node.getColumn(), "CollectionError");
        if (node.getArguments().size() != fields.size())throw new SemanticException("Incorrect number of arguments for " + name, node.getLine(), node.getColumn(), "ArgumentError");
        for (int i = 0; i < fields.size(); i++) {
            Type argumentType = node.getArguments().get(i).accept(this);
            if (!isCompatible(fields.get(i).getType(), argumentType)){
                throw new SemanticException("Field " + fields.get(i).getName() + ": expected " + fields.get(i).getType().getName() + " but received " + argumentType.getName(), node.getLine(), node.getColumn(), "ArgumentError");
            }
        }
        Type collectionType = new CollectionType(name);
        node.setResolvedType(collectionType);
        return collectionType;
    }

    @Override
    public Type visit(ArrayAccess node) {
        Type arrayType = node.getArray().accept(this);
        Type indexType = node.getIndex().accept(this);
        if (indexType!=PrimitiveType.INT)throw new SemanticException("Array index must be INT", node.getLine(), node.getColumn(), "TypeError");
        Type resultType;
        if (arrayType instanceof ArrayType castedArrayType) {
            resultType = castedArrayType.getDimensions() > 1
                ? new ArrayType(castedArrayType.getElementType(), castedArrayType.getDimensions() - 1)
                : castedArrayType.getElementType();
        } else if (arrayType == PrimitiveType.STRING) {
            resultType = PrimitiveType.INT;
        } else {
            throw new SemanticException("Cannot index non array type", node.getLine(), node.getColumn(), "TypeError");
        }
        node.setResolvedType(resultType);
        return resultType;
    }

    @Override
    public Type visit(ArrayCreation node) {
        for (var size : node.getSizes()) {
            Type sizeType = size.accept(this);
            if (sizeType!=PrimitiveType.INT)throw new SemanticException("Array size must be INT", node.getLine(), node.getColumn(), "TypeError");
        }
        Type arrayType = new ArrayType(node.getElementType(), node.getSizes().size());
        node.setResolvedType(arrayType);
        return arrayType;
    }

    @Override
    public Type visit(FieldAccess node) {
        Type objectType = node.getObject().accept(this);
        if (!(objectType instanceof CollectionType))throw new SemanticException("Field access on not collection type", node.getLine(), node.getColumn(), "TypeError");
        List<CollectionField> fields = collectionFields.get(objectType.getName());
        if (fields == null)throw new SemanticException("Unknown collection: " + objectType.getName(), node.getLine(), node.getColumn(), "CollectionError");
        for (var field : fields) {
            if (field.getName().equals(node.getFieldName())) {
                node.setResolvedType(field.getType());
                return field.getType();
            }
        }
        throw new SemanticException("Field '" + node.getFieldName() + "' not found in " + objectType.getName(), node.getLine(), node.getColumn(), "CollectionError");
    }

    @Override
    public Type visit(VariableDeclaration node) {
        Type declaredType = node.getType();
        Type valueType = null;
        if (node.getValue() != null) {
            valueType = node.getValue().accept(this);
        }
        if (declaredType == null) {
            if (valueType == null) {
                throw new SemanticException(
                        "Cannot infer type for '" + node.getName() + "': expression returns void or has no value",
                        node.getLine(), node.getColumn(), "TypeError"
                );
            }
            declaredType = valueType;
            node.setType(declaredType);
        }
        else {
            if (valueType != null) {
                if (!isCompatible(declaredType, valueType)){
                    throw new SemanticException("Cannot assign " + valueType.getName() + " to " + declaredType.getName() + ", must be same type",
                            node.getLine(), node.getColumn(), "TypeError");
                }
            }
        }

        if (node.isFinal() && !(declaredType instanceof PrimitiveType)) {
            throw new SemanticException("Constants can only have base types (INT, FLOAT, BOOL, STRING)",
                    node.getLine(), node.getColumn(), "TypeError");
        }
        if (declaredType instanceof CollectionType coll) {
            if (!collectionFields.containsKey(coll.getName())) {
                throw new SemanticException("Unknown collection type: " + coll.getName(),
                        node.getLine(), node.getColumn(), "TypeError");
            }
        }
        Object cstVal = getConstantValue(node.getValue());
        int line = node.getLine();
        currentSymbolTable.putToScope(node.getName(), declaredType, node.isFinal(),line,cstVal);
        return declaredType;
    }

    @Override
    public Type visit(ArrayInitializer node) {
        Type elementType = null;
        for (var element : node.getElements()) {
            Type type = element.accept(this);
            if (elementType == null) elementType = type;
            else if (!isCompatible(elementType, type))throw new SemanticException("Incorrect types in array initializer", node.getLine(), node.getColumn(), "TypeError");
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
        if (!Character.isUpperCase(collectionName.charAt(0)))throw new SemanticException("Collection name should start with an uppercase: " + collectionName, node.getLine(), node.getColumn(), "CollectionError");
        if (RESERVED_NAMES.contains(collectionName))throw new SemanticException("Collection name '" + collectionName + "' can not overwrite existing type/keyword", node.getLine(), node.getColumn(), "CollectionError");
        if (currentSymbolTable.entries.containsKey(collectionName))throw new SemanticException("Collection '" + collectionName + "' already exists", node.getLine(), node.getColumn(), "CollectionError");
        collectionFields.put(collectionName, node.getFields());
        currentSymbolTable.putToScope(collectionName, new CollectionType(collectionName));
        return null;
    }

    @Override
    public Type visit(FunctionParameter node) {
        return node.getType();
    }

    @Override
    public Type visit(FunctionDeclaration node) {
        currentSymbolTable = new SymbolTable(currentSymbolTable);
        for (var parameter : node.getParameters()){
            currentSymbolTable.putToScope(parameter.getName(), parameter.getType());
        }

        Type savedReturnType = currentReturnType;
        currentReturnType = node.getReturnType();
        node.getBody().accept(this);
        if (currentReturnType != null) {
            if (!alwaysReturns(node.getBody())) {
                throw new SemanticException("Function '" + node.getName() + "' must return " + currentReturnType.getName(),
                        node.getLine(), node.getColumn(), "ReturnError");
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
        checkUnusedVariables(currentSymbolTable);
        currentSymbolTable = currentSymbolTable.previousTable;
        return null;
    }

    @Override
    public Type visit(IfStatement node) {
        if(node.getCondition().accept(this) != PrimitiveType.BOOL){
            throw new SemanticException("if condition must be BOOL", node.getLine(), node.getColumn(), "MissingConditionError");
        }
        node.getThenBlock().accept(this);
        if(node.getElseBlock()!=null)node.getElseBlock().accept(this);
        return null;
    }

    @Override
    public Type visit(WhileStatement node) {
        if (node.getCondition().accept(this) != PrimitiveType.BOOL){
            throw new SemanticException("While condition must be BOOL", node.getLine(), node.getColumn(), "MissingConditionError");
        }
        node.getWhileBlock().accept(this);
        return null;
    }

    @Override
    public Type visit(ForStatement node) {
        node.getVariable().accept(this);
        if (node.getFrom().accept(this) != PrimitiveType.INT || node.getTo().accept(this) != PrimitiveType.INT){
            throw new SemanticException("for range must be INT", node.getLine(), node.getColumn(), "TypeError");
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
            throw new SemanticException("Expected " + currentReturnType + " but got " + returnType, node.getLine(), node.getColumn(), "ReturnError");
        }
        return returnType;
    }

    @Override
    public Type visit(ProgramNode node) {
        // Constants must be on top of the file
        boolean constantsSectionDone = false;
        for (var statement : node.getStatements()) {
            if (statement instanceof VariableDeclaration vd && vd.isFinal()) {
                if (constantsSectionDone)
                    throw new SemanticException("Constants must be declared at the top of the file", vd.getLine(), vd.getColumn(), "ScopeError");
            } 
            else {
                constantsSectionDone = true;
            }
        }

        for(var statement : node.getStatements()) {
            if(statement instanceof CollectionDeclaration coll) {
                coll.accept(this);
            }
        }

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
            if (!(statement instanceof CollectionDeclaration)) {
                statement.accept(this);
            }
        }
        return null;
    }
    private void checkUnusedVariables(SymbolTable table) {
        for (String name : table.entries.keySet()) {
            SymbolTable.SymbolEntry entry = table.entries.get(name);
            if (!entry.isUsed && !(entry.type instanceof FunctionType)) {
                System.out.println("Warning: Variable '" + name + "' declared at line :  "
                        + entry.line + " is never used.");
            }
        }
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

}
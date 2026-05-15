package compiler.Parser;

import compiler.Lexer.Lexer;
import compiler.Lexer.Symbol;
import compiler.Lexer.Lexer.SymbolType;
import compiler.Parser.AST.ASTNode;
import compiler.Parser.AST.ASTNodeBase;
import compiler.Parser.AST.Declarations.ArrayInitializer;
import compiler.Parser.AST.Declarations.CollectionDeclaration;
import compiler.Parser.AST.Declarations.CollectionField;
import compiler.Parser.AST.Declarations.FunctionDeclaration;
import compiler.Parser.AST.Declarations.FunctionParameter;
import compiler.Parser.AST.Declarations.VariableDeclaration;
import compiler.Parser.AST.Expressions.*;
import compiler.Parser.AST.ProgramNode;
import compiler.Parser.AST.Statements.BlockStatement;
import compiler.Parser.AST.Statements.ForStatement;
import compiler.Parser.AST.Statements.IfStatement;
import compiler.Parser.AST.Statements.ReturnStatement;
import compiler.Parser.AST.Statements.WhileStatement;
import compiler.Parser.AST.Types.ArrayType;
import compiler.Parser.AST.Types.PrimitiveType;
import compiler.Parser.AST.Types.CollectionType;
import compiler.Parser.AST.Types.Type;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    private final Lexer lexer;
    private Symbol lookahead;


    public Parser(Lexer lexer) {
        this.lexer = lexer;
        advance();
    }
    private void advance() {
        lookahead = lexer.getNextSymbol();
    }
    private boolean check(String value) {
        return lookahead.getName().equals(value);
    }
    private boolean checkType(Lexer.SymbolType type) {
        return lookahead.getType() == type;
    }
    public ProgramNode getAST()throws ParserException{
        return parseTree();
    }

    private ProgramNode parseTree() throws ParserException{
        ArrayList<ASTNode> nodes = new ArrayList<>();
        while (!checkType(Lexer.SymbolType.EOF)) {
            if (check("coll")) {
                nodes.add(parseCollectionDeclaration());
            } else if (check("def")) {
                nodes.add(parseFunctionDeclaration());
            } else {
                nodes.add(parseStatements());
            }
        }
        return new ProgramNode(nodes);
    }

    private ASTNode parseFunctionDeclaration() throws ParserException {
        int line = lexer.getLine();
        int column = lexer.getColumn();
        match("def");

        //returntype is optionnal, if it is TYPE or COLLECTION_TYpE then ok else null
        Type returnType = null;
        if(checkType(Lexer.SymbolType.TYPE)||checkType(Lexer.SymbolType.COLLECTION_TYPE))returnType=parseType();

        //functino name
        if (!checkType(Lexer.SymbolType.IDENTIFIER))
            throw new ParserException("Expected function name after 'def'", lexer.getLine(), lookahead.getName());
        String name = lookahead.getName();
        advance();

        // params
        match("(");
        List<FunctionParameter> parameters = new ArrayList<>();
        while (!check(")")) {
            Type paramType = parseType();
            if (!checkType(Lexer.SymbolType.IDENTIFIER))
                throw new ParserException("Expected parameter name", lexer.getLine(), lookahead.getName());
            String paramName = lookahead.getName();
            advance();
            parameters.add(new FunctionParameter(paramType, paramName));
            if (check(",")) advance();
        }
        match(")");
        ASTNode body = parseBlock();
        FunctionDeclaration fd = new FunctionDeclaration(returnType, name, parameters, body);
        fd.setLine(line); fd.setColumn(column);
        return fd;
    }

    private ASTNode parseCollectionDeclaration() throws ParserException {
        int line = lexer.getLine();
        int column = lexer.getColumn();
        match("coll");

        if (!checkType(Lexer.SymbolType.COLLECTION_TYPE))
            throw new ParserException("Expected collection name in uppercase after \"coll\"", lexer.getLine(), lookahead.getName());
        String name = lookahead.getName();
        advance();

        match("{");
        List<CollectionField> fields = new ArrayList<>();
        while (!check("}")) {
            Type fieldType = parseType();
            if (!checkType(Lexer.SymbolType.IDENTIFIER))
                throw new ParserException("Expected field name in collection", lexer.getLine(), lookahead.getName());
            String fieldName = lookahead.getName();
            advance();
            match(";");
            fields.add(new CollectionField(fieldType, fieldName));
        }
        match("}");
        CollectionDeclaration cd = new CollectionDeclaration(name, fields);
        cd.setLine(line); cd.setColumn(column);
        return cd;
    }

    private ASTNode parseStatements()throws ParserException {

        if (check("if"))     return parseIfStatement();
        if (check("while"))  return parseWhileStatement();
        if (check("for"))    return parseForStatement();
        if (check("return")) return parseReturnStatement();

        // declaration
        if (check("final") || check("var") ||
                lookahead.getType() == Lexer.SymbolType.TYPE ||
                lookahead.getType() == Lexer.SymbolType.COLLECTION_TYPE) {

            return parseVariableDeclaration();
        }

        ASTNode expr = parseExpression();
        match(";");
        return expr;
    }

    private ASTNode parseIfStatement() throws ParserException {
        int line = lexer.getLine();
        int column = lexer.getColumn();
        match("if");
        match("(");
        ASTNode condition = parseExpression();
        match(")");
        ASTNode thenBlock = parseBlock();
        ASTNode elseBlock = null;
        if (check("else")) {
            advance();
            elseBlock = parseBlock();
        }
        IfStatement ifStatement = new IfStatement(condition, thenBlock, elseBlock);
        ifStatement.setLine(line); ifStatement.setColumn(column);
        return ifStatement;
    }

    private ASTNode parseReturnStatement() throws ParserException {
        int line = lexer.getLine();
        int column = lexer.getColumn();
        match("return");
        // empty return/void
        if (check(";")) {
            advance();
            ReturnStatement returnStatement = new ReturnStatement(null);
            returnStatement.setLine(line); returnStatement.setColumn(column);
            return returnStatement;
        }
        ASTNode value = parseExpression();
        match(";");
        ReturnStatement returnStatement = new ReturnStatement(value);
        returnStatement.setLine(line); returnStatement.setColumn(column);
        return returnStatement;
    }

    private ASTNode parseForStatement() throws ParserException {
        int line = lexer.getLine();
        int column = lexer.getColumn();
        match("for");
        match("(");
        ASTNode variable;
        if (checkType(Lexer.SymbolType.TYPE) || checkType(Lexer.SymbolType.COLLECTION_TYPE)) {
            //parse type thn name : create VariableDeclaration without value
            Type varType = parseType();
            if (!checkType(Lexer.SymbolType.IDENTIFIER))
                throw new ParserException("Expected identifier in for loop", lexer.getLine(), lookahead.getName());
            String varName = lookahead.getName();
            advance();
            variable = new VariableDeclaration(varType, varName, false, null);
        }
        else if (checkType(Lexer.SymbolType.IDENTIFIER)) {
            //else its identifier so reference to existing variable
            String varName = lookahead.getName();
            advance();
            variable = new Identifier(varName);
        }
        else {
            throw new ParserException("Expected type or identifier in for loop", lexer.getLine(), lookahead.getName());
        }
        match(";");

        //accept 1 -> 100 and 1 - > 100
        ASTNode from = parseExpression();
        if (check("->")) {
            advance();
        }
        else if (check("-")) {
            advance();
            match(">");
        }
        else {
            throw new ParserException("Expected '->' in for loop range", lexer.getLine(), lookahead.getName());
        }
        ASTNode to = parseExpression();
        match(";");

        //update ex: i+1
        ASTNode update = parseExpression();
        match(")");
        ASTNode body = parseBlock();
        ForStatement forStatement = new ForStatement(variable, from, to, update, body);
        forStatement.setLine(line); forStatement.setColumn(column);
        return forStatement;
    }

    private ASTNode parseWhileStatement() throws ParserException {
        int line = lexer.getLine();
        int column = lexer.getColumn();
        match("while");
        match("(");
        ASTNode condition = parseExpression();
        match(")");
        ASTNode whileBlock = parseBlock();
        WhileStatement whileStatement = new WhileStatement(condition, whileBlock);
        whileStatement.setLine(line); whileStatement.setColumn(column);
        return whileStatement;
    }


    private ASTNode parseBlock() throws ParserException {
        match("{");
        ArrayList<ASTNode> statements = new ArrayList<>();
        while (!check("}")) {
            statements.add(parseStatements()); // Un bloc contient d'autres statements !
        }
        match("}");
        return new BlockStatement(statements);
    }

    private ASTNode parseExpression() throws ParserException {
        return parseAssignment();
    }



    private ASTNode parseVariableDeclaration() throws ParserException {
        int line = lexer.getLine();
        int column = lexer.getColumn();
        boolean isFinal = false;
        if (check("final")) {
            isFinal = true;
            advance();
        }
        Type type = null;
        boolean isVar = false;
        if (check("var")) {
            isVar = true;
            advance();
        }else {
            type = parseType(); // INT ,INT[]..,Person
        }
        String name = lookahead.getName();
        if(!checkType(Lexer.SymbolType.IDENTIFIER)) {
            throw new ParserException("expected identifier name after type at : ",lexer.getLine(),lookahead.getName());
        }
        advance();
        ASTNode value = null;
        if (check("=")) {
            advance();
            value = parseExpression();
        } else {
            if (isVar) {
                throw new ParserException("Type inference 'var' requires an immediate assignment", line, name);
            }
            if (isFinal) {
                throw new ParserException("Final variables must be initialized", line, name);
            }
        }

        match(";");
        VariableDeclaration variableDeclaration = new VariableDeclaration(type, name, isFinal, value);
        variableDeclaration.setLine(line); variableDeclaration.setColumn(column);
        return variableDeclaration;
    }



    private Type parseType() throws ParserException {
        if (lookahead.getType() != Lexer.SymbolType.TYPE && lookahead.getType() != Lexer.SymbolType.COLLECTION_TYPE) {
            throw new ParserException("type or collection_type expected : ", lexer.getLine(), lookahead.getName());
        }

        String baseName = lookahead.getName();
        advance();
        Type type;
        try {
            type = PrimitiveType.fromString(baseName);
        } catch (Exception e) {
            type = new CollectionType(baseName); // Pour Person, Point, etc.
        }
        // counting  [] for dimensions
        int dimensions = 0;
        while (check("[")) {
            match("[");
            match("]");
            dimensions++;
        }
        if (dimensions > 0) {
            return new ArrayType(type, dimensions);
        }
        return type;
    }
    private ASTNode parseAssignment() throws ParserException {
        ASTNode left = parseLogicalOr();
        if (check("=") || check("+=") || check("-=") || check("*=") || check("/=")) {
            String operator = lookahead.getName();
            int line = lexer.getLine();
            int col = lexer.getColumn();
            advance();
            ASTNode right = parseAssignment();
            if (operator.equals("=")) {
                Assignment assign = new Assignment(left, right);
                assign.setLine(line); assign.setColumn(col);
                return assign;
            } else {
                String baseOp = operator.substring(0, 1);
                BinaryExpression arithmetic = new BinaryExpression(left, baseOp, right);
                arithmetic.setLine(line); arithmetic.setColumn(col);
                Assignment assign = new Assignment(left, arithmetic);
                assign.setLine(line); assign.setColumn(col);
                return assign;
            }
        }
        return left;
    }
    // Ajoute le OU Logique
    private ASTNode parseLogicalOr() throws ParserException {
        ASTNode left = parseLogicalAnd();

        while (check("||")) {
            String operator = lookahead.getName();
            advance();
            ASTNode right = parseLogicalAnd();
            left = new BinaryExpression(left, operator, right);
        }
        return left;
    }

    // Ajoute le ET Logique
    private ASTNode parseLogicalAnd() throws ParserException {
        ASTNode left = parseEquality();

        while (check("&&")) {
            String operator = lookahead.getName();
            advance();
            ASTNode right = parseEquality();
            left = new BinaryExpression(left, operator, right);
        }
        return left;
    }
    private ASTNode parseEquality() throws ParserException {
        ASTNode left = parseRelational();

        while (check("==") || check("=/=")) {
            String operator = lookahead.getName();
            advance();
            ASTNode right = parseRelational();
            left = new BinaryExpression(left, operator, right);
        }
        return left;
    }

    private ASTNode parseRelational() throws ParserException {
        ASTNode left = parseAddition();

        while (check("<") || check(">") || check("<=") || check(">=")) {
            String operator = lookahead.getName();
            advance();
            ASTNode right = parseAddition();
            left = new BinaryExpression(left, operator, right);
        }
        return left;
    }
    // + et -
    private ASTNode parseAddition() throws ParserException {
        ASTNode left = parseMultiplication();
        while (lookahead.getName().equals("+") || lookahead.getName().equals("-")) {
            String operator = lookahead.getName();
            advance(); // on passe le +

            ASTNode right = parseMultiplication();
            left = new BinaryExpression(left, operator, right);
        }
        return left;
    }

    // * et /
    private ASTNode parseMultiplication() throws ParserException {
        ASTNode left = parseUnary();
        while (check("*") || check("/") || check("%")) {
            String operator = lookahead.getName();
            advance();
            ASTNode right = parseUnary();
            left = new BinaryExpression(left, operator, right);
        }
        return left;
    }
    //-a or -5
    private ASTNode parseUnary()throws ParserException{
        if (check("-") || check("not")|| check("!")|| check("++") || check("--")) {
            String operator = lookahead.getName();
            advance();
            ASTNode operand = parseUnary(); // Récursif pour gérer --3 par exemple
            return new UnaryExpression(operator, operand);
        }
        return parsePrimary();
    }
    private ASTNode parsePrimary() throws ParserException {
        // ( expression )
        if (check("(")) {
            advance();
            ASTNode expression = parseExpression();
            match(")");
            return expression;
        }
        //array initialization
        if (check("{")) {
            return parseArrayInitializer();
        }
        // INT
        if (lookahead.getType() == Lexer.SymbolType.INTEGER) {
            int line = lexer.getLine(); int column = lexer.getColumn();
            String value = lookahead.getName();
            String type = getType();
            advance();
            Literal literal = new Literal(value, type);
            literal.setLine(line); literal.setColumn(column);
            return literal;
        }

        // FLOAT
        if (lookahead.getType() == Lexer.SymbolType.FLOAT) {
            int line = lexer.getLine(); int column = lexer.getColumn();
            String value = lookahead.getName();
            String type = getType();
            advance();
            FloatLiteral floatLiteral = new FloatLiteral(value, type);
            floatLiteral.setLine(line); floatLiteral.setColumn(column);
            return floatLiteral;
        }
        // STRING
        if (lookahead.getType() == Lexer.SymbolType.STRING) {
            int line = lexer.getLine(); int column = lexer.getColumn();
            String val = lookahead.getName();
            String type = getType();
            advance();
            CharacterChain characterChain = new CharacterChain(val, type);
            characterChain.setLine(line); characterChain.setColumn(column);
            return characterChain;
        }

        // BOOLEAN
        if (lookahead.getType() == Lexer.SymbolType.BOOLEAN) {
            int line = lexer.getLine(); int column = lexer.getColumn();
            String val = lookahead.getName();
            String type = getType();
            advance();
            BooleanTerm booleanTerm = new BooleanTerm(val, type);
            booleanTerm.setLine(line); booleanTerm.setColumn(column);
            return booleanTerm;
        }
        // identifier (variable) / fct call / collection construction
        if (lookahead.getType() == Lexer.SymbolType.IDENTIFIER || lookahead.getType() == Lexer.SymbolType.COLLECTION_TYPE) {
            int line = lexer.getLine(); int column = lexer.getColumn();
            String name = lookahead.getName();
            advance();
            if (check("(")) {
                List<ASTNode> args = parseArguments();
                ASTNode callNode;
                //collection constructor, starts with Uppercase or is function call
                if (Character.isUpperCase(name.charAt(0))) {
                    callNode = new CollectionConstructor(name, args);
                } else {
                    callNode = new FunctionCall(name, args);
                }
                //we cast in order to setLine and col
                ((ASTNodeBase) callNode).setLine(line); ((ASTNodeBase) callNode).setColumn(column);
                //after call we can have .smthg or [smthg]
                return parsePostfix(callNode);
            }
            //after identifier we can have .smthg or [smthg]
            Identifier identifier = new Identifier(name);
            identifier.setLine(line); identifier.setColumn(column);
            return parsePostfix(identifier);

        }
        //array allocation
        if (lookahead.getType() == Lexer.SymbolType.TYPE || lookahead.getType() == Lexer.SymbolType.COLLECTION_TYPE) {
            String typeName = lookahead.getName();
            Type elementType;
            try {
                elementType = PrimitiveType.fromString(typeName);
            } catch (Exception e) {
                elementType = new CollectionType(typeName);
            }
            advance();

            // check if  ARRAY
            ArrayCreation array = parseArrayAllocation(elementType);
            if (array != null) {
                return array;
            } else {
                throw new ParserException("Keyword 'ARRAY' expected for array allocation", lexer.getLine(), lookahead.getName());
            }
        }


        throw new ParserException("No supported expression at : ", lexer.getLine(), lookahead.getName());
    }
    private List<ASTNode> parseArguments() throws ParserException {
        match("(");
        List<ASTNode> args = new ArrayList<>();

        if (!check(")")) {
            args.add(parseExpression());
            while (check(",")) {
                advance(); // consomme la virgule
                args.add(parseExpression());
            }
        }

        match(")");
        return args;
    }


    /**
     * Parses multidimensional arrays
     * **/
    private ASTNode parseArrayInitializer()throws ParserException {
        match("{");
        List<ASTNode> elements = new ArrayList<>();

        if (!check("}")) {
            elements.add(parseExpression());
            while (check(",")) {
                advance();
                elements.add(parseExpression());
            }
        }

        match("}");
        return new ArrayInitializer(elements);
    }

    /**
     * Parses array allocation INT ARRAY[expr];
     * @param elementType
     * @return
     * @throws ParserException
     */
    private ArrayCreation parseArrayAllocation(Type elementType) throws ParserException {
        if (check("ARRAY")) {
            advance();
            List<ASTNode> sizes = new ArrayList<>();

            match("[");
            sizes.add(parseExpression());
            match("]");
            while (check("[")) {
                advance(); // consomme "["
                sizes.add(parseExpression());
                match("]");
            }

            return new ArrayCreation(elementType, sizes);
        }
        return null;
    }

    /*private ASTNode parseArrayAccess(ASTNode node) throws ParserException {
        while (check("[")) {
            advance();
            ASTNode index = parseExpression();
            match("]");
            node = new ArrayAccess(node, index);
        }
        return node;
    }*/


    private ASTNode parsePostfix(ASTNode node) throws ParserException {
        while (check("[") || check(".")) {
            if (check("[")) {
                advance();
                ASTNode index = parseExpression();
                match("]");
                node = new ArrayAccess(node, index);
            } 
            else {
                //else there is a point so consum "." when advancing
                advance(); 
                if (!checkType(Lexer.SymbolType.IDENTIFIER))
                    throw new ParserException("Expected field name after '.'", lexer.getLine(), lookahead.getName());
                String fieldName = lookahead.getName();
                advance();
                node = new FieldAccess(node, fieldName);
            }
        }
        return node;
    }

    private String getType() {
        return lookahead.getType().toString().toLowerCase();
    }


    private void match(String expected) throws ParserException {
        if (!lookahead.getName().equals(expected)) {
            throw new ParserException(
                    "Expected '" + expected + "', found '" + lookahead.getName() + "'",
                    lexer.getLine(),
                    lookahead.getName()
            );
        }
        advance();
    }
}

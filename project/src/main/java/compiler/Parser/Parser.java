package compiler.Parser;

import compiler.Lexer.Lexer;
import compiler.Lexer.Symbol;
import compiler.Lexer.Lexer.SymbolType;
import compiler.Parser.AST.ASTNode;
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
        return new FunctionDeclaration(returnType, name, parameters, body);
    }

    private ASTNode parseCollectionDeclaration() throws ParserException {
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
        return new CollectionDeclaration(name, fields);
    }

    private ASTNode parseStatements()throws ParserException {

        if (check("if"))     return parseIfStatement();
        if (check("while"))  return parseWhileStatement();
        if (check("for"))    return parseForStatement();
        if (check("return")) return parseReturnStatement();

        // declaration
        if (check("final") ||
                lookahead.getType() == Lexer.SymbolType.TYPE ||
                lookahead.getType() == Lexer.SymbolType.COLLECTION_TYPE) {

            return parseVariableDeclaration();
        }

        ASTNode expr = parseExpression();
        match(";");
        return expr;
    }

    private ASTNode parseIfStatement() throws ParserException {
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
        return new IfStatement(condition, thenBlock, elseBlock);
    }

    private ASTNode parseReturnStatement() throws ParserException {
        match("return");
        // empty return/void
        if (check(";")) {
            advance();
            return new ReturnStatement(null);
        }
        ASTNode value = parseExpression();
        match(";");
        return new ReturnStatement(value);
    }

    private ASTNode parseForStatement() throws ParserException {
        match("for");
        match("(");
        //declaration de variable sans valeur initiale (ex: INT i)
        Type varType = parseType();
        String varName = lookahead.getName();
        if (!checkType(Lexer.SymbolType.IDENTIFIER))
            throw new ParserException("Expected identifier in for loop", lexer.getLine(), lookahead.getName());
        advance();
        VariableDeclaration variable = new VariableDeclaration(varType, varName, false, null);
        match(";");

        //plage  ex: 1 -> 100
        ASTNode from = parseExpression();
        match("->");
        ASTNode to = parseExpression();
        match(";");

        // màj  ex: i+1
        ASTNode update = parseExpression();
        match(")");
        ASTNode body = parseBlock();

        return new ForStatement(variable, from, to, update, body);
    }

    private ASTNode parseWhileStatement() throws ParserException {
        match("while");
        match("(");
        ASTNode condition = parseExpression();
        match(")");
        ASTNode whileBlock = parseBlock();
        return new WhileStatement(condition, whileBlock);
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
        boolean isFinal = false;
        if (check("final")) {
            isFinal = true;
            advance();
        }

        Type type = parseType(); // INT ,INT[]..,Person

        String name = lookahead.getName();
        if(!checkType(Lexer.SymbolType.IDENTIFIER)) {
            throw new ParserException("expected identifier name after type at : ",lexer.getLine(),lookahead.getName());
        }
        advance();
        // initial value : obligatory if final
        ASTNode value = null;
        if (check("=")) {
            advance();
            value = parseExpression();
        } else if (isFinal) {
            throw new ParserException("Une variable 'final' doit être initialisée", lexer.getLine(), name);
        }

        match(";");
        return new VariableDeclaration(type, name, isFinal, value);
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

        if (check("=")|| check("+=") || check("-=") || check("*=") || check("/=")) {
            advance();
            ASTNode right = parseAssignment();
            return new Assignment(left, right);
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
        if (check("-") || check("!")|| check("++") || check("--")) {
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
        // nombre entier
        if (lookahead.getType() == Lexer.SymbolType.INTEGER) {
            String value = lookahead.getName();
            String type = getType();
            advance();
            return new Literal(value, type);
        }

        // nombre flottant
        if (lookahead.getType() == Lexer.SymbolType.FLOAT) {
            String value = lookahead.getName();
            String type = getType();
            advance();
            return new FloatLiteral(value,type);
        }
        // STRING
        if (lookahead.getType() == Lexer.SymbolType.STRING) {
            String val = lookahead.getName();
            String type = getType();
            advance();
            return new CharacterChain(val,type);
        }

        // BOOLEAN
        if (lookahead.getType() == Lexer.SymbolType.BOOLEAN) {
            String val = lookahead.getName();
            String type = getType();
            advance();
            return new BooleanTerm(val,type);
        }
        // identifier (variable) / fct call / collection construction
        if (lookahead.getType() == Lexer.SymbolType.IDENTIFIER || lookahead.getType() == Lexer.SymbolType.COLLECTION_TYPE) {
            String name = lookahead.getName();
            advance();
            if (check("(")) {
                List<ASTNode> args = parseArguments();
                //collection constructor
                if (Character.isUpperCase(name.charAt(0))) {
                    return new CollectionConstructor(name, args);
                } else {
                    // else is function
                    return new FunctionCall(name, args);
                }
            }

            //identifier or array access
            ASTNode node = new Identifier(name);
            node = parseArrayAccess(node);
            return node;

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

    private ASTNode parseArrayAccess(ASTNode node) throws ParserException {
        while (check("[")) {
            advance();
            ASTNode index = parseExpression();
            match("]");
            node = new ArrayAccess(node, index);
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

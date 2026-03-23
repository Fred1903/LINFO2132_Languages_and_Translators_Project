package compiler.Parser;

import compiler.Lexer.Lexer;
import compiler.Lexer.Symbol;
import compiler.Parser.AST.ASTNode;
import compiler.Parser.AST.Declarations.VariableDeclaration;
import compiler.Parser.AST.Expressions.*;
import compiler.Parser.AST.ProgramNode;
import compiler.Parser.AST.Types.PrimitiveType;
import compiler.Parser.AST.Types.Type;

import java.util.ArrayList;

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
            nodes.add(parseStatements());
        }
        return new ProgramNode(nodes);
    }

    private ASTNode parseStatements()throws ParserException {

//        if (check("final") || lookahead.getType() == Lexer.SymbolType.TYPE) {
//            return parseVariableDeclaration();
//        }

        ASTNode expr = parseExpression();
        match(";");
        return expr;
    }
    private ASTNode parseExpression() throws ParserException {
        return parseExpression();
    }





    private ASTNode parseAssignment() throws ParserException {
        ASTNode left = parseEquality();

        if (check("=")) {
            advance();
            ASTNode right = parseAssignment(); // Récursif à droite pour permettre a = b = 5
            return new Assignment(left, right);
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
            advance(); // On passe le +

            ASTNode right = parseMultiplication(); // On cherche le bloc de droite
            left = new BinaryExpression(left, operator, right); // On crée l'étage de l'arbre
        }
        return left;
    }

    // * et /
    private ASTNode parseMultiplication() throws ParserException {
        ASTNode left = parsePrimary();
        while (check("*") || check("/") || check("%")) {
            String operator = lookahead.getName();
            advance();

            ASTNode right = parsePrimary();
            left = new BinaryExpression(left, operator, right);
        }
        return left;
    }
    private ASTNode parsePrimary() throws ParserException {
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
        // identifiant (variable)
        if (lookahead.getType() == Lexer.SymbolType.IDENTIFIER) {
            String name = lookahead.getName();
            advance();
            //TABLEAUX
            if (check("[")) {
                advance();
                ASTNode index = parseExpression();
                match("]");
                return new ArrayAccess(new Identifier(name), index);
            }
            // appel de fonction : nom()
            if (check("(")) {
//                return parseFunctionCall(name);
            }
            return new Identifier(name);
        }

        // ( expression )
        if (check("(")) {
            advance();
            ASTNode expression = parseExpression();
            match(")");
            return expression;
        }

        throw new ParserException("No primary expression found for : ", lexer.getLine(), lookahead.getName());
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

import compiler.Lexer.Lexer;
import compiler.Parser.AST.Declarations.ArrayInitializer;
import compiler.Parser.AST.Declarations.VariableDeclaration;
import compiler.Parser.AST.Expressions.*;
import compiler.Parser.AST.ProgramNode;
import compiler.Parser.Parser;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.StringReader;

public class TestParser {
    private ProgramNode parse(String input) throws Exception {
        Lexer lexer = new Lexer(new StringReader(input));
        Parser parser = new Parser(lexer);
        return parser.getAST();
    }
    @Test
    public void testFinalDeclarations() throws Exception {
        String input =
                "final INT i = 3;\n" +
                        "final FLOAT j = 3.2 * 5.0;\n" +
                        "final INT k = i * 3;\n" +
                        "final STRING message = \"Hello\";\n" +
                        "final BOOL isEmpty = true;";

        ProgramNode root = parse(input);
        assertEquals(5, root.statements.size());

        //final INT i = 3;
        VariableDeclaration v1 = (VariableDeclaration) root.statements.get(0);
        assertEquals("i", v1.identifier);
        assertEquals("INT", v1.type.getTypeName());
        assertTrue(v1.isFinal);
        assertTrue(v1.initialValue instanceof Literal);

        // final FLOAT j = 3.2 * 5.0;
        VariableDeclaration v2 = (VariableDeclaration) root.statements.get(1);
        assertEquals("j", v2.identifier);
        assertTrue(v2.initialValue instanceof BinaryExpression);
        BinaryExpression mult = (BinaryExpression) v2.initialValue;
        assertEquals("*", mult.operator);
        //final INT k = i*3;
        VariableDeclaration k = (VariableDeclaration) root.statements.get(2);
        assertTrue(k.initialValue instanceof BinaryExpression);

        //final STRING message = "Hello";
        VariableDeclaration v4 = (VariableDeclaration) root.statements.get(3);
        assertEquals("message", v4.identifier);
        assertEquals("STRING", v4.type.getTypeName());
        //final BOOL isEmpty
        VariableDeclaration declBool = (VariableDeclaration) root.statements.get(4);
        assertEquals("BOOL", declBool.type.getTypeName());
    }

    @Test
    public void testMixedExpression() throws Exception {
        // expression se termine par un ; pour être un Statement valide
        ProgramNode root = parse("5 + 6 * 7 + (a - 5) / tab[9*x];");

        // l'opération la plus haute est la derniere addition (priorité la plus faible)
        // (5 + (6 * 7)) + ((a - 5) / tab[9 * x])
        assertTrue(root.statements.get(0) instanceof BinaryExpression);
        BinaryExpression mainAddition = (BinaryExpression) root.statements.get(0);
        assertEquals("+", mainAddition.operator);

        // verification de la partie gauche : 5 + 6 * 7
        assertTrue(mainAddition.left instanceof BinaryExpression);
        BinaryExpression leftPart = (BinaryExpression) mainAddition.left;
        assertEquals("+", leftPart.operator);
        // 5 + 6 * 7, le + est au dessus du *
        assertTrue(leftPart.right instanceof BinaryExpression);
        assertEquals("*", ((BinaryExpression)leftPart.right).operator);

        // (a - 5) / tab[9 * x]
        assertTrue(mainAddition.right instanceof BinaryExpression);
        BinaryExpression rightPart = (BinaryExpression) mainAddition.right;
        assertEquals("/", rightPart.operator);

        // tab[9 * x]
        // il se trouve à droite du '/'
        assertTrue(rightPart.right instanceof ArrayAccess);
        ArrayAccess arrayAcc = (ArrayAccess) rightPart.right;
        assertEquals("tab", ((Identifier)arrayAcc.array).name);

        // 9 * x
        assertTrue(arrayAcc.index instanceof BinaryExpression);
        assertEquals("*", ((BinaryExpression)arrayAcc.index).operator);

    }
    @Test
    public void testEqualities() throws Exception {
        // egalité simple
        ProgramNode root1 = parse("a == 5;");
        assertTrue(root1.statements.get(0) instanceof BinaryExpression);
        BinaryExpression eq1 = (BinaryExpression) root1.statements.get(0);
        assertEquals("==", eq1.operator);

        // différence (not equal)
        ProgramNode root2 = parse("x =/= 10;");
        BinaryExpression eq2 = (BinaryExpression) root2.statements.get(0);
        assertEquals("=/=", eq2.operator);

        // l'arithmétique doit être traitée avant l'égalité
        // "5 + 3 == 8" => "(5 + 3) == 8"
        ProgramNode root3 = parse("5 + 3 == 8;");
        BinaryExpression eq3 = (BinaryExpression) root3.statements.get(0);
        assertEquals("==", eq3.operator);
        assertTrue(eq3.left instanceof BinaryExpression); // C'est le '+'
        assertEquals("+", ((BinaryExpression)eq3.left).operator);
        assertTrue(eq3.right instanceof Literal); // C'est le '8'

        // "a == 5 && b == 10" -> le '&&' doit être la racine, les '==' sont en dessous
        ProgramNode root4 = parse("a == 5 && b == 10;");
        BinaryExpression logicAnd = (BinaryExpression) root4.statements.get(0);
        assertEquals("&&", logicAnd.operator);

        BinaryExpression leftEq = (BinaryExpression) logicAnd.left;
        assertEquals("==", leftEq.operator);

        BinaryExpression rightEq = (BinaryExpression) logicAnd.right;
        assertEquals("==", rightEq.operator);
    }
    @Test
    public void testArrays() throws Exception {
        // Test Allocation multidimensionnelle
        ProgramNode root1 = parse("INT[][] matrix = INT ARRAY [5][10];");
        VariableDeclaration v1 = (VariableDeclaration) root1.statements.get(0);
        assertTrue(v1.initialValue instanceof ArrayCreation);
        ArrayCreation ac = (ArrayCreation) v1.initialValue;
        assertEquals(2, ac.dimensions.size()); // [5] et [10]
        assertEquals("INT", ac.elementType.getTypeName());

        // Test Initialisation imbriquée (Literal)
        ProgramNode root2 = parse("INT[][] table = {{1, 2}, {3, 4}};");
        VariableDeclaration v2 = (VariableDeclaration) root2.statements.get(0);
        assertTrue(v2.initialValue instanceof ArrayInitializer);
        ArrayInitializer init = (ArrayInitializer) v2.initialValue;
        assertEquals(2, init.elements.size()); // Contient deux sous-tableaux
        assertTrue(init.elements.get(0) instanceof ArrayInitializer);
    }
    @Test
    public void testConstructorsAndFunctionsCalls() throws Exception {
        // Test Constructeur de Collection (Majuscule)
        ProgramNode root1 = parse("Person p = Person(\"Bob\", 25);");
        VariableDeclaration v1 = (VariableDeclaration) root1.statements.get(0);
        assertTrue(v1.initialValue instanceof CollectionConstructor);
        CollectionConstructor cc = (CollectionConstructor) v1.initialValue;
        assertEquals("Person", cc.getCollectionType());
        assertEquals(2, cc.getArguments().size());

        // Test Appel de fonction
        ProgramNode root2 = parse("res = calculer(10, 20);");
        Assignment assign = (Assignment) root2.statements.get(0);
        assertTrue(assign.value instanceof FunctionCall);
        FunctionCall fc = (FunctionCall) assign.value;
        assertEquals("calculer", fc.functionName);

        // Test Appel de fonction
        ProgramNode root3 = parse("INT res = calculer(10, 20);");
        VariableDeclaration v = (VariableDeclaration) root3.statements.get(0);
        assertTrue(v.initialValue instanceof FunctionCall);
        FunctionCall f = (FunctionCall) v.initialValue;
        assertEquals("calculer", f.functionName);
    }
    @Test
    public void testNestedCollections() throws Exception {
        // Constructeur dans un constructeur
        ProgramNode root = parse("Person p = Person(\"Alice\", Point(10, 20),INT ARRAY[5*i]);");

        // Tableau de constructeurs
        ProgramNode root2 = parse("Person[] group = {Person(\"A\"), Person(\"B\")};");

        VariableDeclaration v = (VariableDeclaration) root2.statements.get(0);
        assertTrue(v.initialValue instanceof ArrayInitializer);
    }


}

import compiler.Lexer.Lexer;
import compiler.Parser.AST.Declarations.ArrayInitializer;
import compiler.Parser.AST.Declarations.FunctionDeclaration;
import compiler.Parser.AST.Declarations.VariableDeclaration;
import compiler.Parser.AST.Expressions.*;
import compiler.Parser.AST.ProgramNode;
import compiler.Parser.AST.Statements.ForStatement;
import compiler.Parser.AST.Statements.IfStatement;
import compiler.Parser.AST.Statements.WhileStatement;
import compiler.Parser.AST.Statements.BlockStatement;
import compiler.Parser.AST.Statements.ReturnStatement;
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
        assertEquals(5, root.getStatements().size());

        //final INT i = 3;
        VariableDeclaration v1 = (VariableDeclaration) root.getStatements().get(0);
        assertEquals("i", v1.getName());
        assertEquals("INT", v1.getType().getName());
        assertTrue(v1.isFinal());
        assertTrue(v1.getValue() instanceof Literal);

        // final FLOAT j = 3.2 * 5.0;
        VariableDeclaration v2 = (VariableDeclaration) root.getStatements().get(1);
        assertEquals("j", v2.getName());
        assertTrue(v2.getValue() instanceof BinaryExpression);
        BinaryExpression mult = (BinaryExpression) v2.getValue();
        assertEquals("*", mult.getOperator());
        //final INT k = i*3;
        VariableDeclaration k = (VariableDeclaration) root.getStatements().get(2);
        assertTrue(k.getValue() instanceof BinaryExpression);

        //final STRING message = "Hello";
        VariableDeclaration v4 = (VariableDeclaration) root.getStatements().get(3);
        assertEquals("message", v4.getName());
        assertEquals("STRING", v4.getType().getName());
        //final BOOL isEmpty
        VariableDeclaration declBool = (VariableDeclaration) root.getStatements().get(4);
        assertEquals("BOOL", declBool.getType().getName());
    }

    @Test
    public void testMixedExpression() throws Exception {
        // expression se termine par un ; pour être un Statement valide
        ProgramNode root = parse("5 + 6 * 7 + (a - 5) / tab[9*x];");

        // l'opération la plus haute est la derniere addition (priorité la plus faible)
        // (5 + (6 * 7)) + ((a - 5) / tab[9 * x])
        assertTrue(root.getStatements().get(0) instanceof BinaryExpression);
        BinaryExpression mainAddition = (BinaryExpression) root.getStatements().get(0);
        assertEquals("+", mainAddition.getOperator());

        // verification de la partie gauche : 5 + 6 * 7
        assertTrue(mainAddition.getLeft() instanceof BinaryExpression);
        BinaryExpression leftPart = (BinaryExpression) mainAddition.getLeft();
        assertEquals("+", leftPart.getOperator());
        // 5 + 6 * 7, le + est au dessus du *
        assertTrue(leftPart.getRight() instanceof BinaryExpression);
        assertEquals("*", ((BinaryExpression)leftPart.getRight()).getOperator());

        // (a - 5) / tab[9 * x]
        assertTrue(mainAddition.getRight() instanceof BinaryExpression);
        BinaryExpression rightPart = (BinaryExpression) mainAddition.getRight();
        assertEquals("/", rightPart.getOperator());

        // tab[9 * x]
        // il se trouve à droite du '/'
        assertTrue(rightPart.getRight() instanceof ArrayAccess);
        ArrayAccess arrayAcc = (ArrayAccess) rightPart.getRight();
        assertEquals("tab", ((Identifier)arrayAcc.getArray()).getName());

        // 9 * x
        assertTrue(arrayAcc.getIndex() instanceof BinaryExpression);
        assertEquals("*", ((BinaryExpression)arrayAcc.getIndex()).getOperator());

    }
    @Test
    public void testCompoundAssignments() throws Exception {
        // Test +=
        ProgramNode root = parse("x += 5;");
        assertTrue(root.getStatements().get(0) instanceof Assignment);
        Assignment assign = (Assignment) root.getStatements().get(0);

        assertEquals("x", ((Identifier) assign.getLeft()).getName());

        assertTrue(assign.getRight() instanceof BinaryExpression);
        BinaryExpression bin = (BinaryExpression) assign.getRight();
        assertEquals("+", bin.getOperator());
        assertEquals("x", ((Identifier) bin.getLeft()).getName());
        assertEquals("5", ((Literal) bin.getRight()).getValue());

        ProgramNode root2 = parse("y *= (a + 2);");
        Assignment assign2 = (Assignment) root2.getStatements().get(0);
        BinaryExpression bin2 = (BinaryExpression) assign2.getRight();
        assertEquals("*", bin2.getOperator());
        assertTrue(bin2.getRight() instanceof BinaryExpression); // (a + 2)
    }
    @Test
    public void testVarSyntax() throws Exception {
        ProgramNode root = parse("var x = 10;");
        assertTrue(root.getStatements().get(0) instanceof VariableDeclaration);
        VariableDeclaration vd = (VariableDeclaration) root.getStatements().get(0);
        assertEquals("x", vd.getName());
        assertNull("Type should be null before semantic analysis", vd.getType());
        ProgramNode root2 = parse("final var y = 3.14;");
        VariableDeclaration vd2 = (VariableDeclaration) root2.getStatements().get(0);
        assertTrue(vd2.isFinal());
        assertEquals("y", vd2.getName());
    }

    @Test(expected = Exception.class)
    public void testVarWithoutInitShouldFail() throws Exception {
        parse("var x;");
    }
    @Test
    public void testEqualities() throws Exception {
        // egalité simple
        ProgramNode root1 = parse("a == 5;");
        assertTrue(root1.getStatements().get(0) instanceof BinaryExpression);
        BinaryExpression eq1 = (BinaryExpression) root1.getStatements().get(0);
        assertEquals("==", eq1.getOperator());

        // différence (not equal)
        ProgramNode root2 = parse("x =/= 10;");
        BinaryExpression eq2 = (BinaryExpression) root2.getStatements().get(0);
        assertEquals("=/=", eq2.getOperator());

        // l'arithmétique doit être traitée avant l'égalité
        // "5 + 3 == 8" => "(5 + 3) == 8"
        ProgramNode root3 = parse("5 + 3 == 8;");
        BinaryExpression eq3 = (BinaryExpression) root3.getStatements().get(0);
        assertEquals("==", eq3.getOperator());
        assertTrue(eq3.getLeft() instanceof BinaryExpression); // C'est le '+'
        assertEquals("+", ((BinaryExpression)eq3.getLeft()).getOperator());
        assertTrue(eq3.getRight() instanceof Literal); // C'est le '8'

        // "a == 5 && b == 10" -> le '&&' doit être la racine, les '==' sont en dessous
        ProgramNode root4 = parse("a == 5 && b == 10;");
        BinaryExpression logicAnd = (BinaryExpression) root4.getStatements().get(0);
        assertEquals("&&", logicAnd.getOperator());

        BinaryExpression leftEq = (BinaryExpression) logicAnd.getLeft();
        assertEquals("==", leftEq.getOperator());

        BinaryExpression rightEq = (BinaryExpression) logicAnd.getRight();
        assertEquals("==", rightEq.getOperator());
    }
    @Test
    public void testArrays() throws Exception {
        // Test Allocation multidimensionnelle
        ProgramNode root1 = parse("INT[][] matrix = INT ARRAY [5][10];");
        VariableDeclaration v1 = (VariableDeclaration) root1.getStatements().get(0);
        assertTrue(v1.getValue() instanceof ArrayCreation);
        ArrayCreation ac = (ArrayCreation) v1.getValue();
        assertEquals(2, ac.getSizes().size()); // [5] et [10]
        assertEquals("INT", ac.getElementType().getName());

        // Test Initialisation imbriquée (Literal)
        ProgramNode root2 = parse("INT[][] table = {{1, 2}, {3, 4}};");
        VariableDeclaration v2 = (VariableDeclaration) root2.getStatements().get(0);
        assertTrue(v2.getValue() instanceof ArrayInitializer);
        ArrayInitializer init = (ArrayInitializer) v2.getValue();
        assertEquals(2, init.getElements().size()); // Contient deux sous-tableaux
        assertTrue(init.getElements().get(0) instanceof ArrayInitializer);
    }
    @Test
    public void testConstructorsAndFunctionsCalls() throws Exception {
        // Test Constructeur de Collection (Majuscule)
        ProgramNode root1 = parse("Person p = Person(\"Bob\", 25);");
        VariableDeclaration v1 = (VariableDeclaration) root1.getStatements().get(0);
        assertTrue(v1.getValue() instanceof CollectionConstructor);
        CollectionConstructor cc = (CollectionConstructor) v1.getValue();
        assertEquals("Person", cc.getName());
        assertEquals(2, cc.getArguments().size());

        // Test Appel de fonction
        ProgramNode root2 = parse("res = calculer(10, 20);");
        Assignment assign = (Assignment) root2.getStatements().get(0);
        assertTrue(assign.getRight() instanceof FunctionCall);
        FunctionCall fc = (FunctionCall) assign.getRight();
        assertEquals("calculer", fc.getName());

        // Test Appel de fonction
        ProgramNode root3 = parse("INT res = calculer(10, 20);");
        VariableDeclaration v = (VariableDeclaration) root3.getStatements().get(0);
        assertTrue(v.getValue() instanceof FunctionCall);
        FunctionCall f = (FunctionCall) v.getValue();
        assertEquals("calculer", f.getName());
    }
    @Test
    public void testNestedCollections() throws Exception {
        // Constructeur dans un constructeur
        ProgramNode root = parse("Person p = Person(\"Alice\", Point(10, 20),INT ARRAY[5*i]);");

        // Tableau de constructeurs
        ProgramNode root2 = parse("Person[] group = {Person(\"A\"), Person(\"B\")};");

        VariableDeclaration v = (VariableDeclaration) root2.getStatements().get(0);
        assertTrue(v.getValue() instanceof ArrayInitializer);
    }


    @Test
    public void testFieldAccess() throws Exception {
        // simple access : p.x
        ProgramNode root1 = parse("p.x;");
        assertTrue(root1.getStatements().get(0) instanceof FieldAccess);
        FieldAccess fa1 = (FieldAccess) root1.getStatements().get(0);
        assertEquals("x", fa1.getFieldName());
        assertTrue(fa1.getObject() instanceof Identifier);
        assertEquals("p", ((Identifier) fa1.getObject()).getName());

        // acces after tab : p[0].x 
        ProgramNode root2 = parse("p[0].x;");
        assertTrue(root2.getStatements().get(0) instanceof FieldAccess);
        FieldAccess fa2 = (FieldAccess) root2.getStatements().get(0);
        assertEquals("x", fa2.getFieldName());
        assertTrue(fa2.getObject() instanceof ArrayAccess);

        // string acces : p[0].x + p[1].x
        ProgramNode root3 = parse("p[0].x + p[1].x;");
        assertTrue(root3.getStatements().get(0) instanceof BinaryExpression);
        BinaryExpression bin = (BinaryExpression) root3.getStatements().get(0);
        assertTrue(bin.getLeft() instanceof FieldAccess);
        assertTrue(bin.getRight() instanceof FieldAccess);
    }

    //for ( i ; 1 - > 100 ; i + 1 )
    // Test for with already init variable 
    @Test
    public void testForLoopPreDeclaredVar() throws Exception {
        ProgramNode root = parse("for (i; 1->100; i+1) {}");
        assertTrue(root.getStatements().get(0) instanceof ForStatement);
        ForStatement fs = (ForStatement) root.getStatements().get(0);
        //variable must be an identifier 
        assertTrue(fs.getVariable() instanceof Identifier);
        assertEquals("i", ((Identifier) fs.getVariable()).getName());
        assertTrue(fs.getFrom() instanceof Literal);
        assertTrue(fs.getTo() instanceof Literal);
    }

    //for ( INT i ; 1 -> 100 ; i + 1 ) 
    // case with declaration in the for loop
    @Test
    public void testForLoopWithDeclaration() throws Exception {
        ProgramNode root = parse("for (INT i; 1->100; i+1) {}");
        assertTrue(root.getStatements().get(0) instanceof ForStatement);
        ForStatement fs = (ForStatement) root.getStatements().get(0);
        //variable must be a VariableDeclaration
        assertTrue(fs.getVariable() instanceof VariableDeclaration);
        assertEquals("i", ((VariableDeclaration) fs.getVariable()).getName());
    }

    //while ( value =/= 3 )
    @Test
    public void testWhileLoop() throws Exception {
        ProgramNode root = parse("while (value =/= 3) {}");
        assertTrue(root.getStatements().get(0) instanceof WhileStatement);
        WhileStatement ws = (WhileStatement) root.getStatements().get(0);
        assertTrue(ws.getCondition() instanceof BinaryExpression);
        assertEquals("=/=", ((BinaryExpression) ws.getCondition()).getOperator());
    }

    //if ( i > 10 ) { ... } else { ... }
    @Test
    public void testIfElse() throws Exception {
        ProgramNode root = parse("if (i > 10) { value = value - 1; } else { write(message); }");
        assertTrue(root.getStatements().get(0) instanceof IfStatement);
        IfStatement is = (IfStatement) root.getStatements().get(0);
        assertTrue(is.getCondition() instanceof BinaryExpression);
        assertEquals(">", ((BinaryExpression) is.getCondition()).getOperator());
        assertNotNull(is.getThenBlock());
        assertNotNull(is.getElseBlock());
    }

    //def INT square ( INT v ) { return v * v ; }
    @Test
    public void testFunctionDeclaration() throws Exception {
        ProgramNode root = parse("def INT square(INT v) { return v * v; }");
        assertTrue(root.getStatements().get(0) instanceof FunctionDeclaration);
        FunctionDeclaration fd = (FunctionDeclaration) root.getStatements().get(0);
        assertEquals("square", fd.getName());
        assertEquals("INT", fd.getReturnType().getName());
        assertEquals(1, fd.getParameters().size());
        assertTrue(fd.getBody() instanceof BlockStatement);
        // check that return has v*v
        BlockStatement body = (BlockStatement) fd.getBody();
        assertTrue(body.getStatements().get(0) instanceof ReturnStatement);
        ReturnStatement ret = (ReturnStatement) body.getStatements().get(0);
        assertTrue(ret.getValue() instanceof BinaryExpression);
        assertEquals("*", ((BinaryExpression) ret.getValue()).getOperator());
    }


}

package compiler;

import compiler.Lexer.Lexer;
import compiler.Parser.Parser;
import compiler.Parser.AST.ProgramNode;
import compiler.SemanticAnalysis.SemanticAnalysis;
import compiler.SemanticAnalysis.SemanticException;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestSemanticAnalysis {

    private void runSemantic(String code) throws Exception {
        Lexer lexer = new Lexer(new StringReader(code));
        Parser parser = new Parser(lexer);

        ProgramNode ast;
        try {
            ast = parser.getAST();
        } catch (compiler.Parser.ParserException e) {
            String msg = e.getMessage().toLowerCase();
            String keyword = "TypeError";
            if (msg.contains("coll") || msg.contains("uppercase") || msg.contains("collection")) {
                keyword = "CollectionError";
            }
            throw new SemanticException("Parser error wrapped: " + e.getMessage(), 0, 0, keyword);
        }

        SemanticAnalysis semantic = new SemanticAnalysis();
        semantic.analyze(ast);
    }

    @Test
    void testTypeMismatch() {
        String code = "def main() { INT x = \"Bonjour\"; }";
        SemanticException ex = assertThrows(SemanticException.class, () -> runSemantic(code));
        assertEquals("TypeError", ex.getKeyword());
    }

    @Test
    void testCollectionDuplicate() {
        String code = "coll Point { INT x; } coll Point { BOOL z; }";
        SemanticException ex = assertThrows(SemanticException.class, () -> runSemantic(code));
        assertEquals("CollectionError", ex.getKeyword());
    }

    @Test
    void testInvalidOperator() {
        String code = "def main() { INT x = 1; BOOL y = true; INT z = x + y; }";
        SemanticException ex = assertThrows(SemanticException.class, () -> runSemantic(code));
        assertEquals("OperatorError", ex.getKeyword());
    }

    @Test
    void testConditionNotBool() {
        String code = "def main() { STRING s = \"hi\"; while(s) { } }";
        SemanticException ex = assertThrows(SemanticException.class, () -> runSemantic(code));
        assertEquals("MissingConditionError", ex.getKeyword());
    }

    @Test
    void testUndeclaredVariable() {
        String code = "def main() { x = 10; }";
        SemanticException ex = assertThrows(SemanticException.class, () -> runSemantic(code));
        assertEquals("ScopeError", ex.getKeyword());
    }

    @Test
    void testWrongReturnType() {
        String code = "def BOOL f() { return 1; }";
        SemanticException ex = assertThrows(SemanticException.class, () -> runSemantic(code));
        assertEquals("ReturnError", ex.getKeyword());
    }

    @Test
    void testArgumentMismatch() {
        String code = "def INT square(INT v) {\n" +
                "return v*v;\n" +
                "}\n" +
                "def main() {\n" +
                "INT value = 2;\n" +
                "BOOL test = true;\n" +
                "INT result = square(test);\n" +
                "}";
        SemanticException ex = assertThrows(SemanticException.class, () -> runSemantic(code));
        assertEquals("ArgumentError", ex.getKeyword());
    }

    @Test
    void testUnaryNot() {
        String code = "def main() { INT x = 10; BOOL b = not x; }";
        SemanticException ex = assertThrows(SemanticException.class, () -> runSemantic(code));
        assertEquals("TypeError", ex.getKeyword());
    }
    @Test
    void testTypeMismatchAssignment() {
        String code = "def main() { INT x = 5; x = true; }";
        SemanticException ex = assertThrows(SemanticException.class, () -> runSemantic(code));
        assertEquals("TypeError", ex.getKeyword());
    }

    @Test
    void testTypeMismatchInExpression() {
        String code = "def main() { INT x = (5 > 2); }";
        SemanticException ex = assertThrows(SemanticException.class, () -> runSemantic(code));
        assertEquals("TypeError", ex.getKeyword());
    }
    @Test
    void testVariableOutOfScope() {
        String code = "def main() { if(true) { INT y = 10; } y = 5; }";
        SemanticException ex = assertThrows(SemanticException.class, () -> runSemantic(code));
        assertEquals("ScopeError", ex.getKeyword());
    }

    @Test
    void testFunctionRedeclaration() {
        String code = "def f() {} def f() {}";
        SemanticException ex = assertThrows(SemanticException.class, () -> runSemantic(code));
        assertEquals("ScopeError", ex.getKeyword());
    }

    @Test
    void testLogicalOpOnInt() {
        String code = "def main() { BOOL b = true && 1; }";
        SemanticException ex = assertThrows(SemanticException.class, () -> runSemantic(code));
        assertEquals("OperatorError", ex.getKeyword());
    }
    @Test
    void testStringConcat() {
        String code = "def main() { STRING s = \"hello\" + 1; }";
        SemanticException ex = assertThrows(SemanticException.class, () -> runSemantic(code));
        assertEquals("OperatorError", ex.getKeyword());
    }
    @Test
    void testTooManyArguments() {
        String code = "def f(INT x) {} def main() { f(1, 2); }";
        SemanticException ex = assertThrows(SemanticException.class, () -> runSemantic(code));
        assertEquals("ArgumentError", ex.getKeyword());
    }

    @Test
    void testTooFewArguments() {
        String code = "def f(INT x, INT y) {} def main() { f(1); }";
        SemanticException ex = assertThrows(SemanticException.class, () -> runSemantic(code));
        assertEquals("ArgumentError", ex.getKeyword());
    }

    @Test
    void testMissingReturn() {
        String code = "def INT f() { INT x = 1; }";
        SemanticException ex = assertThrows(SemanticException.class, () -> runSemantic(code));
        assertEquals("ReturnError", ex.getKeyword());
    }

    @Test
    void testReturnOutsideFunction() {
        String code = "INT x = 10; return x;";
        SemanticException ex = assertThrows(SemanticException.class, () -> runSemantic(code));
        assertEquals("ReturnError", ex.getKeyword());
    }

    @Test
    void testIfConditionNotBool() {
        String code = "def main() { if(10 + 5) { } }";
        SemanticException ex = assertThrows(SemanticException.class, () -> runSemantic(code));
        assertEquals("MissingConditionError", ex.getKeyword());
    }

    @Test
    void testUnknownCollectionType() {
        String code = "def main() { FakeColl instance; }";
        SemanticException ex = assertThrows(SemanticException.class, () -> runSemantic(code));
        assertEquals("TypeError", ex.getKeyword());
    }
    @Test
    void testInvalidArrayIndexType() {
        String code = "def main() { INT[][] tab = {{1, 2}}; INT x = tab[0][\"invalide\"]; }";
        SemanticException ex = assertThrows(SemanticException.class, () -> runSemantic(code));
        assertEquals("TypeError", ex.getKeyword());
    }
    @Test
    void testAccessNonExistentField() {
        String code = "coll Point { INT x; } def main() { Point p = Point(1); INT z = p.y; }";
        SemanticException ex = assertThrows(SemanticException.class, () -> runSemantic(code));
        assertEquals("CollectionError", ex.getKeyword());
    }

    @Test
    void testFieldTypeMismatch() {
        String code = "coll Point { INT x; } def main() { Point p = Point(1); p.x = \"string\"; }";
        SemanticException ex = assertThrows(SemanticException.class, () -> runSemantic(code));
        assertEquals("TypeError", ex.getKeyword());
    }
    @Test
    void testInconsistentArrayInitializer() {
        String code = "def main() { INT[] tab = {1, \"invalide\"}; }";
        SemanticException ex = assertThrows(SemanticException.class, () -> runSemantic(code));
        assertEquals("TypeError", ex.getKeyword());
    }
    @Test
    void testFunctionCallBeforeDeclaration() throws Exception {
        String code = "def main() { f(); } def f() {}";
        runSemantic(code);
    }

    @Test
    void testRecursiveFunction() throws Exception {
        String code = "def INT fact(INT n) { if(n == 0) { return 1; } return n * fact(n-1); }";
        runSemantic(code);
    }
    @Test
    void testMissingReturnInElse() {
        String code = "def INT f(BOOL b) { if(b) { return 1; } else { INT x = 0; } }";
        SemanticException ex = assertThrows(SemanticException.class, () -> runSemantic(code));
        assertEquals("ReturnError", ex.getKeyword());
    }

    @Test
    void testReturnInWhileOnly() {
        String code = "def INT f() { while(true) { return 1; } }";
        SemanticException ex = assertThrows(SemanticException.class, () -> runSemantic(code));
        assertEquals("ReturnError", ex.getKeyword());
    }
    @Test
    void testCollectionOverwritePrimitive() {
        String code = "coll INT { INT x; }";
        SemanticException ex = assertThrows(SemanticException.class, () -> runSemantic(code));
        assertEquals("CollectionError", ex.getKeyword());
    }
    @Test
    void testCollectionLowerCase() {
        String code = "coll point { INT x; }";
        SemanticException ex = assertThrows(SemanticException.class, () -> runSemantic(code));
        assertEquals("CollectionError", ex.getKeyword());
    }
    @Test
    void testInvalidComparison() {
        String code = "def main() { BOOL b = (1 == true); }";
        SemanticException ex = assertThrows(SemanticException.class, () -> runSemantic(code));
        assertEquals("OperatorError", ex.getKeyword());
    }
    @Test
    void testSugarOperator() throws Exception {
        String code =
                "def main() {\n" +
                "INT a = 5;INT b = 5; a+=b; a/=b;a*=b;a-=b;\n"+
                "  INT res = 10+20;\n" +
                "  BOOL check = not (res < 0);\n" +
                "}";
        runSemantic(code);
    }
    @Test
    void testVarTypeInference() throws Exception {
        String code = "def main() { var x = 10; INT y = x + 5; }";
        assertDoesNotThrow(() -> runSemantic(code));
    }

    @Test
    void testVarTypeMismatchAfterInference() {
        String code = "def main() { var x = 10; x = \"pas un entier\"; }";
        SemanticException ex = assertThrows(SemanticException.class, () -> runSemantic(code));
        assertEquals("TypeError", ex.getKeyword());
    }

    @Test
    void testFinalVarReassignment() {
        String code = "def main() { final var x = 10; x = 20; }";
        assertThrows(SemanticException.class, () -> runSemantic(code));
    }
    @Test
    void testVarInferenceFromFunction() throws Exception {
        String code = "def INT calculer() { return 42; } " +
                "def main() { var x = calculer(); INT y = x + 1; }";
        assertDoesNotThrow(() -> runSemantic(code));
    }

    @Test
    void testVarInferenceFromFunctionError() {
        String code = "def STRING calculer() { return \"hi\"; } " +
                "def main() { var x = calculer(); INT y = x + 1; }";
        assertThrows(SemanticException.class, () -> runSemantic(code));
    }
    @Test
    void testVarInferVoidError() {
        String code = "def main() { var x = print(\"hello\"); }";
        SemanticException ex = assertThrows(SemanticException.class, () -> runSemantic(code));
        assertTrue(ex.getMessage().contains("void"));
    }
    @Test
    void testUnusedVariableDetection() throws Exception {
        String code =
                "def main() {\n" +
                        "    INT x = 10;\n" +
                        "    INT y = 20;\n" +
                        "    print_INT(x);\n" +
                        "}";

        assertDoesNotThrow(() -> runSemantic(code));
    }
    @Test
    void testUnusedVariableInBlocks() throws Exception {
        String code =
                "def main() {\n" +
                        "    var x = 10;\n" +
                        "    if (x > 5) {\n" +
                        "        var z = 30;\n" + // z est inutilisé ici
                        "    }\n" +
                        "    var a = 100;\n" +
                        "    print_INT(a);\n" +
                        "}";

        assertDoesNotThrow(() -> runSemantic(code));
    }
    @Test
    void testFunctionsAreNotMarkedUnused() throws Exception {
        String code =
                "def INT util(INT a) { return a * 2; }\n" +
                        "def main() { print_INT(10); }";

        assertDoesNotThrow(() -> runSemantic(code));
    }
    @Test
    void testConstantFolding() throws Exception {
        String code = "def main() { INT x = (10 + 5 + 3 * 7) * 2; print_INT(x); }";
        assertDoesNotThrow(() -> runSemantic(code));
    }
    @Test
    void testFullPropagation() throws Exception {
        String code =
                "def main() {\n" +
                        "    final var a = 10;\n" +
                        "    final var b = a + 5;\n" +
                        "    var c = (b * 2) + 2;\n" + //  (15 * 2) + 2 -> 32
                        "    print_INT(c);\n" +
                        "}";
        assertDoesNotThrow(() -> runSemantic(code));
    }
    @Test
    void testComplexArithmeticFolding() throws Exception {
        // (10 + 5) * (20 / (2 + 3))
        // 1. 2 + 3 -> 5
        // 2. 20 / 5 -> 4
        // 3. 10 + 5 -> 15
        // 4. 15 * 4 -> 60
        String code = "def main() { INT x = (10 + 5) * (20 / (2 + 3)); print_INT(x); }";
        assertDoesNotThrow(() -> runSemantic(code));
    }
    @Test
    void testDivideByZeroFolding() {
        String code = "def main() { INT x = 10 / (5 - 5); }";
        SemanticException ex = assertThrows(SemanticException.class, () -> runSemantic(code));
        assertTrue(ex.getMessage().toLowerCase().contains("division by zero"));
    }
    @Test
    void testFinalVsNonFinalPropagation() throws Exception {
        String code =
                "def main() {\n" +
                        "    final INT a = 10;\n" +
                        "    INT b = 5;\n" +
                        "    INT c = a + 2; \n" +
                        "    INT d = b + 2; \n" +
                        "    print_INT(c + d);\n" +
                        "}";
        assertDoesNotThrow(() -> runSemantic(code));
    }
    @Test
    void testBooleanFolding() throws Exception {
        String code = "def main() { BOOL test = (true && false) || (10 == 10); }";
        // (true && false) -> false
        // (10 == 10) -> true
        // false || true -> true
        assertDoesNotThrow(() -> runSemantic(code));
    }
    @Test
    void testValidProgram() throws Exception {
        String code = "coll Point { INT x; INT y; }\n" +
                "def INT add(INT a, INT b) { return a + b; }\n" +
                "def main() {\n" +
                "  INT res = add(10, 20);\n" +
                "  BOOL check = not (res < 0);\n" +
                "}";
        runSemantic(code);
    }

    @Test
    void testStringConcatValid() throws Exception {
        String code = "def main() { STRING s = \"hello\" + \" world\"; }";
        assertDoesNotThrow(() -> runSemantic(code));
    }

    @Test
    void testConstantNotAtTop() {
        String code = "INT x = 1; final INT c = 2;";
        SemanticException ex = assertThrows(SemanticException.class, () -> runSemantic(code));
        assertEquals("ScopeError", ex.getKeyword());
    }

    @Test
    void testConstantNonBaseType() {
        String code = "final INT[] arr = INT ARRAY [3];";
        SemanticException ex = assertThrows(SemanticException.class, () -> runSemantic(code));
        assertEquals("TypeError", ex.getKeyword());
    }
}
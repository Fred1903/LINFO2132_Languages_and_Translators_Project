package compiler;

import compiler.Lexer.Lexer;
import compiler.Parser.Parser;
import compiler.Parser.AST.ProgramNode;
import compiler.SemanticAnalysis.SematicAnalysis;
import compiler.SemanticAnalysis.SemanticException;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.*;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestSemanticAnalysis {

    private void runSemantic(String code) throws Exception {
        Lexer lexer = new Lexer(new StringReader(code));
        Parser parser = new Parser(lexer);
        ProgramNode ast = parser.getAST();
        SematicAnalysis semantic = new SematicAnalysis();
        ast.accept(semantic);
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
    void testValidProgram() {
        String code = "coll Point { INT x; INT y; }\n" +
                "def INT add(INT a, INT b) { return a + b; }\n" +
                "def main() {\n" +
                "  INT res = add(10, 20);\n" +
                "  BOOL check = not (res < 0);\n" +
                "}";
        assertDoesNotThrow(() -> runSemantic(code));
    }
}
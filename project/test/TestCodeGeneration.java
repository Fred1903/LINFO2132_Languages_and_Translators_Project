import compiler.CodeGeneration.CodeGenerator;
import compiler.Lexer.Lexer;
import compiler.Parser.Parser;
import compiler.Parser.AST.ProgramNode;
import compiler.SemanticAnalysis.SemanticAnalysis;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.*;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;

public class TestCodeGeneration {

    // Compiles code, runs Program.class in a temp dir, returns trimmed stdout
    private String compileAndRun(String code) throws Exception {
        return compileAndRun(code, null);
    }

    private String compileAndRun(String code, String stdin) throws Exception {
        Path tempDir = Files.createTempDirectory("codegen-test-");
        try {
            Lexer lexer = new Lexer(new StringReader(code));
            Parser parser = new Parser(lexer);
            ProgramNode ast = parser.getAST();
            new SemanticAnalysis().analyze(ast);

            Path classFile = tempDir.resolve("Program.class");
            new CodeGenerator().generateCode(ast, classFile.toString());

            ProcessBuilder pb = new ProcessBuilder("java", "-cp", tempDir.toString(), "Program");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            if (stdin != null) {
                try (OutputStream os = process.getOutputStream()) {
                    os.write(stdin.getBytes());
                }
            }

            String output = new String(process.getInputStream().readAllBytes());
            process.waitFor();
            return output.trim().replace("\r\n", "\n");
        } finally {
            Files.walk(tempDir).sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        }
    }

    // ── Phase 4 basic features ──────────────────────────────────────────────

    @Test
    void testPrintln() throws Exception {
        assertEquals("Hello World", compileAndRun(
            "def main() { println(\"Hello World\"); }"));
    }

    @Test
    void testArithmeticInt() throws Exception {
        // (2 + 3) * 4 - 6 / 2 = 17
        assertEquals("17", compileAndRun(
            "def main() { println((2 + 3) * 4 - 6 / 2); }"));
    }

    @Test
    void testArithmeticMod() throws Exception {
        assertEquals("1", compileAndRun(
            "def main() { println(10 % 3); }"));
    }

    @Test
    void testFloatPromotion() throws Exception {
        assertEquals("2.5", compileAndRun(
            "def main() { FLOAT f = 1 + 1.5; println(f); }"));
    }

    @Test
    void testGlobalVariables() throws Exception {
        assertEquals("20", compileAndRun(
            "final INT x = 10;\nINT y = x * 2;\ndef main() { println(y); }"));
    }

    @Test
    void testIfElse() throws Exception {
        assertEquals("yes", compileAndRun(
            "def main() { INT x = 5; if (x > 3) { println(\"yes\"); } else { println(\"no\"); } }"));
    }

    @Test
    void testWhileLoop() throws Exception {
        assertEquals("3", compileAndRun(
            "def main() { INT i = 0; while (i < 3) { i = i + 1; } println(i); }"));
    }

    @Test
    void testForLoop() throws Exception {
        // sum = 1+2+3+4 = 10
        assertEquals("10", compileAndRun(
            "def main() { INT sum = 0; for (INT i; 1 -> 5; i + 1) { sum = sum + i; } println(sum); }"));
    }

    @Test
    void testFunction() throws Exception {
        assertEquals("49", compileAndRun(
            "def INT square(INT n) { return n * n; }\ndef main() { println(square(7)); }"));
    }

    @Test
    void testRecursion() throws Exception {
        assertEquals("120", compileAndRun(
            "def INT fact(INT n) { if (n == 0) { return 1; } return n * fact(n - 1); }\n" +
            "def main() { println(fact(5)); }"));
    }

    @Test
    void testArray() throws Exception {
        assertEquals("20", compileAndRun(
            "def main() { INT[] arr = INT ARRAY [3]; arr[0] = 10; arr[1] = 20; arr[2] = 30; println(arr[1]); }"));
    }

    @Test
    void testCollection() throws Exception {
        assertEquals("3\n7", compileAndRun(
            "coll Point { INT x; INT y; }\n" +
            "def main() { Point p = Point(3, 7); println(p.x); println(p.y); }"));
    }

    @Test
    void testBooleanAnd() throws Exception {
        assertEquals("no", compileAndRun(
            "def main() { BOOL b = true && false; if (b) { println(\"yes\"); } else { println(\"no\"); } }"));
    }

    @Test
    void testBooleanOr() throws Exception {
        assertEquals("yes", compileAndRun(
            "def main() { BOOL b = false || true; if (b) { println(\"yes\"); } else { println(\"no\"); } }"));
    }

    @Test
    void testNotOperator() throws Exception {
        assertEquals("no", compileAndRun(
            "def main() { BOOL b = not true; if (b) { println(\"yes\"); } else { println(\"no\"); } }"));
    }

    @Test
    void testLengthString() throws Exception {
        assertEquals("5", compileAndRun(
            "def main() { println(length(\"hello\")); }"));
    }

    @Test
    void testLengthArray() throws Exception {
        assertEquals("7", compileAndRun(
            "def main() { INT[] arr = INT ARRAY [7]; println(length(arr)); }"));
    }

    @Test
    void testFloorCeil() throws Exception {
        assertEquals("3\n4", compileAndRun(
            "def main() { println(floor(3.7)); println(ceil(3.2)); }"));
    }

    @Test
    void testReadInt() throws Exception {
        assertEquals("84", compileAndRun(
            "def main() { INT x = read_INT(); println(x * 2); }", "42\n"));
    }

    // ── Fixes applied in this session ──────────────────────────────────────

    @Test
    void testArrayInitializer() throws Exception {
        assertEquals("4\n6", compileAndRun(
            "def main() { INT[] arr = {4, 5, 6}; println(arr[0]); println(arr[2]); }"));
    }

    @Test
    void testStringConcat() throws Exception {
        assertEquals("Hello World", compileAndRun(
            "def main() { STRING s = \"Hello\" + \" World\"; println(s); }"));
    }

    @Test
    void testStringEquality() throws Exception {
        assertEquals("equal", compileAndRun(
            "def main() { STRING a = \"hello\"; STRING b = \"hello\";\n" +
            "if (a == b) { println(\"equal\"); } else { println(\"not equal\"); } }"));
    }

    @Test
    void testStringInequality() throws Exception {
        assertEquals("different", compileAndRun(
            "def main() { STRING a = \"foo\"; STRING b = \"bar\";\n" +
            "if (a =/= b) { println(\"different\"); } else { println(\"same\"); } }"));
    }

    @Test
    void testStringIndex() throws Exception {
        // 'H' = 72
        assertEquals("72", compileAndRun(
            "def main() { STRING s = \"Hello\"; println(s[0]); }"));
    }

    @Test
    void testPrintlnNoArgs() throws Exception {
        assertEquals("a\n\nb", compileAndRun(
            "def main() { println(\"a\"); println(); println(\"b\"); }"));
    }

    @Test
    void testStrFunction() throws Exception {
        // str(65) = "A"
        assertEquals("A", compileAndRun(
            "def main() { println(str(65)); }"));
    }
}
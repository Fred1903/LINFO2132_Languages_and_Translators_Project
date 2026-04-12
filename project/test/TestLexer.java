import static org.junit.Assert.*;
import org.junit.Test;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import compiler.Lexer.Lexer;
import compiler.Lexer.Symbol;

public class TestLexer {

    // Helper : retourne tous les tokens produits pour un input donne (EOF inclus)
    private List<Symbol> tokens(String input) {
        Lexer lexer = new Lexer(new StringReader(input));
        List<Symbol> result = new ArrayList<>();
        Symbol s;
        while ((s = lexer.getNextSymbol()).getType() != Lexer.SymbolType.EOF) {
            result.add(s);
        }
        result.add(s); // EOF
        return result;
    }

    @Test
    public void test() {
        String input = "var x int = 2;";
        StringReader reader = new StringReader(input);
        Lexer lexer = new Lexer(reader);
        assertNotNull(lexer.getNextSymbol());
    }

    //INT i = 3
    @Test
    public void testVariableDeclaration() {
        List<Symbol> toks = tokens("INT i = 3 ;");
        assertEquals(Lexer.SymbolType.TYPE,       toks.get(0).getType());
        assertEquals("INT",                        toks.get(0).getName());
        assertEquals(Lexer.SymbolType.IDENTIFIER,  toks.get(1).getType());
        assertEquals("i",                          toks.get(1).getName());
        assertEquals(Lexer.SymbolType.SYMBOL,      toks.get(2).getType());
        assertEquals("=",                          toks.get(2).getName());
        assertEquals(Lexer.SymbolType.INTEGER,     toks.get(3).getType());
        assertEquals("3",                          toks.get(3).getName());
        assertEquals(Lexer.SymbolType.SYMBOL,      toks.get(4).getType());
        assertEquals(";",                          toks.get(4).getName());
    }

    //final FLOAT j = 3.2 * 5.0 
    @Test
    public void testFinalFloatDeclaration() {
        List<Symbol> toks = tokens("final FLOAT j = 3.2 * 5.0 ;");
        assertEquals(Lexer.SymbolType.KEYWORD,  toks.get(0).getType());
        assertEquals("final",                   toks.get(0).getName());
        assertEquals(Lexer.SymbolType.TYPE,     toks.get(1).getType());
        assertEquals("FLOAT",                   toks.get(1).getName());
        assertEquals(Lexer.SymbolType.FLOAT,    toks.get(4).getType());
        assertEquals("3.2",                     toks.get(4).getName());
        assertEquals(Lexer.SymbolType.FLOAT,    toks.get(6).getType());
        assertEquals("5.0",                     toks.get(6).getName());
    }

    //final STRING message = \"Hello\";
    @Test
    public void testStringLiteral() {
        List<Symbol> toks = tokens("final STRING message = \"Hello\" ;");
        assertEquals(Lexer.SymbolType.STRING, toks.get(4).getType());
        assertEquals("Hello",                 toks.get(4).getName());
    }

    //final BOOL isEmpty = true
    @Test
    public void testBooleanLiteral() {
        List<Symbol> toks = tokens("final BOOL isEmpty = true ;");
        assertEquals(Lexer.SymbolType.TYPE,    toks.get(1).getType());
        assertEquals("BOOL",                   toks.get(1).getName());
        assertEquals(Lexer.SymbolType.BOOLEAN, toks.get(4).getType());
        assertEquals("true",                   toks.get(4).getName());
    }

    //coll Point { INT x ; INT y ; }
    @Test
    public void testCollectionDeclaration() {
        List<Symbol> toks = tokens("coll Point { INT x ; INT y ; }");
        assertEquals(Lexer.SymbolType.KEYWORD,          toks.get(0).getType());
        assertEquals("coll",                            toks.get(0).getName());
        assertEquals(Lexer.SymbolType.COLLECTION_TYPE,  toks.get(1).getType());
        assertEquals("Point",                           toks.get(1).getName());
        assertEquals(Lexer.SymbolType.TYPE,             toks.get(3).getType());
    }

    //Person d = Person ( \"me\" , Point ( 3 , 7 ) , INT ARRAY [ i * 2 ] ) 
    @Test
    public void testCollectionConstructorTokens() {
        List<Symbol> toks = tokens("Person d = Person ( \"me\" , Point ( 3 , 7 ) ) ;");
        assertEquals(Lexer.SymbolType.COLLECTION_TYPE, toks.get(0).getType());
        assertEquals("Person",                         toks.get(0).getName());
        assertEquals(Lexer.SymbolType.IDENTIFIER,      toks.get(1).getType());
        assertEquals(Lexer.SymbolType.COLLECTION_TYPE, toks.get(3).getType());
    }

    // p [ 0 ] . x 
    @Test
    public void testFieldAccessDot() {
        List<Symbol> toks = tokens("p [ 0 ] . x");
        assertEquals(Lexer.SymbolType.IDENTIFIER, toks.get(0).getType());
        assertEquals("p",                         toks.get(0).getName());
        assertEquals(Lexer.SymbolType.SYMBOL,     toks.get(4).getType());
        assertEquals(".",                          toks.get(4).getName());
        assertEquals(Lexer.SymbolType.IDENTIFIER, toks.get(5).getType());
        assertEquals("x",                          toks.get(5).getName());
    }

    // 1 -> 100
    @Test
    public void testArrowOperator() {
        List<Symbol> toks = tokens("1 -> 100");
        assertEquals(Lexer.SymbolType.SYMBOL, toks.get(1).getType());
        assertEquals("->",                    toks.get(1).getName());
    }

    // 1 - > 100
    @Test
    public void testArrowWithSpaces() {
        List<Symbol> toks = tokens("1 - > 100");
        assertEquals(3, toks.size() - 1); // 3 tokens avant EOF : 1, ->, 100
        assertEquals(Lexer.SymbolType.SYMBOL, toks.get(1).getType());
        assertEquals("->",                    toks.get(1).getName());
    }

    // value =/= 3
    @Test
    public void testNotEqualOperator() {
        List<Symbol> toks = tokens("value =/= 3");
        assertEquals(Lexer.SymbolType.SYMBOL, toks.get(1).getType());
        assertEquals("=/=",                   toks.get(1).getName());
    }

    // read_INT ( )
    @Test
    public void testIdentifierWithUnderscore() {
        List<Symbol> toks = tokens("read_INT ( )");
        assertEquals(Lexer.SymbolType.IDENTIFIER, toks.get(0).getType());
        assertEquals("read_INT",                  toks.get(0).getName());
    }

    // check that comments are ignored
    @Test
    public void testComment() {
        List<Symbol> toks = tokens("# this is a comment\nINT x ;");
        assertEquals(Lexer.SymbolType.TYPE,       toks.get(0).getType());
        assertEquals("INT",                        toks.get(0).getName());
        assertEquals(Lexer.SymbolType.IDENTIFIER,  toks.get(1).getType());
        assertEquals("x",                          toks.get(1).getName());
    }

    // checks that EOF is there at the end
    @Test
    public void testEOF() {
        List<Symbol> toks = tokens("");
        assertEquals(1, toks.size());
        assertEquals(Lexer.SymbolType.EOF, toks.get(0).getType());
    }

    // tests ==, <=, >=, &&, ||
    @Test
    public void testMultiCharOperators() {
        assertEquals("==",  tokens("a == b").get(1).getName());
        assertEquals("<=",  tokens("a <= b").get(1).getName());
        assertEquals(">=",  tokens("a >= b").get(1).getName());
        assertEquals("&&",  tokens("a && b").get(1).getName());
        assertEquals("||",  tokens("a || b").get(1).getName());
    }
}
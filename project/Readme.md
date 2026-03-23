# Compilation and execution : Lexer

The file `test.lang` is used as an input example: it contains a program that uses most of the language syntactic elements (types, variables, operators, comments, strings, etc.), allowing verification of the lexer behavior and lexical error handling.
## Compilation

From the `project` directory:

```bash
javac -d out src/main/java/compiler/Lexer/LexerException.java src/main/java/compiler/Lexer/Symbol.java src/main/java/compiler/Lexer/Lexer.java src/main/java/compiler/Compiler.java
```

## Execution

```bash
java -cp out compiler.Compiler -lexer src/main/java/compiler/test.lang
```

---

# Compilation and execution : Parser

## Compilation

From the `project` directory:

```bash
javac -d out -sourcepath src/main/java src/main/java/compiler/Compiler.java
```

## Execution

```bash
java -cp out compiler.Compiler -parser src/main/java/compiler/test.lang
```

## Alternative with Gradle

```bash
.\gradlew.bat run --args="-parser src/main/java/compiler/test.lang"
```


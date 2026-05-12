package compiler.CodeGeneration;

import compiler.Parser.AST.*;
import compiler.Parser.AST.Declarations.*;
import compiler.Parser.AST.Expressions.*;
import compiler.Parser.AST.Statements.*;
import compiler.Parser.AST.Types.*;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import static org.objectweb.asm.Opcodes.*;

import java.io.IOException;
import java.nio.file.*;

public class CodeGenerator implements ASTVisitor<Void> {

    private ClassWriter classWriter;
    private MethodVisitor currentMethodVisitor;

    public void generateCode(ProgramNode program, String outputPath) throws IOException {
        //Extract class name from the path, ex: test.class --> test
        String fileName = Paths.get(outputPath).getFileName().toString();
        String className = fileName.replace(".class", "");

        // Creates class with ASM
        classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        classWriter.visit(V1_8, ACC_PUBLIC, className, null, "java/lang/Object", null);

        // Traverse the AST
        program.accept(this);

        classWriter.visitEnd();

        //write the .class
        byte[] byteCode = classWriter.toByteArray();
        Path out = Paths.get(outputPath);
        if (out.getParent() != null) Files.createDirectories(out.getParent());
        Files.write(out, byteCode);
    }


    ////// Program //////
    @Override
    public Void visit(ProgramNode node) {
        for (ASTNode statement : node.getStatements()) {
            statement.accept(this);
        }
        return null;
    }

    ////// Declarations //////
    @Override
    public Void visit(FunctionDeclaration functionDeclaration) {
        // Create main method : public static void main(String[] args)
        currentMethodVisitor = classWriter.visitMethod(
            ACC_PUBLIC | ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null
        );
        currentMethodVisitor.visitCode();

        // Run through the body of the function
        functionDeclaration.getBody().accept(this);

        //end of method, return
        currentMethodVisitor.visitInsn(RETURN);
        currentMethodVisitor.visitMaxs(-1, -1);
        currentMethodVisitor.visitEnd();
        return null;
    }

    @Override public Void visit(FunctionParameter node)      { return null; }
    @Override public Void visit(VariableDeclaration node)    { return null; }
    @Override public Void visit(ArrayInitializer node)       { return null; }
    @Override public Void visit(CollectionDeclaration node)  { return null; }
    @Override public Void visit(CollectionField node)        { return null; }


    ////// Statements //////
    @Override
    public Void visit(BlockStatement blockStatement) {
        // Run through the body of the block
        for (ASTNode statement : blockStatement.getStatements()) {
            statement.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(FunctionCall functionCall) {
        if (functionCall.getName().equals("println")) {
            //Read the string from the AST
            String text = ((CharacterChain) functionCall.getArguments().get(0)).getChain().toString();
            //syso
            currentMethodVisitor.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            //text inside the syso
            currentMethodVisitor.visitLdcInsn(text);
            //println
            currentMethodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println",
                "(Ljava/lang/String;)V", false);
        }
        return null;
    }

    @Override public Void visit(IfStatement node)       { return null; }
    @Override public Void visit(WhileStatement node)    { return null; }
    @Override public Void visit(ForStatement node)      { return null; }
    @Override public Void visit(ReturnStatement node)   { return null; }


    ////// Expressions //////
    @Override public Void visit(Assignment node)             { return null; }
    @Override public Void visit(BinaryExpression node)       { return null; }
    @Override public Void visit(UnaryExpression node)        { return null; }
    @Override public Void visit(Literal node)                { return null; }
    @Override public Void visit(FloatLiteral node)           { return null; }
    @Override public Void visit(BooleanTerm node)            { return null; }
    @Override public Void visit(CharacterChain node)         { return null; }
    @Override public Void visit(Identifier node)             { return null; }
    @Override public Void visit(CollectionConstructor node)  { return null; }
    @Override public Void visit(ArrayAccess node)            { return null; }
    @Override public Void visit(ArrayCreation node)          { return null; }
    @Override public Void visit(FieldAccess node)            { return null; }
}

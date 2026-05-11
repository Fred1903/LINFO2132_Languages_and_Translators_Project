package compiler.CodeGeneration;

import compiler.Parser.AST.ASTNode;
import compiler.Parser.AST.ProgramNode;
import compiler.Parser.AST.Declarations.FunctionDeclaration;
import compiler.Parser.AST.Expressions.CharacterChain;
import compiler.Parser.AST.Expressions.FunctionCall;
import compiler.Parser.AST.Statements.BlockStatement;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import static org.objectweb.asm.Opcodes.*;

import java.io.IOException;
import java.nio.file.*;

public class CodeGenerator {

    public void generateCode(ProgramNode program, String outputPath) throws IOException {
        //Extract class name from the path, ex: test.class --> test
        String fileName = Paths.get(outputPath).getFileName().toString();
        String className = fileName.replace(".class", "");

        // Creates class with ASM
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        classWriter.visit(V1_8, ACC_PUBLIC | ACC_SUPER, className, null, "java/lang/Object", null);


        //search def main in AST and creates the content
        for (ASTNode statement : program.getStatements()) {
            if (statement instanceof FunctionDeclaration functionDeclaration && functionDeclaration.getName().equals("main")) {
                generateMain(classWriter, functionDeclaration);
            }
        }

        classWriter.visitEnd();

        //write the .class
        byte[] byteCode = classWriter.toByteArray();
        Path out = Paths.get(outputPath);
        if (out.getParent() != null) Files.createDirectories(out.getParent());
        Files.write(out, byteCode);
    }

    private void generateMain(ClassWriter classWriter, FunctionDeclaration fd) {
        MethodVisitor mainMethodVisitor = classWriter.visitMethod(
            ACC_PUBLIC | ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null
        );
        mainMethodVisitor.visitCode();

        // Run trough the body of the main
        BlockStatement body = (BlockStatement) fd.getBody();
        for (ASTNode statement : body.getStatements()) {
            if (statement instanceof FunctionCall functionCall && functionCall.getName().equals("println")) {
                //Read the string from the AST
                String text = ((CharacterChain) functionCall.getArguments().get(0)).getChain().toString();
                //syso
                mainMethodVisitor.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
                //text inside the syso
                mainMethodVisitor.visitLdcInsn(text);
                //println
                mainMethodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println","(Ljava/lang/String;)V", false);
            }
        }
        
        //end of method, return
        mainMethodVisitor.visitInsn(RETURN);
        mainMethodVisitor.visitMaxs(-1, -1); 
        mainMethodVisitor.visitEnd();
    }
}

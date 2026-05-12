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
import java.util.HashMap;
import java.util.Map;

public class CodeGenerator implements ASTVisitor<Void> {

    private ClassWriter classWriter;
    private MethodVisitor currentMethodVisitor;

    // JVM slot allocated to each local variable 
    private Map<String, Integer> localVariableSlots = new HashMap<>();
    private int nextVariableSlot = 0;

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
        //Re-init slots for each function 
        localVariableSlots = new HashMap<>();
        // Slot 0 = String[] args for main, so local var start at 1
        nextVariableSlot = 1;

        currentMethodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
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
    
    @Override
    public Void visit(VariableDeclaration node) {
        int variableSlot = nextVariableSlot++;
        localVariableSlots.put(node.getName(), variableSlot);
        
        if (node.getValue() != null) {
            node.getValue().accept(this);
        } 
        else {
            pushDefaultValue(node.getType());
        }
        storeVariable(node.getType(), variableSlot);
        return null;
    }
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
            ASTNode argument = functionCall.getArguments().get(0);
            currentMethodVisitor.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            argument.accept(this);
            // Check resolves type to do signature
            String printlnSignature = switch (argument.getResolvedType().getName()) {
                case "FLOAT"  -> "(F)V";
                case "BOOL"   -> "(Z)V";
                case "STRING" -> "(Ljava/lang/String;)V";
                default       -> "(I)V";
            };
            currentMethodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println",
                printlnSignature, false);
        }
        return null;
    }

    @Override public Void visit(IfStatement node)       { return null; }
    @Override public Void visit(WhileStatement node)    { return null; }
    @Override public Void visit(ForStatement node)      { return null; }
    @Override public Void visit(ReturnStatement node)   { return null; }


    ////// Expressions //////
    @Override public Void visit(Assignment node)             { return null; }
    @Override
    public Void visit(BinaryExpression node) {
        node.getLeft().accept(this);
        node.getRight().accept(this);
        // opération arithmétique correspondante
        switch (node.getOperator()) {
            case "+" -> currentMethodVisitor.visitInsn(IADD);
            case "-" -> currentMethodVisitor.visitInsn(ISUB);
            case "*" -> currentMethodVisitor.visitInsn(IMUL);
            case "/" -> currentMethodVisitor.visitInsn(IDIV);
            case "%" -> currentMethodVisitor.visitInsn(IREM);
        }
        return null;
    }
    @Override public Void visit(UnaryExpression node)        { return null; }
    @Override
    public Void visit(Literal node) {
        int value = Integer.parseInt(node.getValue());
        // Choisir instruction selon valeur de -1 à 5
        if (value >= -1 && value <= 5) currentMethodVisitor.visitInsn(ICONST_0 + value);
        else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) currentMethodVisitor.visitIntInsn(BIPUSH, value);
        else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) currentMethodVisitor.visitIntInsn(SIPUSH, value);
        else currentMethodVisitor.visitLdcInsn(value);
        return null;
    }
    @Override public Void visit(FloatLiteral node)           { return null; }
    @Override public Void visit(BooleanTerm node)            { return null; }

    @Override
    public Void visit(CharacterChain node) {
        currentMethodVisitor.visitLdcInsn(node.getChain().toString());
        return null;
    }

    @Override
    public Void visit(Identifier node) {
        int variableSlot = localVariableSlots.get(node.getName());
        loadVariable(node.getResolvedType(), variableSlot);
        return null;
    }
    @Override public Void visit(CollectionConstructor node)  { return null; }
    @Override public Void visit(ArrayAccess node)            { return null; }
    @Override public Void visit(ArrayCreation node)          { return null; }
    @Override public Void visit(FieldAccess node)            { return null; }


    ////// Helpers //////

    // ILOAD/FLOAD/ALOAD depends on type
    private void loadVariable(Type type, int slot) {
        if (type == PrimitiveType.FLOAT) currentMethodVisitor.visitVarInsn(FLOAD, slot);
        else if (type == PrimitiveType.INT || type == PrimitiveType.BOOL) currentMethodVisitor.visitVarInsn(ILOAD, slot);
        else currentMethodVisitor.visitVarInsn(ALOAD, slot); // String, Array, Collection
    }

    // ISTORE/FSTORE/ASTORE depends on type
    private void storeVariable(Type type, int slot) {
        if (type == PrimitiveType.FLOAT) currentMethodVisitor.visitVarInsn(FSTORE, slot);
        else if (type == PrimitiveType.INT || type == PrimitiveType.BOOL) currentMethodVisitor.visitVarInsn(ISTORE, slot);
        else currentMethodVisitor.visitVarInsn(ASTORE, slot); 
    }

    //Default value for non init variable
    private void pushDefaultValue(Type type) {
        if (type == PrimitiveType.FLOAT) currentMethodVisitor.visitInsn(FCONST_0);
        else if (type == PrimitiveType.INT || type == PrimitiveType.BOOL) currentMethodVisitor.visitInsn(ICONST_0);
        else currentMethodVisitor.visitInsn(ACONST_NULL);
    }
}

package compiler.CodeGeneration;

import compiler.Parser.AST.*;
import compiler.Parser.AST.Declarations.*;
import compiler.Parser.AST.Expressions.*;
import compiler.Parser.AST.Statements.*;
import compiler.Parser.AST.Types.*;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import static org.objectweb.asm.Opcodes.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

public class CodeGenerator implements ASTVisitor<Void> {

    private ClassWriter classWriter;
    private MethodVisitor currentMethodVisitor;
    private String className;

    // JVM slot allocated to each local variable 
    private Map<String, Integer> localVariableSlots = new HashMap<>();
    private int nextVariableSlot = 0;

    // All function declarations, pre-scanned before code generation
    // Needed to build INVOKESTATIC descriptors when calling user functions
    private Map<String, FunctionDeclaration> functionDeclarations = new HashMap<>();


    public void generateCode(ProgramNode program, String outputPath) throws IOException {
        //Extract class name from the path, ex: test.class --> test
        String fileName = Paths.get(outputPath).getFileName().toString();
        className = fileName.replace(".class", "");

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
        // collect all function declaration
        // so that INVOKESTATIC can find descriptor of function
        for (ASTNode statement : node.getStatements()) {
            if (statement instanceof FunctionDeclaration functionDeclaration) {
                functionDeclarations.put(functionDeclaration.getName(), functionDeclaration);
            }
        }
        // generate code
        for (ASTNode statement : node.getStatements()) {
            statement.accept(this);
        }
        return null;
    }


    ////// Declarations //////
    /// 
    @Override
    public Void visit(FunctionDeclaration functionDeclaration) {
        //Re-init slots for each function
        localVariableSlots = new HashMap<>();

        String methodName = functionDeclaration.getName();
        String descriptor = buildMethodDescriptor(functionDeclaration);

        currentMethodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_STATIC, methodName, descriptor, null, null);
        currentMethodVisitor.visitCode();

        if (methodName.equals("main")) {
             // Slot 0 = String[] args for main, so local var start at 1
            nextVariableSlot = 1;
        } 
        else {
            //params in the first slots 
            nextVariableSlot = 0;
            for (FunctionParameter functionParameter : functionDeclaration.getParameters()) {
                localVariableSlots.put(functionParameter.getName(), nextVariableSlot++);
            }
        }

        // Run through the body of the function
        functionDeclaration.getBody().accept(this);

        //end of method, return
        if (functionDeclaration.getReturnType() == null) {
            currentMethodVisitor.visitInsn(RETURN);
        }
        currentMethodVisitor.visitMaxs(-1, -1);
        currentMethodVisitor.visitEnd();
        return null;
    }

    @Override public Void visit(FunctionParameter node)     { return null; }

    @Override
    public Void visit(VariableDeclaration node) {
        int variableSlot = nextVariableSlot++;
        localVariableSlots.put(node.getName(), variableSlot);

        if (node.getValue() != null) {
            node.getValue().accept(this);
        } else {
            pushDefaultValue(node.getType());
        }
        storeVariable(node.getType(), variableSlot);
        return null;
    }

    @Override public Void visit(ArrayInitializer node) { return null; }
    @Override public Void visit(CollectionDeclaration node) { return null; }
    @Override public Void visit(CollectionField node) { return null; }


    ////// Statements //////
    @Override
    public Void visit(BlockStatement blockStatement) {
        for (ASTNode statement : blockStatement.getStatements()) {
            statement.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(ReturnStatement node) {
        if (node.getValue() == null) {
            currentMethodVisitor.visitInsn(RETURN);
        } 
        else {
            node.getValue().accept(this);
            pushReturnValue(node.getValue().getResolvedType());
        }
        return null;
    }

    @Override
    public Void visit(FunctionCall functionCall) {
        String name = functionCall.getName();

        if (name.equals("println")) {
            ASTNode argument = functionCall.getArguments().get(0);
            currentMethodVisitor.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            argument.accept(this);
            String printlnSignature = switch (argument.getResolvedType().getName()) {
                case "FLOAT"  -> "(F)V";
                case "BOOL"   -> "(Z)V";
                case "STRING" -> "(Ljava/lang/String;)V";
                default       -> "(I)V";
            };
            currentMethodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println",
                printlnSignature, false);

        } else if (functionDeclarations.containsKey(name)) {
            // User-defined function: push arguments then INVOKESTATIC
            for (ASTNode argument : functionCall.getArguments()) {
                argument.accept(this);
            }
            currentMethodVisitor.visitMethodInsn(INVOKESTATIC, className, name,
                buildMethodDescriptor(functionDeclarations.get(name)), false);
        }
        return null;
    }

    @Override
    public Void visit(IfStatement node) {
        Label elseLabel = new Label();
        Label endLabel  = new Label();
        node.getCondition().accept(this);
        // if =0 else jump
        currentMethodVisitor.visitJumpInsn(IFEQ, elseLabel);
        node.getThenBlock().accept(this);
        if (node.getElseBlock() != null) {
            currentMethodVisitor.visitJumpInsn(GOTO, endLabel);
            currentMethodVisitor.visitLabel(elseLabel);
            node.getElseBlock().accept(this);
            currentMethodVisitor.visitLabel(endLabel);
        } 
        else {
            currentMethodVisitor.visitLabel(elseLabel);
        }
        return null;
    }

    @Override
    public Void visit(WhileStatement node) {
        Label startLabel = new Label();
        Label endLabel   = new Label();
        currentMethodVisitor.visitLabel(startLabel);
        node.getCondition().accept(this);
        currentMethodVisitor.visitJumpInsn(IFEQ, endLabel);
        node.getWhileBlock().accept(this);
        currentMethodVisitor.visitJumpInsn(GOTO, startLabel);
        currentMethodVisitor.visitLabel(endLabel);
        return null;
    }

    @Override
    public Void visit(ForStatement node) {
        String variableName;
        Type variableType;
        int variableSlot;

        if (node.getVariable() instanceof VariableDeclaration variableDeclaration) {
            //new variable, allocate a slot and stock "from"
            variableSlot = nextVariableSlot++;
            localVariableSlots.put(variableDeclaration.getName(), variableSlot);
            variableName = variableDeclaration.getName();
            variableType = variableDeclaration.getType();
        } 
        else {
            //variable already declared, retrieve existing slot
            Identifier id = (Identifier) node.getVariable();
            variableName  = id.getName();
            variableType  = id.getResolvedType();
            variableSlot  = localVariableSlots.get(variableName);
        }
        node.getFrom().accept(this);
        storeVariable(variableType, variableSlot);


        // Loop/condition,   variable < toEndCondition
        Label startLabel = new Label();
        Label endLabel   = new Label();
        currentMethodVisitor.visitLabel(startLabel);
        loadVariable(variableType, variableSlot);
        node.getTo().accept(this);
        currentMethodVisitor.visitJumpInsn(IF_ICMPGE, endLabel);

        //body
        node.getBody().accept(this);

        // update 
        node.getUpdate().accept(this);
        storeVariable(variableType, variableSlot);
        currentMethodVisitor.visitJumpInsn(GOTO, startLabel);
        currentMethodVisitor.visitLabel(endLabel);
        return null;
    }

    @Override
    public Void visit(Assignment node) {
        if (node.getLeft() instanceof Identifier id) {
            node.getRight().accept(this);
            storeVariable(id.getResolvedType(), localVariableSlots.get(id.getName()));
        }
        return null;
    }


    ////// Expressions //////
    /// 
    
    @Override
    public Void visit(BinaryExpression node) {
        String operator = node.getOperator();

        // && and || 
        if (operator.equals("&&")) {
            Label falseLabel = new Label();
            Label endLabel   = new Label();
            node.getLeft().accept(this);
            currentMethodVisitor.visitJumpInsn(IFEQ, falseLabel);
            node.getRight().accept(this);
            currentMethodVisitor.visitJumpInsn(IFEQ, falseLabel);
            currentMethodVisitor.visitInsn(ICONST_1);
            currentMethodVisitor.visitJumpInsn(GOTO, endLabel);
            currentMethodVisitor.visitLabel(falseLabel);
            currentMethodVisitor.visitInsn(ICONST_0);
            currentMethodVisitor.visitLabel(endLabel);
            return null;
        }
        if (operator.equals("||")) {
            Label trueLabel = new Label();
            Label endLabel  = new Label();
            node.getLeft().accept(this);
            currentMethodVisitor.visitJumpInsn(IFNE, trueLabel);
            node.getRight().accept(this);
            currentMethodVisitor.visitJumpInsn(IFNE, trueLabel);
            currentMethodVisitor.visitInsn(ICONST_0);
            currentMethodVisitor.visitJumpInsn(GOTO, endLabel);
            currentMethodVisitor.visitLabel(trueLabel);
            currentMethodVisitor.visitInsn(ICONST_1);
            currentMethodVisitor.visitLabel(endLabel);
            return null;
        }

        // all other operators
        node.getLeft().accept(this);
        node.getRight().accept(this);
        switch (operator) {
            case "+"    -> currentMethodVisitor.visitInsn(IADD);
            case "-"   -> currentMethodVisitor.visitInsn(ISUB);
            case "*"   -> currentMethodVisitor.visitInsn(IMUL);
            case "/"   -> currentMethodVisitor.visitInsn(IDIV);
            case "%"   -> currentMethodVisitor.visitInsn(IREM);
            case "=="  -> emitComparison(IF_ICMPEQ);
            case "=/=" -> emitComparison(IF_ICMPNE);
            case "<"   -> emitComparison(IF_ICMPLT);
            case ">"   -> emitComparison(IF_ICMPGT);
            case "<="  -> emitComparison(IF_ICMPLE);
            case ">="  -> emitComparison(IF_ICMPGE);
        }
        return null;
    }

    @Override public Void visit(UnaryExpression node) { return null; }

    @Override
    public Void visit(Literal node) {
        int value = Integer.parseInt(node.getValue());
        if (value >= -1 && value <= 5) currentMethodVisitor.visitInsn(ICONST_0 + value);
        else if (value >= Byte.MIN_VALUE  && value <= Byte.MAX_VALUE) currentMethodVisitor.visitIntInsn(BIPUSH, value);
        else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) currentMethodVisitor.visitIntInsn(SIPUSH, value);
        else currentMethodVisitor.visitLdcInsn(value);
        return null;
    }

    @Override public Void visit(FloatLiteral node) { return null; }
    @Override public Void visit(BooleanTerm node) { return null; }

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

    @Override public Void visit(CollectionConstructor node) { return null; }
    @Override public Void visit(ArrayAccess node) { return null; }
    @Override public Void visit(ArrayCreation node) { return null; }
    @Override public Void visit(FieldAccess node) { return null; }


    ////// Helpers //////

    // ILOAD/FLOAD/ALOAD depending on type
    private void loadVariable(Type type, int slot) {
        if (type == PrimitiveType.FLOAT) currentMethodVisitor.visitVarInsn(FLOAD, slot);
        else if (type == PrimitiveType.INT || type == PrimitiveType.BOOL) currentMethodVisitor.visitVarInsn(ILOAD, slot);
        else currentMethodVisitor.visitVarInsn(ALOAD, slot);
    }

    // ISTORE/FSTORE/ASTORE depending on type
    private void storeVariable(Type type, int slot) {
        if (type == PrimitiveType.FLOAT) currentMethodVisitor.visitVarInsn(FSTORE, slot);
        else if (type == PrimitiveType.INT || type == PrimitiveType.BOOL) currentMethodVisitor.visitVarInsn(ISTORE, slot);
        else currentMethodVisitor.visitVarInsn(ASTORE, slot);
    }

    // IRETURN/FRETURN/ARETURN/RETURN depending on return type
    private void pushReturnValue(Type type) {
        if (type == PrimitiveType.FLOAT) currentMethodVisitor.visitInsn(FRETURN);
        else if (type == PrimitiveType.INT || type == PrimitiveType.BOOL) currentMethodVisitor.visitInsn(IRETURN);
        else if (type == null) currentMethodVisitor.visitInsn(RETURN);
        else currentMethodVisitor.visitInsn(ARETURN);
    }

    // Default value pushed on stack for uninitialized variables
    private void pushDefaultValue(Type type) {
        if (type == PrimitiveType.FLOAT) currentMethodVisitor.visitInsn(FCONST_0);
        else if (type == PrimitiveType.INT || type == PrimitiveType.BOOL) currentMethodVisitor.visitInsn(ICONST_0);
        else currentMethodVisitor.visitInsn(ACONST_NULL);
    }


    // Compares 2 ints already on stack, pushes 1 if true else 0 
    private void emitComparison(int jumpOpcode) {
        Label trueLabel = new Label();
        Label endLabel  = new Label();
        currentMethodVisitor.visitJumpInsn(jumpOpcode, trueLabel);
        currentMethodVisitor.visitInsn(ICONST_0);
        currentMethodVisitor.visitJumpInsn(GOTO, endLabel);
        currentMethodVisitor.visitLabel(trueLabel);
        currentMethodVisitor.visitInsn(ICONST_1);
        currentMethodVisitor.visitLabel(endLabel);
    }

    // returns JVM type descriptor: INT --> I, FLOAT --> F, ...
    private String getTypeDescriptor(Type type) {
        if (type == PrimitiveType.INT) return "I";
        if (type == PrimitiveType.FLOAT) return "F";
        if (type == PrimitiveType.BOOL) return "Z";
        if (type == PrimitiveType.STRING) return "Ljava/lang/String;";
        if (type instanceof ArrayType at) return "[" + getTypeDescriptor(at.getElementType());
        if (type instanceof CollectionType ct) return "L" + ct.getName() + ";";
        return "V";
    }

    // Build JVM method descriptor from a function declaration
    private String buildMethodDescriptor(FunctionDeclaration functionDeclaration) {
        if (functionDeclaration.getName().equals("main")) return "([Ljava/lang/String;)V";
        StringBuilder descriptor = new StringBuilder("(");
        for (FunctionParameter functionParameter : functionDeclaration.getParameters()) {
            descriptor.append(getTypeDescriptor(functionParameter.getType()));
        }
        descriptor.append(")");
        descriptor.append(functionDeclaration.getReturnType() == null ? "V" : getTypeDescriptor(functionDeclaration.getReturnType()));
        return descriptor.toString();
    }
}

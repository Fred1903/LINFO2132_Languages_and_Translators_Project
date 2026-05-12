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
import java.util.List;
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

    // Global variables/constants, stored as static fields
    private Map<String, Type> globalVariables = new HashMap<>();

    private Map<String, List<CollectionField>> collectionFields = new HashMap<>();

    // Output directory, used to write collection .class files alongside the main .class
    private String outputDir;


    public void generateCode(ProgramNode program, String outputPath) throws IOException {
        //Extract class name from the path, ex: test.class --> test
        String fileName = Paths.get(outputPath).getFileName().toString();
        className = fileName.replace(".class", "");
        Path parentDir = Paths.get(outputPath).getParent();
        outputDir = (parentDir != null) ? parentDir.toString() : ".";

        // Creates class with ASM
        classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        classWriter.visit(V1_8, ACC_PUBLIC, className, null, "java/lang/Object", null);
        classWriter.visitField(ACC_PRIVATE | ACC_STATIC, "$scanner", "Ljava/util/Scanner;", null, null).visitEnd();

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

        //collect all collection declarations 
        for (ASTNode statement : node.getStatements()) {
            if (statement instanceof CollectionDeclaration collectionDeclaration) {
                collectionFields.put(collectionDeclaration.getName(), collectionDeclaration.getFields());
            }
        }

        // declare global variables/constants as static fields
        for (ASTNode statement : node.getStatements()) {
            if (statement instanceof VariableDeclaration variableDeclaration) {
                int flags = ACC_PUBLIC | ACC_STATIC;
                if (variableDeclaration.isFinal()) flags |= ACC_FINAL;
                classWriter.visitField(flags, variableDeclaration.getName(), getTypeDescriptor(variableDeclaration.getType()), null, null).visitEnd();
                globalVariables.put(variableDeclaration.getName(), variableDeclaration.getType());
            }
        }

        // generate clinit to init globals/constants
        generateClinit(node);

        // generate code
        for (ASTNode statement : node.getStatements()) {
            if (statement instanceof FunctionDeclaration || statement instanceof CollectionDeclaration) {
                statement.accept(this);
            }
        }
        return null;
    }

    private void generateClinit(ProgramNode node) {
        MethodVisitor clinit = classWriter.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
        clinit.visitCode();
        MethodVisitor savedMethodVisitor = currentMethodVisitor;
        currentMethodVisitor = clinit;

        // Init Scanner (new Scanner(System.in))
        clinit.visitTypeInsn(NEW, "java/util/Scanner");
        clinit.visitInsn(DUP);
        clinit.visitFieldInsn(GETSTATIC, "java/lang/System", "in", "Ljava/io/InputStream;");
        clinit.visitMethodInsn(INVOKESPECIAL, "java/util/Scanner", "<init>", "(Ljava/io/InputStream;)V", false);
        clinit.visitFieldInsn(PUTSTATIC, className, "$scanner", "Ljava/util/Scanner;");

        // Init global variables
        for (ASTNode statement : node.getStatements()) {
            if (statement instanceof VariableDeclaration variableDeclaration && variableDeclaration.getValue() != null) {
                variableDeclaration.getValue().accept(this);
                currentMethodVisitor.visitFieldInsn(PUTSTATIC, className, variableDeclaration.getName(), getTypeDescriptor(variableDeclaration.getType()));
            }
        }

        clinit.visitInsn(RETURN);
        clinit.visitMaxs(-1, -1);
        clinit.visitEnd();
        currentMethodVisitor = savedMethodVisitor;
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

    @Override
    public Void visit(CollectionDeclaration node) {
        // Each collectiom generates its own .class file in the same directory as the main .class 
        ClassWriter collectionClassWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        collectionClassWriter.visit(V1_8, ACC_PUBLIC, node.getName(), null, "java/lang/Object", null);

        // Declare each field public
        for (CollectionField field : node.getFields()) {
            collectionClassWriter.visitField(ACC_PUBLIC, field.getName(), getTypeDescriptor(field.getType()), null, null).visitEnd();
        }

        // Default constructor, calls super() then return
        MethodVisitor init = collectionClassWriter.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(ALOAD, 0);
        init.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitInsn(RETURN);
        init.visitMaxs(-1, -1);
        init.visitEnd();

        collectionClassWriter.visitEnd();
        try {
            Files.write(Paths.get(outputDir, node.getName() + ".class"), collectionClassWriter.toByteArray());
        } 
        catch (IOException error) {
            throw new RuntimeException("Failed to write collection class: " + node.getName(), error);
        }
        return null;
    }

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

        } else if (name.equals("print")) {
            ASTNode argument = functionCall.getArguments().get(0);
            currentMethodVisitor.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            argument.accept(this);
            String printSignature = switch (argument.getResolvedType().getName()) {
                case "FLOAT"  -> "(F)V";
                case "BOOL"   -> "(Z)V";
                case "STRING" -> "(Ljava/lang/String;)V";
                default       -> "(I)V";
            };
            currentMethodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "print", printSignature, false);

        } else if (name.equals("print_INT")) {
            currentMethodVisitor.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            functionCall.getArguments().get(0).accept(this);
            currentMethodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(I)V", false);

        } else if (name.equals("print_FLOAT")) {
            currentMethodVisitor.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            functionCall.getArguments().get(0).accept(this);
            currentMethodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(F)V", false);

        } else if (name.equals("read_INT")) {
            currentMethodVisitor.visitFieldInsn(GETSTATIC, className, "$scanner", "Ljava/util/Scanner;");
            currentMethodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/util/Scanner", "nextInt", "()I", false);

        } else if (name.equals("read_FLOAT")) {
            currentMethodVisitor.visitFieldInsn(GETSTATIC, className, "$scanner", "Ljava/util/Scanner;");
            currentMethodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/util/Scanner", "nextFloat", "()F", false);

        } else if (name.equals("read_STRING")) {
            currentMethodVisitor.visitFieldInsn(GETSTATIC, className, "$scanner", "Ljava/util/Scanner;");
            currentMethodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/util/Scanner", "next", "()Ljava/lang/String;", false);

        } else if (name.equals("length")) {
            ASTNode argument = functionCall.getArguments().get(0);
            argument.accept(this);
            if (argument.getResolvedType() == PrimitiveType.STRING) {
                currentMethodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "length", "()I", false);
            } else {
                currentMethodVisitor.visitInsn(ARRAYLENGTH);
            }

        } else if (name.equals("floor")) {
            functionCall.getArguments().get(0).accept(this);
            currentMethodVisitor.visitInsn(F2D);
            currentMethodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "floor", "(D)D", false);
            currentMethodVisitor.visitInsn(D2I);

        } else if (name.equals("ceil")) {
            functionCall.getArguments().get(0).accept(this);
            currentMethodVisitor.visitInsn(F2D);
            currentMethodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "ceil", "(D)D", false);
            currentMethodVisitor.visitInsn(D2I);

        } else if (name.equals("str")) {
            functionCall.getArguments().get(0).accept(this);
            currentMethodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/String", "valueOf", "(I)Ljava/lang/String;", false);

        } else if (name.equals("not")) {
            functionCall.getArguments().get(0).accept(this);
            currentMethodVisitor.visitInsn(ICONST_1);
            currentMethodVisitor.visitInsn(IXOR);

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
        if (node.getLeft() instanceof Identifier identifier) {
            node.getRight().accept(this);
            String name = identifier.getName();
            if (localVariableSlots.containsKey(name)) {
                storeVariable(identifier.getResolvedType(), localVariableSlots.get(name));
            }
            else if (globalVariables.containsKey(name)) {
                currentMethodVisitor.visitFieldInsn(PUTSTATIC, className, name, getTypeDescriptor(globalVariables.get(name)));
            }
        }
        else if (node.getLeft() instanceof ArrayAccess arrayAccess) {
            arrayAccess.getArray().accept(this);
            arrayAccess.getIndex().accept(this);
            node.getRight().accept(this);
            storeArrayElement(getArrayElementType(arrayAccess.getArray()));
        }
        else if (node.getLeft() instanceof FieldAccess fieldAccess) {
            fieldAccess.getObject().accept(this);
            node.getRight().accept(this);
            String collectionName = fieldAccess.getObject().getResolvedType().getName();
            Type fieldType  = getFieldType(collectionName, fieldAccess.getFieldName());
            currentMethodVisitor.visitFieldInsn(PUTFIELD, collectionName, fieldAccess.getFieldName(), getTypeDescriptor(fieldType));
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

        // all other operators: check if float is involved
        Type leftType  = node.getLeft().getResolvedType();
        Type rightType = node.getRight().getResolvedType();
        boolean isFloat = leftType == PrimitiveType.FLOAT || rightType == PrimitiveType.FLOAT;

        node.getLeft().accept(this);
        if (isFloat && leftType == PrimitiveType.INT) currentMethodVisitor.visitInsn(I2F);
        node.getRight().accept(this);
        if (isFloat && rightType == PrimitiveType.INT) currentMethodVisitor.visitInsn(I2F);

        switch (operator) {
            // can not directly operate float and int, thereofre convert int in float if its the case
            case "+"   -> currentMethodVisitor.visitInsn(isFloat ? FADD : IADD);
            case "-"   -> currentMethodVisitor.visitInsn(isFloat ? FSUB : ISUB);
            case "*"   -> currentMethodVisitor.visitInsn(isFloat ? FMUL : IMUL);
            case "/"   -> currentMethodVisitor.visitInsn(isFloat ? FDIV : IDIV);
            case "%"   -> currentMethodVisitor.visitInsn(IREM);
            case "=="  -> { if (isFloat) pushFloatComparison("==");  else pushIntComparison(IF_ICMPEQ); }
            case "=/=" -> { if (isFloat) pushFloatComparison("=/="); else pushIntComparison(IF_ICMPNE); }
            case "<"   -> { if (isFloat) pushFloatComparison("<");   else pushIntComparison(IF_ICMPLT); }
            case ">"   -> { if (isFloat) pushFloatComparison(">");   else pushIntComparison(IF_ICMPGT); }
            case "<="  -> { if (isFloat) pushFloatComparison("<=");  else pushIntComparison(IF_ICMPLE); }
            case ">="  -> { if (isFloat) pushFloatComparison(">=");  else pushIntComparison(IF_ICMPGE); }
        }
        return null;
    }

    @Override
    public Void visit(UnaryExpression node) {
        node.getOperand().accept(this);
        switch (node.getOperator()) {
            //ineg inverse int sign, fneg same for float
            case "-"   -> currentMethodVisitor.visitInsn(node.getResolvedType() == PrimitiveType.FLOAT ? FNEG : INEG);
            case "not" -> { currentMethodVisitor.visitInsn(ICONST_1); currentMethodVisitor.visitInsn(IXOR); }
        }
        return null;
    }

    @Override
    public Void visit(Literal node) {
        int value = Integer.parseInt(node.getValue());
        if (value >= -1 && value <= 5) currentMethodVisitor.visitInsn(ICONST_0 + value);
        else if (value >= Byte.MIN_VALUE  && value <= Byte.MAX_VALUE) currentMethodVisitor.visitIntInsn(BIPUSH, value);
        else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) currentMethodVisitor.visitIntInsn(SIPUSH, value);
        else currentMethodVisitor.visitLdcInsn(value);
        return null;
    }

    @Override
    public Void visit(FloatLiteral node) {
        currentMethodVisitor.visitLdcInsn(Float.parseFloat(node.getValue()));
        return null;
    }

    @Override
    public Void visit(BooleanTerm node) {
        //Bool is represented like int, JVM : 1=true and 0=false
        Object value = node.getValue();
        // checks if value is a boolean, then check if has value "true" and stocks it as boolean
        boolean isTrue = (value instanceof Boolean bool) ? bool : "true".equals(value.toString());
        currentMethodVisitor.visitInsn(isTrue ? ICONST_1 : ICONST_0);
        return null;
    }

    @Override
    public Void visit(CharacterChain node) {
        currentMethodVisitor.visitLdcInsn(node.getChain().toString());
        return null;
    }

    @Override
    public Void visit(Identifier node) {
        String name = node.getName();
        if (localVariableSlots.containsKey(name)) {
            loadVariable(node.getResolvedType(), localVariableSlots.get(name));
        } 
        else if (globalVariables.containsKey(name)) {
            currentMethodVisitor.visitFieldInsn(GETSTATIC, className, name, getTypeDescriptor(globalVariables.get(name)));
        }
        return null;
    }

    @Override
    public Void visit(CollectionConstructor node) {
        String collectionName = node.getName();
        List<CollectionField> fields = collectionFields.get(collectionName);

        currentMethodVisitor.visitTypeInsn(NEW, collectionName);
        currentMethodVisitor.visitInsn(DUP);
        currentMethodVisitor.visitMethodInsn(INVOKESPECIAL, collectionName, "<init>", "()V", false);

        for (int i = 0; i < node.getArguments().size(); i++) {
            currentMethodVisitor.visitInsn(DUP); // keep ref for next PUTFIELD
            node.getArguments().get(i).accept(this);
            CollectionField field = fields.get(i);
            currentMethodVisitor.visitFieldInsn(PUTFIELD, collectionName, field.getName(), getTypeDescriptor(field.getType()));
        }
        return null;
    }

    @Override
    public Void visit(ArrayAccess node) {
        node.getArray().accept(this);  
        node.getIndex().accept(this); 
        loadArrayElement(getArrayElementType(node.getArray()));
        return null;
    }

    @Override
    public Void visit(ArrayCreation node) {
        node.getSizes().get(0).accept(this); // push size
        Type elementType = node.getElementType();
        if (elementType == PrimitiveType.INT) currentMethodVisitor.visitIntInsn(NEWARRAY, T_INT);
        else if (elementType == PrimitiveType.FLOAT) currentMethodVisitor.visitIntInsn(NEWARRAY, T_FLOAT);
        else if (elementType == PrimitiveType.BOOL) currentMethodVisitor.visitIntInsn(NEWARRAY, T_BOOLEAN);
        else currentMethodVisitor.visitTypeInsn(ANEWARRAY, internalName(elementType));
        return null;
    }

    @Override
    public Void visit(FieldAccess node) {
        // p.x --> push object reference, then GETFIELD collName.fieldName descriptor
        node.getObject().accept(this);
        String collectionName = node.getObject().getResolvedType().getName();
        Type fieldType  = getFieldType(collectionName, node.getFieldName());
        currentMethodVisitor.visitFieldInsn(GETFIELD, collectionName, node.getFieldName(), getTypeDescriptor(fieldType));
        return null;
    }


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
    private void pushIntComparison(int jumpOpcode) {
        Label trueLabel = new Label();
        Label endLabel  = new Label();
        currentMethodVisitor.visitJumpInsn(jumpOpcode, trueLabel);
        currentMethodVisitor.visitInsn(ICONST_0);
        currentMethodVisitor.visitJumpInsn(GOTO, endLabel);
        currentMethodVisitor.visitLabel(trueLabel);
        currentMethodVisitor.visitInsn(ICONST_1);
        currentMethodVisitor.visitLabel(endLabel);
    }

    // IALOAD/FALOAD/BALOAD/AALOAD depending on element type
    private void loadArrayElement(Type elementType) {
        if(elementType == PrimitiveType.INT) currentMethodVisitor.visitInsn(IALOAD);
        else if (elementType == PrimitiveType.FLOAT) currentMethodVisitor.visitInsn(FALOAD);
        else if (elementType == PrimitiveType.BOOL) currentMethodVisitor.visitInsn(BALOAD);
        else currentMethodVisitor.visitInsn(AALOAD);
    }

    // IASTORE/FASTORE/BASTORE/AASTORE depending on element type
    private void storeArrayElement(Type elementType) {
        if (elementType == PrimitiveType.INT) currentMethodVisitor.visitInsn(IASTORE);
        else if (elementType == PrimitiveType.FLOAT) currentMethodVisitor.visitInsn(FASTORE);
        else if (elementType == PrimitiveType.BOOL) currentMethodVisitor.visitInsn(BASTORE);
        else currentMethodVisitor.visitInsn(AASTORE);
    }

    // Returns element type of array node
    private Type getArrayElementType(ASTNode arrayNode) {
        if (arrayNode.getResolvedType() instanceof ArrayType arrayType) return arrayType.getElementType();
        return PrimitiveType.INT;
    }

    // Returns type of a field in collection 
    private Type getFieldType(String collectionName, String fieldName) {
        List<CollectionField> fields = collectionFields.get(collectionName);
        if (fields != null) {
            for (CollectionField field : fields) {
                if (field.getName().equals(fieldName)) return field.getType();
            }
        }
        return PrimitiveType.INT;
    }

    // Internal jvm class name for reference types
    private String internalName(Type type) {
        if (type == PrimitiveType.STRING) return "java/lang/String";
        if (type instanceof CollectionType collectionType) return collectionType.getName();
        return "java/lang/Object";
    }

    // Float comparison: two floats already on stack → FCMPL pushes -1/0/1, then jump
    private void pushFloatComparison(String op) {
        currentMethodVisitor.visitInsn(FCMPL);
        Label trueLabel = new Label();
        Label endLabel  = new Label();
        switch (op) {
            case "=="  -> currentMethodVisitor.visitJumpInsn(IFEQ, trueLabel);
            case "=/=" -> currentMethodVisitor.visitJumpInsn(IFNE, trueLabel);
            case "<"   -> currentMethodVisitor.visitJumpInsn(IFLT, trueLabel);
            case ">"   -> currentMethodVisitor.visitJumpInsn(IFGT, trueLabel);
            case "<="  -> currentMethodVisitor.visitJumpInsn(IFLE, trueLabel);
            case ">="  -> currentMethodVisitor.visitJumpInsn(IFGE, trueLabel);
        }
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

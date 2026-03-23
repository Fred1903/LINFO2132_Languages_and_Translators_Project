package compiler.Parser.AST.Types;

/**
 * Represente un type tableau.
 * Ex : "INT[]" -> ArrayType(PrimitiveType.INT, 1)
 *      "INT[][]" -> ArrayType(PrimitiveType.INT, 2)
 * elementType peut aussi etre un CollectionType : "Point[]"
 */
public class ArrayType implements Type {
    private final Type elementType;
    private final int dimensions;

    public ArrayType(Type elementType, int dimensions) {
        this.elementType = elementType;
        this.dimensions = dimensions;
    }

    public Type getElementType() { return elementType; }
    public int getDimensions()   { return dimensions; }

    @Override
    public String getName() {
        return elementType.getName() + "[]".repeat(dimensions);
    }
}

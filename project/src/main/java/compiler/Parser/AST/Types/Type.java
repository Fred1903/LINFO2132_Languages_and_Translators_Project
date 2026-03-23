package compiler.Parser.AST.Types;

/**
 * Interface commune a tous les types du langage.
 * Permet de manipuler uniformement PrimitiveType, CollectionType et ArrayType
 * sans connaitre leur nature exacte 
 */
public interface Type {
    String getName();
}

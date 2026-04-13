#!/bin/bash

# Dossier contenant tes tests
TEST_DIR="/home/fder/Bureau/lt_project/project/src/main/java/compiler/tests_semantic"

# Liste des fichiers à tester
FILES=("test_type.lang" "test_coll.lang" "test_operator.lang" "test_arg_error.lang" "test_cond.lang" "test_return.lang" "test_scope.lang")

echo "--- Démarrage de l'analyse sémantique ---"

for file in "${FILES[@]}"
do
    echo "---------------------------------------"
    echo "Exécution de : $file"
    # On lance la commande gradle
    ./gradlew run --args="-semantic $TEST_DIR/$file"
    echo ""
done

echo "--- Fin des tests ---"
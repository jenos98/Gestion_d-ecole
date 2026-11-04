#!/bin/bash
# scripts/run.sh — Lancement de l'application de gestion scolaire avec ses dépendances
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Détection de Java 21
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA_CMD="$JAVA_HOME/bin/java"
elif command -v java >/dev/null 2>&1; then
    JAVA_CMD="java"
elif [ -d "/home/jenos/.local/share/jdk-21" ]; then
    JAVA_CMD="/home/jenos/.local/share/jdk-21/bin/java"
else
    echo "Erreur : Java n'est pas installé sur ce système." >&2
    exit 1
fi

JAR_FILE="$PROJECT_ROOT/target/gestion-scolaire-1.0.0.jar"
LIB_DIR="$PROJECT_ROOT/target/lib"

if [ ! -f "$JAR_FILE" ]; then
    echo "Le fichier $JAR_FILE est introuvable."
    echo "Veuillez d'abord compiler et packager le projet avec : ./mvnw clean package"
    exit 1
fi

echo "Démarrage de Gestion Scolaire avec $($JAVA_CMD -version 2>&1 | head -n 1)..."
exec "$JAVA_CMD" -jar "$JAR_FILE" "$@"

#!/bin/bash
# Wrapper Maven pour le projet Gestion Scolaire
set -e

# Configuration du JDK 21
if [ -d "/home/jenos/.local/share/jdk-21" ]; then
    export JAVA_HOME="/home/jenos/.local/share/jdk-21"
    export PATH="$JAVA_HOME/bin:$PATH"
fi

# Localisation du binaire Maven
if [ -f "/home/jenos/.local/share/apache-maven-3.9.9/bin/mvn" ]; then
    MAVEN_BIN="/home/jenos/.local/share/apache-maven-3.9.9/bin/mvn"
elif command -v mvn >/dev/null 2>&1; then
    MAVEN_BIN="$(command -v mvn)"
else
    echo "Erreur : Maven n'est pas trouvé dans ~/.local/share/apache-maven-3.9.9 ni dans le PATH." >&2
    exit 1
fi

exec "$MAVEN_BIN" "$@"

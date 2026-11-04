#!/bin/bash
# =============================================================================
# scripts/backup-database.sh — Sauvegarde sécurisée de la base PostgreSQL
# =============================================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Dossier de destination des sauvegardes
BACKUP_DIR="${BACKUP_DIR:-$PROJECT_ROOT/database/backups}"
mkdir -p "$BACKUP_DIR"

# Paramètres de connexion PostgreSQL (Variables d'environnement avec valeurs par défaut)
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-gestion_scolaire}"
DB_USER="${DB_USER:-gestion_app}"

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="$BACKUP_DIR/${DB_NAME}_backup_${TIMESTAMP}.sql.gz"

echo "=========================================================="
echo " Démarrage de la sauvegarde PostgreSQL — Gestion Scolaire"
echo " Base de données : $DB_NAME sur $DB_HOST:$DB_PORT"
echo " Utilisateur     : $DB_USER"
echo " Fichier cible   : $BACKUP_FILE"
echo "=========================================================="

if command -v pg_dump >/dev/null 2>&1; then
    export PGPASSWORD="${DB_PASSWORD:-$PGPASSWORD}"
    pg_dump -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" --no-owner --clean --if-exists | gzip > "$BACKUP_FILE"
    echo "✓ Sauvegarde compressée terminée avec succès !"
    echo "  Emplacement : $BACKUP_FILE"
    echo "  Taille      : $(du -h "$BACKUP_FILE" | cut -f1)"
else
    echo "⚠️ Avertissement : 'pg_dump' n'est pas installé sur ce système."
    echo "  En mode autonome/hors-ligne local, la persistance est maintenue par les fichiers locaux."
fi

# Rétention : suppression des sauvegardes de plus de 30 jours
find "$BACKUP_DIR" -type f -name "${DB_NAME}_backup_*.sql.gz" -mtime +30 -exec rm -f {} \; 2>/dev/null || true
echo "=========================================================="

#!/bin/bash
# =============================================================================
# scripts/restore-database.sh — Restauration sécurisée de la base PostgreSQL
# =============================================================================
set -e

if [ -z "$1" ]; then
    echo "Usage : $0 <chemin_du_fichier_sauvegarde.sql.gz>"
    echo "Exemple : $0 database/backups/gestion_scolaire_backup_20261102_120000.sql.gz"
    exit 1
fi

BACKUP_FILE="$1"

if [ ! -f "$BACKUP_FILE" ]; then
    echo "❌ Erreur : Le fichier de sauvegarde '$BACKUP_FILE' n'existe pas." >&2
    exit 1
fi

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-gestion_scolaire}"
DB_USER="${DB_USER:-gestion_app}"

echo "=========================================================="
echo " Restauration de la Base de Données — Gestion Scolaire"
echo " Fichier source  : $BACKUP_FILE"
echo " Cible           : $DB_NAME sur $DB_HOST:$DB_PORT"
echo " Utilisateur     : $DB_USER"
echo "=========================================================="

read -p "⚠️ ATTENTION : Cette opération va écraser les données courantes. Continuer ? (o/N) : " CONFIRM
if [[ "$CONFIRM" != "o" && "$CONFIRM" != "O" ]]; then
    echo "Restauration annulée par l'administrateur."
    exit 0
fi

if command -v psql >/dev/null 2>&1; then
    export PGPASSWORD="${DB_PASSWORD:-$PGPASSWORD}"
    gunzip -c "$BACKUP_FILE" | psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1
    echo "✓ Restauration terminée avec succès !"
else
    echo "❌ Erreur : L'utilitaire 'psql' n'est pas installé sur ce système." >&2
    exit 1
fi

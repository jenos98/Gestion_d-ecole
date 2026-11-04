# Guide de Déploiement, Installation et Exploitation — Gestion Scolaire v1.0.0 LTS

## 1. Vue d'Ensemble
**Gestion Scolaire** est une application desktop d'entreprise développée en Java 21 LTS et JavaFX, conçue pour opérer en autonomie complète (100% hors-ligne) avec persistance PostgreSQL et cloisonnement financier strict (USD / CDF).

---

## 2. Prérequis Système
- **Système d'exploitation** : Linux (Ubuntu 22.04+, Debian 12+) ou Windows 10/11 (64-bit).
- **Java Runtime** : Java 21 LTS (OpenJDK / Eclipse Temurin / Adoptium).
- **Base de données** : PostgreSQL 14+ (recommandé 15 ou 16).
- **Mémoire Vive (RAM)** : 4 Go minimum (8 Go recommandés).
- **Espace Disque** : 500 Mo pour l'application et les bibliothèques, 10 Go pour les archives et sauvegardes.

---

## 3. Installation et Configuration de la Base de Données

### A. Création de la base et de l'utilisateur PostgreSQL
```bash
# Connexion en tant qu'administrateur postgres
sudo -u postgres psql

# Création de l'utilisateur dédié et de la base
CREATE USER gestion_app WITH PASSWORD 'votre_mot_de_passe_securise';
CREATE DATABASE gestion_scolaire OWNER gestion_app;
GRANT ALL PRIVILEGES ON DATABASE gestion_scolaire TO gestion_app;
\q
```

### B. Exécution des migrations du schéma
```bash
# Injection du schéma initial
psql -h localhost -p 5432 -U gestion_app -d gestion_scolaire -f database/migrations/V001__initial_schema.sql
```

### C. Variables d'Environnement (Recommandé en Production)
Définir dans `/etc/environment` ou dans votre profil utilisateur :
```bash
export DB_URL="jdbc:postgresql://localhost:5432/gestion_scolaire"
export DB_USER="gestion_app"
export DB_PASSWORD="votre_mot_de_passe_securise"
```

---

## 4. Compilation et Packaging de l'Application

### Compilation Maven :
```bash
JAVA_HOME=/home/jenos/.local/share/jdk-21 ./mvnw clean package
```

Le fichier JAR exécutable autonome est généré sous :
`target/gestion-scolaire-1.0.0.jar`
Les bibliothèques tierces associées sont copiées dans `target/lib/`.

---

## 5. Démarrage de l'Application

### Via le script officiel :
```bash
./scripts/run.sh
```

### Ou en ligne de commande directe :
```bash
java -jar target/gestion-scolaire-1.0.0.jar
```

---

## 6. Procédure de Sauvegarde et Restauration

### A. Sauvegarde quotidienne (Automatisable via Crontab)
```bash
./scripts/backup-database.sh
```
- Produit une archive compressée `.sql.gz` dans `database/backups/`.
- Purge automatiquement les archives datant de plus de 30 jours.

### B. Restauration d'urgence (Plan de Reprise d'Activité)
```bash
./scripts/restore-database.sh database/backups/gestion_scolaire_backup_YYYYMMDD_HHMMSS.sql.gz
```
- Demande une confirmation explicite avant d'écraser la base.
- Décompresse et réinjecte l'intégralité des tables et transactions en mode transactionnel sécurisé.

---

## 7. Règles d'Exploitation & Sécurité
1. **Règle d'or de clôture** : La caisse journalière ne peut jamais être fermée sans impression préalable du rapport d'activité.
2. **Cloisonnement des devises** : Aucun croisement monétaire ou conversion implicite USD/CDF n'est autorisé.
3. **Protection des secrets** : Les mots de passe et données confidentielles sont masqués dans les journaux d'audit et non exportables.

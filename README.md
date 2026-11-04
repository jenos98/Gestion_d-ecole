# Gestion Scolaire — Système Intégré de Gestion & Caisse

Application de bureau moderne pour la gestion administrative, académique et financière scolaire.
Conçue avec **Java 21 LTS**, **JavaFX 21**, **PostgreSQL** et **Maven**.

---

## 1. Prérequis Système

- **Système d'exploitation** : Linux Ubuntu (ou distribution compatible)
- **Java** : OpenJDK 21 LTS (`java -version`)
- **Maven** : Apache Maven 3.9+ (ou via le script `./mvnw`)
- **Base de données** : PostgreSQL 14+ avec port 5432 actif

---

## 2. Configuration de la Base de Données

1. Créer la base de données et le compte applicatif :
   ```sql
   CREATE USER gestion_app WITH PASSWORD 'votre_mot_de_passe_securise';
   CREATE DATABASE gestion_scolaire OWNER gestion_app;
   GRANT ALL PRIVILEGES ON DATABASE gestion_scolaire TO gestion_app;
   ```

2. Exécuter la migration initiale :
   ```bash
   psql -U gestion_app -d gestion_scolaire -f database/migrations/V001__initial_schema.sql
   ```

3. (Optionnel en développement) Insérer les données de démonstration :
   ```bash
   psql -U gestion_app -d gestion_scolaire -f database/seeds/development_data.sql
   ```

4. Définir les variables d'environnement (ou renseigner `src/main/resources/application.properties`) :
   ```bash
   export DB_URL=jdbc:postgresql://localhost:5432/gestion_scolaire
   export DB_USER=gestion_app
   export DB_PASSWORD=votre_mot_de_passe_securise
   ```

---

## 3. Commandes de Compilation et Tests

- **Compiler le projet** :
  ```bash
  ./mvnw clean compile
  ```

- **Lancer les tests automatisés** :
  ```bash
  ./mvnw test
  ```

- **Exécuter l'application en mode développement** :
  ```bash
  ./mvnw javafx:run
  ```

- **Construire le livrable (JAR exécutable + dépendances)** :
  ```bash
  ./mvnw clean package
  ```

---

## 4. Livraison & Lancement en Production

Le packaging Maven produit dans `target/` :
- `target/gestion-scolaire-1.0.0.jar` : Le JAR exécutable référençant ses bibliothèques.
- `target/lib/` : L'ensemble des dépendances nécessaires (JavaFX natif, PostgreSQL, PDFBox, etc.).

Pour lancer l'application sur Ubuntu :
```bash
./scripts/run.sh
```
Ou directement en ligne de commande :
```bash
java -jar target/gestion-scolaire-1.0.0.jar
```

---

## 5. Architecture Logicielle

- `cd.ecole.gestionscolaire.AppLauncher` : Point d'entrée bootstrap sans dépendance directe à `Application`.
- `cd.ecole.gestionscolaire.MainApp` : Initialisation JavaFX et configuration du Stage.
- `cd.ecole.gestionscolaire.config` : Configuration base de données et applicative.
- `cd.ecole.gestionscolaire.service` : Logique métier et validation transactionnelle (Paiement, Caisse, Dépense, Audit).
- `cd.ecole.gestionscolaire.repository` : Accès aux données JDBC paramétrées (sans ORM lourd).

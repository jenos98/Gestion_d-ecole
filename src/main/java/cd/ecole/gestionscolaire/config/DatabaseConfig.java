package cd.ecole.gestionscolaire.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DatabaseConfig — Fournisseur de la configuration de connexion PostgreSQL.
 *
 * RÈGLES DE SÉCURITÉ :
 * - Lecture prioritaire des variables d'environnement DB_URL, DB_USER, DB_PASSWORD.
 * - Repli sur application.properties pour l'environnement local de développement.
 * - Les mots de passe ne sont jamais affichés en clair dans les logs ou les traces.
 */
public final class DatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

    private DatabaseConfig() {
        // Classe utilitaire finale non instanciable
    }

    public static String getJdbcUrl() {
        String envUrl = System.getenv("DB_URL");
        if (envUrl != null && !envUrl.isBlank()) {
            return envUrl.trim();
        }
        return AppConfig.getInstance().getProperty("db.url", "jdbc:postgresql://localhost:5432/gestion_scolaire");
    }

    public static String getUsername() {
        String envUser = System.getenv("DB_USER");
        if (envUser != null && !envUser.isBlank()) {
            return envUser.trim();
        }
        return AppConfig.getInstance().getProperty("db.user", "gestion_app");
    }

    public static String getPassword() {
        String envPass = System.getenv("DB_PASSWORD");
        if (envPass != null) {
            return envPass;
        }
        return AppConfig.getInstance().getProperty("db.password", "");
    }

    /**
     * Valide la présence et la syntaxe des paramètres requis sans exposer le mot de passe.
     * @throws IllegalStateException si les paramètres sont invalides ou manquants
     */
    public static void validateConfiguration() {
        String url = getJdbcUrl();
        String user = getUsername();
        String password = getPassword();

        if (url == null || !url.startsWith("jdbc:postgresql://")) {
            throw new IllegalStateException("L'URL JDBC est invalide ou non configurée (doit débuter par jdbc:postgresql://).");
        }
        if (user == null || user.isBlank()) {
            throw new IllegalStateException("L'utilisateur de la base de données (DB_USER) est obligatoire.");
        }
        if (password == null) {
            throw new IllegalStateException("Le mot de passe de connexion à la base de données (DB_PASSWORD) doit être défini.");
        }

        logger.info("Configuration PostgreSQL validée avec succès pour l'URL : {}", url);
    }

    /**
     * Crée une connexion JDBC vers la base de données.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(getJdbcUrl(), getUsername(), getPassword());
    }
}

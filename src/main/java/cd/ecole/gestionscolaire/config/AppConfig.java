package cd.ecole.gestionscolaire.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * AppConfig — Gestionnaire centralisé de la configuration applicative.
 */
public class AppConfig {

    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);
    private static AppConfig instance;
    private final Properties properties = new Properties();

    private AppConfig() {
        load();
    }

    public static synchronized AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }

    public synchronized void load() {
        try (InputStream in = getClass().getResourceAsStream("/application.properties")) {
            if (in != null) {
                properties.load(in);
                logger.info("Configuration application.properties chargée avec succès.");
            } else {
                logger.warn("application.properties introuvable dans le classpath. Utilisation des valeurs par défaut.");
            }
        } catch (Exception e) {
            logger.error("Erreur lors de la lecture de application.properties : {}", e.getMessage());
        }

        // Chargement du fichier .env local si présent
        File envFile = new File(".env");
        if (envFile.exists() && envFile.isFile()) {
            try (InputStream envIn = new FileInputStream(envFile)) {
                Properties envProps = new Properties();
                envProps.load(envIn);
                for (String name : envProps.stringPropertyNames()) {
                    properties.setProperty(name, envProps.getProperty(name));
                }
                logger.info("Fichier .env local chargé avec succès.");
            } catch (Exception e) {
                logger.warn("Impossible de charger le fichier .env : {}", e.getMessage());
            }
        }
    }

    public String getProperty(String key, String defaultValue) {
        // 1. Priorité aux variables d'environnement système (pour Docker / cloud)
        String envKey = key.toUpperCase().replace('.', '_');
        String envVal = System.getenv(envKey);
        if (envVal != null && !envVal.isBlank()) {
            return envVal;
        }

        String rawEnv = System.getenv(key);
        if (rawEnv != null && !rawEnv.isBlank()) {
            return rawEnv;
        }

        // 2. Recherche dans properties (application.properties ou .env)
        if (properties.containsKey(key)) {
            return properties.getProperty(key);
        }
        if (properties.containsKey(envKey)) {
            return properties.getProperty(envKey);
        }

        return defaultValue;
    }

    public int getIntProperty(String key, int defaultValue) {
        String val = getProperty(key, null);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean getBooleanProperty(String key, boolean defaultValue) {
        String val = getProperty(key, null);
        if (val == null) return defaultValue;
        return Boolean.parseBoolean(val.trim());
    }

    public String getSchoolName() {
        return getProperty("app.school.name", "Complexe Scolaire Jenos");
    }

    public String getPrimaryCurrency() {
        return getProperty("app.school.currency.primary", "USD");
    }

    public String getSecondaryCurrency() {
        return getProperty("app.school.currency.secondary", "CDF");
    }
}

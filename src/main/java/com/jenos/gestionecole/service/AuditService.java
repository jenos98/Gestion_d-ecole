package com.jenos.gestionecole.service;

import com.jenos.gestionecole.model.AuditLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * AuditService — Traçabilité intégrale, immuable et sécurisée des opérations sensibles.
 *
 * RÈGLES DU BLOC 6 :
 * 1. Immutabilité : Les événements sont enregistrés en mode append-only. Aucune suppression n'est permise.
 * 2. Protection des secrets : Les mots de passe et secrets sont automatiquement masqués ([PROTÉGÉ]).
 * 3. Filtrage & Recherche : Recherche multi-critères par utilisateur, mot-clé, module, résultat et date.
 * 4. Audit des échecs : Les échecs critiques (ex: tentatives de connexion suspectes) sont tracés.
 */
public class AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
    private static AuditService instance;

    private final List<AuditLog> auditLogs = new ArrayList<>();

    // Détection des secrets pour sanitization automatique
    private static final Pattern SENSITIVE_PATTERN = Pattern.compile(
            "(?i)(password|motdepasse|pwd|secret|token|hash)[:=]\\s*[^\\s,;]+");

    private AuditService() {}

    public static synchronized AuditService getInstance() {
        if (instance == null) {
            instance = new AuditService();
        }
        return instance;
    }

    public static synchronized void resetForTests() {
        instance = null;
    }

    /**
     * Enregistre un événement d'audit complet.
     */
    public synchronized AuditLog log(String username, String action, AuditLog.AuditModule module,
                                     String entityName, AuditLog.AuditResult result,
                                     String details, String oldValue, String newValue) {
        int nextId = auditLogs.size() + 1;

        String safeUsername = (username != null && !username.isBlank()) ? username.trim() : "Système";
        String safeDetails = sanitize(details);
        String safeOldValue = sanitize(oldValue);
        String safeNewValue = sanitize(newValue);

        AuditLog entry = new AuditLog(
                nextId,
                LocalDateTime.now(),
                safeUsername,
                action != null ? action : "ACTION_NON_SPÉCIFIÉE",
                module != null ? module : AuditLog.AuditModule.SECURITE,
                entityName != null ? entityName : "Système",
                result != null ? result : AuditLog.AuditResult.SUCCES,
                safeDetails,
                safeOldValue,
                safeNewValue
        );

        auditLogs.add(entry);

        if (cd.ecole.gestionscolaire.config.DatabaseConfig.isDatabaseReachable()) {
            saveToDatabase(entry);
        }

        logger.info("[AUDIT] {} - {} par '{}' sur '{}' - Résultat: {}",
                entry.getModule(), entry.getAction(), entry.getUsername(),
                entry.getEntityName(), entry.getResult());

        return entry;
    }

    private void saveToDatabase(AuditLog entry) {
        String sql = "INSERT INTO audit_logs (timestamp, username, action, entity_name, old_value, new_value) VALUES (?, ?, ?, ?, ?, ?)";
        try (java.sql.Connection conn = cd.ecole.gestionscolaire.config.DatabaseConfig.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, java.sql.Timestamp.valueOf(entry.getTimestamp()));
            ps.setString(2, entry.getUsername());
            ps.setString(3, entry.getAction());
            ps.setString(4, entry.getEntityName() != null ? entry.getEntityName() : entry.getModule().name());
            ps.setString(5, entry.getOldValue());
            ps.setString(6, entry.getNewValue());
            ps.executeUpdate();
        } catch (Exception e) {
            logger.debug("Journalisation d'audit PostgreSQL non persistée : {}", e.getMessage());
        }
    }

    /**
     * Enregistre une action avec résultat de succès.
     */
    public AuditLog logSuccess(String username, String action, AuditLog.AuditModule module,
                               String entityName, String details) {
        return log(username, action, module, entityName, AuditLog.AuditResult.SUCCES, details, null, details);
    }

    /**
     * Enregistre une action avec résultat d'échec (ex: échec d'authentification).
     */
    public AuditLog logFailure(String username, String action, AuditLog.AuditModule module,
                               String entityName, String failureReason) {
        return log(username, action, module, entityName, AuditLog.AuditResult.ECHEC, failureReason, null, failureReason);
    }

    /**
     * Méthode de compatibilité historique.
     */
    public void logAction(String username, String action, String entityName, String oldValue, String newValue) {
        int nextId = auditLogs.size() + 1;
        AuditLog log = new AuditLog(nextId, LocalDateTime.now(), username != null ? username : "Système",
                action, entityName, sanitize(oldValue), sanitize(newValue));
        auditLogs.add(log);
    }

    /**
     * Retourne la liste immuable de tous les enregistrements d'audit.
     * Aucune modification externe de la liste n'est possible.
     */
    public synchronized List<AuditLog> getAllLogs() {
        return Collections.unmodifiableList(new ArrayList<>(auditLogs));
    }

    public synchronized int getLogCount() {
        return auditLogs.size();
    }

    /**
     * Moteur de recherche et de filtrage multi-critères.
     */
    public synchronized List<AuditLog> searchLogs(String keyword, String username,
                                                  AuditLog.AuditModule module,
                                                  AuditLog.AuditResult result,
                                                  LocalDate startDate, LocalDate endDate) {
        return auditLogs.stream()
                .filter(log -> {
                    // 1. Filtre par nom d'utilisateur
                    if (username != null && !username.isBlank()) {
                        if (log.getUsername() == null || !log.getUsername().equalsIgnoreCase(username.trim())) {
                            return false;
                        }
                    }

                    // 2. Filtre par module
                    if (module != null) {
                        if (log.getModule() != module) {
                            return false;
                        }
                    }

                    // 3. Filtre par résultat (SUCCES / ECHEC)
                    if (result != null) {
                        if (log.getResult() != result) {
                            return false;
                        }
                    }

                    // 4. Filtre par date
                    if (startDate != null && log.getTimestamp() != null) {
                        if (log.getTimestamp().toLocalDate().isBefore(startDate)) {
                            return false;
                        }
                    }
                    if (endDate != null && log.getTimestamp() != null) {
                        if (log.getTimestamp().toLocalDate().isAfter(endDate)) {
                            return false;
                        }
                    }

                    // 5. Filtre par mot-clé global
                    if (keyword != null && !keyword.isBlank()) {
                        String kw = keyword.toLowerCase().trim();
                        boolean match = (log.getUsername() != null && log.getUsername().toLowerCase().contains(kw))
                                || (log.getAction() != null && log.getAction().toLowerCase().contains(kw))
                                || (log.getEntityName() != null && log.getEntityName().toLowerCase().contains(kw))
                                || (log.getDetails() != null && log.getDetails().toLowerCase().contains(kw))
                                || (log.getNewValue() != null && log.getNewValue().toLowerCase().contains(kw));
                        if (!match) return false;
                    }

                    return true;
                })
                .collect(Collectors.toList());
    }

    /**
     * Masque les secrets et mots de passe pour garantir la confidentialité.
     */
    public static String sanitize(String input) {
        if (input == null) return null;
        String sanitized = SENSITIVE_PATTERN.matcher(input).replaceAll("$1=[PROTÉGÉ]");

        // Masquage spécifique si la chaîne contient directement des identifiants sensibles
        if (input.toLowerCase().contains("password=") || input.toLowerCase().contains("pwd=")) {
            return "[PROTÉGÉ]";
        }
        return sanitized;
    }
}

package com.jenos.gestionecole.model;

import java.time.LocalDateTime;

/**
 * AuditLog — Enregistrement d'audit immuable pour la traçabilité des opérations du système.
 *
 * RÈGLES DU BLOC 6 :
 * 1. Immutabilité : un enregistrement créé ne peut être ni modifié ni supprimé.
 * 2. Précision : horodatage, utilisateur, action, module, objet cible et résultat (Succès/Échec).
 * 3. Confidentialité : aucun mot de passe ou secret n'est conservé en clair.
 */
public class AuditLog {

    public enum AuditModule {
        AUTHENTIFICATION("Authentification"),
        ELEVES("Élèves"),
        PERSONNEL("Personnel"),
        FINANCES("Finances"),
        SALAIRES("Salaires"),
        FRAIS_SCOLAIRES("Frais Scolaires"),
        CONFIGURATION("Configuration"),
        SECURITE("Sécurité");

        private final String label;

        AuditModule(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum AuditResult {
        SUCCES("Succès"),
        ECHEC("Échec");

        private final String label;

        AuditResult(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private int id;
    private LocalDateTime timestamp;
    private String username;
    private String action;
    private AuditModule module;
    private String entityName;
    private AuditResult result;
    private String details;
    private String oldValue;
    private String newValue;

    public AuditLog() {
        this.timestamp = LocalDateTime.now();
        this.module = AuditModule.SECURITE;
        this.result = AuditResult.SUCCES;
    }

    /**
     * Constructeur historique préservé pour compatibilité ascendante.
     */
    public AuditLog(int id, LocalDateTime timestamp, String username, String action,
                    String entityName, String oldValue, String newValue) {
        this.id = id;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        this.username = username != null ? username : "Système";
        this.action = action;
        this.entityName = entityName;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.details = newValue;
        this.module = deduceModule(action, entityName);
        this.result = deduceResult(action, newValue);
    }

    /**
     * Constructeur complet enrichi pour le Bloc 6.
     */
    public AuditLog(int id, LocalDateTime timestamp, String username, String action,
                    AuditModule module, String entityName, AuditResult result,
                    String details, String oldValue, String newValue) {
        this.id = id;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        this.username = username != null ? username : "Système";
        this.action = action;
        this.module = module != null ? module : deduceModule(action, entityName);
        this.entityName = entityName;
        this.result = result != null ? result : AuditResult.SUCCES;
        this.details = details != null ? details : newValue;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    private static AuditModule deduceModule(String action, String entityName) {
        if (action == null) return AuditModule.SECURITE;
        String a = action.toUpperCase();
        String e = entityName != null ? entityName.toUpperCase() : "";

        if (a.contains("CONNEXION") || a.contains("SESSION") || a.contains("LOGOUT") || a.contains("LOGIN")) {
            return AuditModule.AUTHENTIFICATION;
        }
        if (a.contains("ELEVE") || a.contains("INSCRIPTION") || e.contains("ELEVE") || e.contains("STUDENT")) {
            return AuditModule.ELEVES;
        }
        if (a.contains("STAFF") || a.contains("PERSONNEL") || e.contains("STAFF")) {
            return AuditModule.PERSONNEL;
        }
        if (a.contains("PAIE") || a.contains("SALAIRE") || a.contains("AVANCE")) {
            return AuditModule.SALAIRES;
        }
        if (a.contains("FRAIS") || a.contains("REDUCTION") || a.contains("EXEMPTION")) {
            return AuditModule.FRAIS_SCOLAIRES;
        }
        if (a.contains("CAISSE") || a.contains("DEPENSE") || a.contains("PAIEMENT") || a.contains("BUDGET")) {
            return AuditModule.FINANCES;
        }
        if (a.contains("PARAMETRE") || a.contains("SETTINGS") || a.contains("ECOLE")) {
            return AuditModule.CONFIGURATION;
        }
        return AuditModule.SECURITE;
    }

    private static AuditResult deduceResult(String action, String details) {
        if (action != null && (action.toUpperCase().contains("ECHEC") || action.toUpperCase().contains("ÉCHEC")
                || action.toUpperCase().contains("BLOQU") || action.toUpperCase().contains("REFUS"))) {
            return AuditResult.ECHEC;
        }
        if (details != null && (details.toUpperCase().contains("ECHEC") || details.toUpperCase().contains("ÉCHEC")
                || details.toUpperCase().contains("REFUS"))) {
            return AuditResult.ECHEC;
        }
        return AuditResult.SUCCES;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public AuditModule getModule() { return module; }
    public void setModule(AuditModule module) { this.module = module; }

    public String getEntityName() { return entityName; }
    public void setEntityName(String entityName) { this.entityName = entityName; }

    public AuditResult getResult() { return result; }
    public void setResult(AuditResult result) { this.result = result; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getOldValue() { return oldValue; }
    public void setOldValue(String oldValue) { this.oldValue = oldValue; }

    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }
}

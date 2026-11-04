package com.jenos.gestionecole.service;

import com.jenos.gestionecole.model.SchoolSettings;
import com.jenos.gestionecole.util.RoleGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SchoolSettingsService — Gestionnaire sécurisé des paramètres globaux de l'école.
 *
 * RÈGLES DU BLOC 5 :
 * 1. Accès restreint : Modification réservée aux rôles autorisés (PROMOTEUR, ADMIN_TECHNIQUE).
 * 2. Validation rigoureuse avant enregistrement (nom obligatoire, devises USD/CDF valides).
 * 3. Traçabilité des modifications dans AuditService.
 * 4. Immutabilité des opérations historiques : la modification des paramètres généraux
 *    ne modifie jamais les montants ou opérations financières passées.
 */
public class SchoolSettingsService {

    private static final Logger logger = LoggerFactory.getLogger(SchoolSettingsService.class);
    private static SchoolSettingsService instance;
    private SchoolSettings settings;

    private SchoolSettingsService() {
        this.settings = new SchoolSettings();
    }

    public static synchronized SchoolSettingsService getInstance() {
        if (instance == null) {
            instance = new SchoolSettingsService();
        }
        return instance;
    }

    public static synchronized void resetInstance() {
        instance = null;
    }

    public SchoolSettings getSettings() {
        return settings;
    }

    /**
     * Met à jour les paramètres de l'établissement avec contrôle de permissions et validation.
     *
     * @param newSettings les nouveaux paramètres
     * @param username    l'utilisateur effectuant l'action
     * @throws SecurityException        si l'utilisateur n'a pas les droits nécessaires
     * @throws IllegalArgumentException si les paramètres sont invalides
     */
    public void updateSettings(SchoolSettings newSettings, String username) {
        if (newSettings == null) {
            throw new IllegalArgumentException("Les paramètres ne peuvent pas être null.");
        }

        // 1. Contrôle des droits d'accès
        if (!RoleGuard.canManageSettings() && !RoleGuard.can("school.edit")) {
            throw new SecurityException("Accès refusé : Seul le promoteur ou l'administrateur peut modifier les paramètres généraux.");
        }

        // 2. Validation rigoureuse
        if (newSettings.getSchoolName() == null || newSettings.getSchoolName().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom officiel de l'école est obligatoire.");
        }

        String primary = newSettings.getPrimaryCurrency();
        if (primary != null && !"USD".equalsIgnoreCase(primary) && !"CDF".equalsIgnoreCase(primary)) {
            throw new IllegalArgumentException("La devise principale doit être USD ou CDF.");
        }

        String secondary = newSettings.getSecondaryCurrency();
        if (secondary != null && !"USD".equalsIgnoreCase(secondary) && !"CDF".equalsIgnoreCase(secondary)) {
            throw new IllegalArgumentException("La devise secondaire doit être USD ou CDF.");
        }

        if (newSettings.getAutoHideMinutes() < 0) {
            throw new IllegalArgumentException("Le délai de masquage automatique ne peut pas être négatif.");
        }

        // 3. Application et traçabilité
        String oldName = this.settings != null ? this.settings.getSchoolName() : "";
        this.settings = newSettings;

        AuditService.getInstance().logAction(
                username != null ? username : "admin",
                "MODIFICATION_PARAMÈTRES_ÉCOLE",
                "SchoolSettings",
                oldName,
                "Nom: " + newSettings.getSchoolName() + " | Devises: " + primary + "/" + secondary
        );

        logger.info("Paramètres d'établissement mis à jour par '{}' : Nom='{}', Devise='{}'",
                username, newSettings.getSchoolName(), primary);
    }

    /** Méthode de compatibilité. */
    public void updateSettings(SchoolSettings newSettings) {
        updateSettings(newSettings, "admin");
    }
}

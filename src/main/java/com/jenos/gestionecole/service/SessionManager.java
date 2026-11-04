package com.jenos.gestionecole.service;

import com.jenos.gestionecole.model.AcademicYear;
import com.jenos.gestionecole.model.User;
import com.jenos.gestionecole.util.MoneyVisibilityManager;
import com.jenos.gestionecole.util.NavigationManager;
import com.jenos.gestionecole.util.RoleGuard;
import com.jenos.gestionecole.util.ThemeManager;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * SessionManager — Gestion centralisée de la session applicative.
 *
 * RÈGLES DE SÉCURITÉ :
 * - Authentification stricte via UserService.
 * - Compteur d'échecs de connexion (5 échecs consécutifs = verrouillage temporaire).
 * - Traçabilité systématique des succès et échecs dans AuditService.
 * - Initialisation du rôle dans RoleGuard et de l'année scolaire active.
 */
public class SessionManager {

    private static SessionManager instance;

    private User currentUser;
    private AcademicYear activeAcademicYear;
    private LocalDateTime loginTime;

    private final Map<String, Integer> failedAttempts = new HashMap<>();
    private final Map<String, LocalDateTime> lockouts = new HashMap<>();
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 15;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    /**
     * Authentifie un utilisateur avec vérification du statut et verrouillage.
     */
    public User login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Identifiant et mot de passe requis.");
        }

        String key = username.toLowerCase().trim();

        // 1. Vérifier si le compte est temporairement verrouillé par tentatives excessives
        if (isAccountLocked(key)) {
            AuditService.getInstance().logAction(username, "TENTATIVE_CONNEXION_BLOQUÉE", "Session", null, "Tentative sur compte temporairement verrouillé");
            throw new SecurityException("Trop de tentatives infructueuses. Compte verrouillé pour " + LOCKOUT_MINUTES + " minutes.");
        }

        UserService userService = UserService.getInstance();
        User user = userService.findByUsername(key);

        // Utilisateur inexistant
        if (user == null) {
            recordFailedAttempt(key);
            AuditService.getInstance().logAction(username, "ÉCHEC_CONNEXION", "Session", null, "Utilisateur introuvable");
            throw new SecurityException("Identifiant ou mot de passe incorrect.");
        }

        // Vérification du statut administratif
        if (user.getStatus() == User.UserStatus.BLOQUE) {
            AuditService.getInstance().logAction(username, "ÉCHEC_CONNEXION", "Session", null, "Tentative de connexion sur compte bloqué");
            throw new SecurityException("Ce compte est bloqué. Veuillez contacter la direction.");
        }
        if (user.getStatus() == User.UserStatus.DESACTIVE) {
            AuditService.getInstance().logAction(username, "ÉCHEC_CONNEXION", "Session", null, "Tentative de connexion sur compte désactivé");
            throw new SecurityException("Ce compte a été désactivé.");
        }

        // Vérification du mot de passe
        if (!userService.verifyPassword(user, password)) {
            recordFailedAttempt(key);
            AuditService.getInstance().logAction(username, "ÉCHEC_CONNEXION", "Session", null, "Mot de passe incorrect");
            throw new SecurityException("Identifiant ou mot de passe incorrect.");
        }

        // Succès de l'authentification
        resetFailedAttempts(key);
        user.setLastLogin(LocalDateTime.now());
        this.currentUser = user;
        this.loginTime = LocalDateTime.now();

        // Configurer RoleGuard
        RoleGuard.setCurrentRole(user.getRole());

        // Charger l'année académique active
        try {
            this.activeAcademicYear = AcademicYearService.getInstance().getActiveYear();
        } catch (Exception e) {
            this.activeAcademicYear = null;
        }

        // Masquage financier initial systématique
        try {
            MoneyVisibilityManager.getInstance().hideAll();
        } catch (Exception ignored) {}

        // Chargement du thème préféré de l'utilisateur
        try {
            ThemeManager.getInstance().onUserLogin(user.getUsername());
        } catch (Exception ignored) {}

        // Journalisation du succès
        AuditService.getInstance().logAction(user.getUsername(), "CONNEXION", "Session", null, "Connexion réussie (" + user.getRole() + ")");

        return user;
    }

    /**
     * Déconnecte l'utilisateur courant et réinitialise l'environnement de sécurité.
     */
    public void logout() {
        if (currentUser != null) {
            AuditService.getInstance().logAction(currentUser.getUsername(), "DÉCONNEXION", "Session", null, "Déconnexion de session");
        }
        this.currentUser = null;
        this.loginTime = null;
        RoleGuard.clearRole();
        try {
            MoneyVisibilityManager.getInstance().reset();
            NavigationManager.getInstance().clearHistory();
            ThemeManager.getInstance().onUserLogout();
        } catch (Exception ignored) {}
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null) {
            RoleGuard.setCurrentRole(user.getRole());
        }
    }

    public AcademicYear getActiveAcademicYear() {
        return activeAcademicYear;
    }

    public void setActiveAcademicYear(AcademicYear activeAcademicYear) {
        this.activeAcademicYear = activeAcademicYear;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    // ─── Gestion du verrouillage par tentatives répétées ───

    public boolean isAccountLocked(String username) {
        if (username == null) return false;
        String key = username.toLowerCase().trim();
        LocalDateTime lockUntil = lockouts.get(key);
        if (lockUntil == null) return false;

        if (LocalDateTime.now().isAfter(lockUntil)) {
            lockouts.remove(key);
            failedAttempts.remove(key);
            return false;
        }
        return true;
    }

    public void recordFailedAttempt(String username) {
        if (username == null) return;
        String key = username.toLowerCase().trim();
        int attempts = failedAttempts.getOrDefault(key, 0) + 1;
        failedAttempts.put(key, attempts);

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            lockouts.put(key, LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES));
            AuditService.getInstance().logAction(username, "VERROUILLAGE_SÉCURITÉ", "Session", null, "Compte temporairement verrouillé (" + MAX_FAILED_ATTEMPTS + " échecs)");
        }
    }

    public void resetFailedAttempts(String username) {
        if (username == null) return;
        String key = username.toLowerCase().trim();
        failedAttempts.remove(key);
        lockouts.remove(key);
    }

    public int getFailedAttempts(String username) {
        if (username == null) return 0;
        return failedAttempts.getOrDefault(username.toLowerCase().trim(), 0);
    }

    /** Pour les tests : réinitialisation complète de l'état */
    public void resetForTests() {
        this.currentUser = null;
        this.activeAcademicYear = null;
        this.loginTime = null;
        this.failedAttempts.clear();
        this.lockouts.clear();
    }
}

package com.jenos.gestionecole.util;

import java.util.EnumSet;
import java.util.Set;

/**
 * RoleGuard - Couche centrale de contrôle des permissions par rôle.
 *
 * Aucun contrôleur ne doit disperser des conditions comme :
 *   if (user.getRole().equals("CAISSIERE")) { ... }
 *
 * Toutes les vérifications passent par cette classe.
 *
 * Rôles disponibles :
 *   CAISSIERE, PROMOTEUR, DIRECTEUR, SECRETARIAT
 */
public class RoleGuard {

    /** Rôles de l'application. */
    public enum Role {
        CAISSIERE,
        PROMOTEUR,
        DIRECTEUR,
        SECRETARIAT,
        ADMIN_TECHNIQUE
    }

    // Rôle de l'utilisateur connecté — initialisé à la connexion.
    private static Role currentRole;

    private RoleGuard() {}

    /** Initialise le rôle à la connexion. */
    public static void setCurrentRole(Role role) {
        currentRole = role;
    }

    /** Réinitialise le rôle à la déconnexion. */
    public static void clearRole() {
        currentRole = null;
    }

    public static Role getCurrentRole() {
        return currentRole;
    }

    // -------------------------------------------------------------------------
    // DASHBOARD
    // -------------------------------------------------------------------------

    /** Peut accéder au tableau de bord. */
    public static boolean canAccessDashboard() {
        return hasAnyRole(Role.CAISSIERE, Role.PROMOTEUR, Role.DIRECTEUR, Role.SECRETARIAT);
    }

    // -------------------------------------------------------------------------
    // FINANCES
    // -------------------------------------------------------------------------

    /** Peut voir les montants financiers (solde, recettes, dépenses, budget). */
    public static boolean canViewFinancialData() {
        return hasAnyRole(Role.CAISSIERE, Role.PROMOTEUR);
    }

    /** Peut voir les caisses des autres caissières. */
    public static boolean canViewOtherCashRegisters() {
        return hasAnyRole(Role.PROMOTEUR);
    }

    // -------------------------------------------------------------------------
    // PAIEMENTS
    // -------------------------------------------------------------------------

    /** Peut créer un paiement (élève ou personnel). */
    public static boolean canCreatePayment() {
        return hasAnyRole(Role.CAISSIERE, Role.PROMOTEUR);
    }

    // -------------------------------------------------------------------------
    // DÉPENSES
    // -------------------------------------------------------------------------

    /** Peut enregistrer une dépense (droit paramétrable par rôle). */
    public static boolean canCreateExpense() {
        return hasAnyRole(Role.CAISSIERE, Role.PROMOTEUR);
    }

    // -------------------------------------------------------------------------
    // OPÉRATIONS
    // -------------------------------------------------------------------------

    /** Peut annuler une opération. */
    public static boolean canCancelOperation() {
        return hasAnyRole(Role.PROMOTEUR);
    }

    /** Peut corriger une opération. */
    public static boolean canCorrectOperation() {
        return hasAnyRole(Role.PROMOTEUR);
    }

    // -------------------------------------------------------------------------
    // CAISSE & CYCLE DE CLÔTURE
    // -------------------------------------------------------------------------

    /** Peut ouvrir une caisse enregistreuse. */
    public static boolean canOpenCashRegister() {
        return hasAnyRole(Role.CAISSIERE, Role.PROMOTEUR);
    }

    /** Peut voir sa propre caisse active. */
    public static boolean canViewOwnCashRegister() {
        return hasAnyRole(Role.CAISSIERE, Role.PROMOTEUR);
    }

    /** Peut voir toutes les caisses enregistreuses. */
    public static boolean canViewAllCashRegisters() {
        return hasAnyRole(Role.PROMOTEUR);
    }

    /** Peut générer le rapport journalier de caisse. */
    public static boolean canGenerateDailyReport() {
        return hasAnyRole(Role.CAISSIERE, Role.PROMOTEUR);
    }

    /** Peut imprimer le rapport journalier de caisse. */
    public static boolean canPrintDailyReport() {
        return hasAnyRole(Role.CAISSIERE, Role.PROMOTEUR);
    }

    /** Peut procéder à la clôture normale de la caisse. */
    public static boolean canCloseCashRegister() {
        return hasAnyRole(Role.CAISSIERE, Role.PROMOTEUR);
    }

    /** Peut forcer une clôture exceptionnelle sans rapport (réservé au superviseur/promoteur). */
    public static boolean canForceCloseCashRegister() {
        return hasAnyRole(Role.PROMOTEUR);
    }

    /** Peut autoriser un écart de caisse supérieur au seuil. */
    public static boolean canAuthorizeVariance() {
        return hasAnyRole(Role.PROMOTEUR);
    }

    /** Peut clôturer la caisse (méthode de compatibilité). */
    public static boolean canCloseCash() {
        return canCloseCashRegister();
    }

    /** Peut forcer une clôture exceptionnelle (méthode de compatibilité). */
    public static boolean canForceCloseCash() {
        return canForceCloseCashRegister();
    }

    // -------------------------------------------------------------------------
    // RAPPORTS
    // -------------------------------------------------------------------------

    /** Peut générer des rapports de caisse. */
    public static boolean canGenerateCashReports() {
        return hasAnyRole(Role.CAISSIERE, Role.PROMOTEUR);
    }

    /** Peut voir les rapports de toutes les caisses. */
    public static boolean canViewAllCashReports() {
        return hasAnyRole(Role.PROMOTEUR);
    }

    /** Peut générer des rapports élèves. */
    public static boolean canGenerateStudentReports() {
        return hasAnyRole(Role.PROMOTEUR, Role.DIRECTEUR, Role.SECRETARIAT);
    }

    // -------------------------------------------------------------------------
    // ÉLÈVES
    // -------------------------------------------------------------------------

    /** Peut accéder à la gestion des élèves. */
    public static boolean canAccessStudents() {
        return hasAnyRole(Role.CAISSIERE, Role.PROMOTEUR, Role.DIRECTEUR, Role.SECRETARIAT, Role.ADMIN_TECHNIQUE);
    }

    /** Peut visualiser la liste des élèves. */
    public static boolean canViewStudents() {
        return canAccessStudents();
    }

    /** Peut visualiser l'historique général des opérations. */
    public static boolean canViewHistory() {
        return hasAnyRole(Role.CAISSIERE, Role.PROMOTEUR, Role.DIRECTEUR, Role.SECRETARIAT, Role.ADMIN_TECHNIQUE);
    }

    /** Peut accéder au module des rapports. */
    public static boolean canViewReports() {
        return hasAnyRole(Role.CAISSIERE, Role.PROMOTEUR, Role.DIRECTEUR, Role.SECRETARIAT, Role.ADMIN_TECHNIQUE);
    }

    /** Peut inscrire / créer un nouvel élève. */
    public static boolean canCreateStudent() {
        return hasAnyRole(Role.CAISSIERE, Role.PROMOTEUR, Role.DIRECTEUR, Role.SECRETARIAT);
    }

    /** Peut désactiver un élève (statut inactif non destructif). */
    public static boolean canDeactivateStudent() {
        return hasAnyRole(Role.PROMOTEUR, Role.DIRECTEUR, Role.SECRETARIAT);
    }

    // -------------------------------------------------------------------------
    // PERSONNEL
    // -------------------------------------------------------------------------

    /** Peut accéder à la gestion du personnel (vue administrative). */
    public static boolean canAccessStaff() {
        return hasAnyRole(Role.PROMOTEUR, Role.DIRECTEUR, Role.SECRETARIAT);
    }

    /** Peut créer un nouveau membre du personnel. */
    public static boolean canCreateStaff() {
        return hasAnyRole(Role.PROMOTEUR, Role.DIRECTEUR, Role.SECRETARIAT);
    }

    /** Peut archiver un membre du personnel (non destructif). */
    public static boolean canArchiveStaff() {
        return hasAnyRole(Role.PROMOTEUR, Role.DIRECTEUR);
    }

    /** Peut voir les montants salariaux du personnel. */
    public static boolean canViewSalaries() {
        return hasAnyRole(Role.PROMOTEUR, Role.DIRECTEUR);
    }

    /** Peut valider les rémunérations et gérer les paiements de paie. */
    public static boolean canManagePayroll() {
        return hasAnyRole(Role.PROMOTEUR, Role.DIRECTEUR);
    }

    /** Peut accorder une réduction ou exemption de frais scolaires. */
    public static boolean canGrantFeeReduction() {
        return hasAnyRole(Role.PROMOTEUR, Role.DIRECTEUR);
    }

    /** Peut annuler un paiement validé. */
    public static boolean canCancelPayment() {
        return hasAnyRole(Role.PROMOTEUR);
    }

    // -------------------------------------------------------------------------
    // EXPORTS DE DONNÉES & RAPPORTS
    // -------------------------------------------------------------------------

    /** Peut exporter les données des élèves (listes, effectifs). */
    public static boolean canExportStudentData() {
        return hasAnyRole(Role.PROMOTEUR, Role.DIRECTEUR, Role.SECRETARIAT);
    }

    /** Peut exporter les données financières globales (paiements, caisse, impayés). */
    public static boolean canExportFinancialData() {
        return hasAnyRole(Role.PROMOTEUR);
    }

    /** Peut exporter les données administratives du personnel. */
    public static boolean canExportStaffData() {
        return hasAnyRole(Role.PROMOTEUR, Role.DIRECTEUR);
    }

    /** Peut exporter les données salariales et fiches de paie. */
    public static boolean canExportSalaryData() {
        return hasAnyRole(Role.PROMOTEUR);
    }

    // -------------------------------------------------------------------------
    // OPTIONS ET PARAMÈTRES
    // -------------------------------------------------------------------------

    /** Peut gérer les options d'étude (Section/Promotion/Option/Classe). */
    public static boolean canManageOptions() {
        return hasAnyRole(Role.PROMOTEUR, Role.DIRECTEUR, Role.SECRETARIAT, Role.ADMIN_TECHNIQUE);
    }

    /** Peut consulter le journal d'audit et de traçabilité système. */
    public static boolean canViewAuditLog() {
        return hasAnyRole(Role.PROMOTEUR, Role.ADMIN_TECHNIQUE);
    }

    /** Peut gérer la grille tarifaire et les frais scolaires. */
    public static boolean canManageFees() {
        return hasAnyRole(Role.PROMOTEUR, Role.ADMIN_TECHNIQUE);
    }

    /** Peut accéder aux paramètres globaux de l'application. */
    public static boolean canManageSettings() {
        return hasAnyRole(Role.PROMOTEUR, Role.ADMIN_TECHNIQUE);
    }

    /** Peut administrer les utilisateurs du système. */
    public static boolean canManageUsers() {
        return hasAnyRole(Role.PROMOTEUR, Role.ADMIN_TECHNIQUE);
    }

    // -------------------------------------------------------------------------
    // CONTRÔLE CENTRALISÉ DES PERMISSIONS GRANULAIRES
    // -------------------------------------------------------------------------

    /**
     * Vérifie si le rôle courant dispose d'une permission granulaire.
     * Exemple : RoleGuard.can("cash.close"), RoleGuard.can("report.financial"), RoleGuard.can("payment.cancel")
     */
    public static boolean can(String permission) {
        if (currentRole == null || permission == null) return false;
        return switch (permission.toLowerCase().trim()) {
            // Élèves
            case "student.view" -> hasAnyRole(Role.CAISSIERE, Role.PROMOTEUR, Role.DIRECTEUR, Role.SECRETARIAT, Role.ADMIN_TECHNIQUE);
            case "student.create", "student.edit" -> hasAnyRole(Role.CAISSIERE, Role.PROMOTEUR, Role.DIRECTEUR, Role.SECRETARIAT, Role.ADMIN_TECHNIQUE);
            case "student.archive" -> hasAnyRole(Role.PROMOTEUR, Role.DIRECTEUR, Role.SECRETARIAT, Role.ADMIN_TECHNIQUE);

            // Personnel
            case "staff.view" -> hasAnyRole(Role.PROMOTEUR, Role.DIRECTEUR, Role.SECRETARIAT, Role.ADMIN_TECHNIQUE);
            case "staff.create", "staff.edit" -> hasAnyRole(Role.PROMOTEUR, Role.DIRECTEUR, Role.SECRETARIAT, Role.ADMIN_TECHNIQUE);
            case "staff.archive" -> hasAnyRole(Role.PROMOTEUR, Role.DIRECTEUR, Role.ADMIN_TECHNIQUE);

            // Paiements (Admin technique strictement exclu des opérations financières)
            case "payment.create", "payment.view.own" -> hasAnyRole(Role.CAISSIERE, Role.PROMOTEUR);
            case "payment.view.all", "payment.correct", "payment.cancel" -> hasAnyRole(Role.PROMOTEUR);

            // Dépenses (Admin technique strictement exclu des flux de fonds)
            case "expense.create", "expense.view.own" -> hasAnyRole(Role.CAISSIERE, Role.PROMOTEUR);
            case "expense.view.all", "expense.approve", "expense.cancel" -> hasAnyRole(Role.PROMOTEUR);

            // Caisse
            case "cash.open", "cash.view.own", "cash.report.generate", "cash.report.print", "cash.close" -> hasAnyRole(Role.CAISSIERE, Role.PROMOTEUR);
            case "cash.view.all", "cash.forceclose" -> hasAnyRole(Role.PROMOTEUR);

            // Rapports
            case "report.view" -> hasAnyRole(Role.CAISSIERE, Role.PROMOTEUR, Role.DIRECTEUR, Role.SECRETARIAT, Role.ADMIN_TECHNIQUE);
            case "report.financial" -> hasAnyRole(Role.PROMOTEUR); // Admin technique n'a JAMAIS accès aux montants financiers
            case "report.student" -> hasAnyRole(Role.PROMOTEUR, Role.DIRECTEUR, Role.SECRETARIAT, Role.ADMIN_TECHNIQUE);
            case "report.staff" -> hasAnyRole(Role.PROMOTEUR, Role.DIRECTEUR, Role.ADMIN_TECHNIQUE);
            case "report.export" -> hasAnyRole(Role.PROMOTEUR, Role.DIRECTEUR, Role.SECRETARIAT);

            // Administration, Audit & Configuration
            case "audit.view", "audit.export" -> hasAnyRole(Role.PROMOTEUR, Role.ADMIN_TECHNIQUE);
            case "user.manage", "role.view", "role.edit", "school.edit" -> hasAnyRole(Role.PROMOTEUR, Role.ADMIN_TECHNIQUE);
            case "academic.manage" -> hasAnyRole(Role.PROMOTEUR, Role.DIRECTEUR, Role.SECRETARIAT, Role.ADMIN_TECHNIQUE);
            case "fee.manage", "payment_method.manage" -> hasAnyRole(Role.PROMOTEUR, Role.ADMIN_TECHNIQUE);
            case "fee.reduction", "fee.exemption" -> hasAnyRole(Role.PROMOTEUR, Role.DIRECTEUR);
            case "budget.manage" -> hasAnyRole(Role.PROMOTEUR); // Admin technique exclu de la gestion budgétaire financière

            default -> false;
        };
    }

    // -------------------------------------------------------------------------
    // UTILITAIRE INTERNE
    // -------------------------------------------------------------------------

    private static boolean hasAnyRole(Role... roles) {
        if (currentRole == null) return false;
        return EnumSet.copyOf(Set.of(roles)).contains(currentRole);
    }
}

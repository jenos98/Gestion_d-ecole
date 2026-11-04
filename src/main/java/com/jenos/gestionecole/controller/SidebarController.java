package com.jenos.gestionecole.controller;

import com.jenos.gestionecole.util.NavigationManager;
import com.jenos.gestionecole.util.RoleGuard;
import com.jenos.gestionecole.util.Route;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;

/**
 * SidebarController — Contrôleur du menu de navigation latéral.
 *
 * Responsabilités :
 * - Navigation vers les vues via NavigationManager + Route enum
 * - Construction dynamique du menu selon le rôle (RoleGuard)
 * - Isolation absolue de l'Administrateur Technique et du Directeur des opérations financières
 * - Highlight du bouton de la vue active
 */
public class SidebarController {

    @FXML private Button btnDashboard;
    @FXML private Button btnStudents;
    @FXML private Button btnEnrollment;
    @FXML private Button btnStaff;
    @FXML private Button btnFees;
    @FXML private Button btnPayments;
    @FXML private Button btnExpenses;
    @FXML private Button btnCash;
    @FXML private Button btnHistory;
    @FXML private Button btnReports;
    @FXML private Button btnSettings;
    @FXML private Button btnAuditLog;
    @FXML private Button btnGuide;

    @FXML
    public void initialize() {
        applyPermissions();
    }

    // -------------------------------------------------------------------------
    // PERMISSIONS DYNAMIQUES PAR RÔLE
    // -------------------------------------------------------------------------

    public void applyPermissions() {
        // Scolarité / Élèves / Inscription
        guard(btnEnrollment, RoleGuard.canCreateStudent(), "Accès non autorisé");
        guard(btnStudents, RoleGuard.canViewStudents(), "Accès non autorisé");

        // Personnel
        guard(btnStaff, RoleGuard.canAccessStaff(), "Accès réservé");

        // Caisse & Finances (Strictement interdit à l'ADMIN_TECHNIQUE et au DIRECTEUR)
        boolean canFinancial = RoleGuard.canViewFinancialData();
        guard(btnCash, RoleGuard.canCloseCash() && canFinancial, "Réservé aux caissières et responsables financiers");
        guard(btnPayments, RoleGuard.canCreatePayment() && canFinancial, "Accès financier non autorisé");
        guard(btnExpenses, RoleGuard.canCreateExpense() && canFinancial, "Accès financier non autorisé");
        guard(btnFees, RoleGuard.canManageFees(), "Réservé au promoteur et administrateur");

        // Suivi & Rapports
        guard(btnHistory, RoleGuard.canViewHistory(), "Accès non autorisé");
        guard(btnReports, RoleGuard.canViewReports(), "Accès non autorisé");

        // Administration & Audit
        guard(btnSettings, RoleGuard.canManageSettings(), "Réservé aux administrateurs");
        guard(btnAuditLog, RoleGuard.can("user.manage"), "Réservé au promoteur et administrateur technique");
    }

    /**
     * Désactive le bouton et ajoute un tooltip si non autorisé.
     */
    private void guard(Button btn, boolean allowed, String reason) {
        if (btn == null) return;
        btn.setDisable(!allowed);
        if (!allowed && btn.getTooltip() == null) {
            btn.setTooltip(new Tooltip(reason));
        }
    }

    // -------------------------------------------------------------------------
    // NAVIGATION
    // -------------------------------------------------------------------------

    @FXML private void goToDashboard()  { navigate(Route.DASHBOARD); }
    @FXML private void goToStudents()   { navigate(Route.STUDENTS); }
    @FXML private void goToEnrollment() { navigate(Route.ENROLLMENT); }
    @FXML private void goToStaff()      { navigate(Route.STAFF); }
    @FXML private void goToFees()       { navigate(Route.FEES); }
    @FXML private void goToPayments()  { navigate(Route.PAYMENTS); }
    @FXML private void goToExpenses()  { navigate(Route.EXPENSES); }
    @FXML private void goToCash()      { navigate(Route.CASH); }
    @FXML private void goToHistory()   { navigate(Route.HISTORY); }
    @FXML private void goToReports()   { navigate(Route.REPORTS); }
    @FXML private void goToGuide()     { navigate(Route.GUIDE); }

    @FXML
    private void goToSettings() {
        if (RoleGuard.canManageSettings()) {
            navigate(Route.SETTINGS);
        }
    }

    @FXML
    private void goToAuditLog() {
        if (RoleGuard.can("user.manage")) {
            navigate(Route.AUDIT_LOG);
        }
    }

    private void navigate(Route route) {
        NavigationManager.getInstance().navigate(route);
        setActiveButton(route);
    }

    // -------------------------------------------------------------------------
    // HIGHLIGHT DU BOUTON ACTIF
    // -------------------------------------------------------------------------

    private void setActiveButton(Route route) {
        clearActive();

        Button active = switch (route) {
            case DASHBOARD  -> btnDashboard;
            case STUDENTS   -> btnStudents;
            case ENROLLMENT -> btnEnrollment;
            case STAFF      -> btnStaff;
            case FEES       -> btnFees;
            case PAYMENTS   -> btnPayments;
            case EXPENSES   -> btnExpenses;
            case CASH, CASH_REGISTER -> btnCash;
            case HISTORY    -> btnHistory;
            case REPORTS    -> btnReports;
            case SETTINGS   -> btnSettings;
            case AUDIT_LOG  -> btnAuditLog;
            case GUIDE      -> btnGuide;
            default         -> null;
        };

        if (active != null) {
            active.getStyleClass().add("active");
        }
    }

    private void clearActive() {
        Button[] all = {btnDashboard, btnStudents, btnEnrollment, btnStaff, btnFees, btnPayments,
                        btnExpenses, btnCash, btnHistory, btnReports,
                        btnSettings, btnAuditLog, btnGuide};
        for (Button b : all) {
            if (b != null) {
                b.getStyleClass().remove("active");
            }
        }
    }
}

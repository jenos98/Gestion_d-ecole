package com.jenos.gestionecole.controller;

import com.jenos.gestionecole.model.AcademicYear;
import com.jenos.gestionecole.service.NotificationService;
import com.jenos.gestionecole.service.SessionManager;
import com.jenos.gestionecole.util.DialogManager;
import com.jenos.gestionecole.util.MoneyVisibilityManager;
import com.jenos.gestionecole.util.NavigationManager;
import com.jenos.gestionecole.util.RoleGuard;
import com.jenos.gestionecole.util.Route;
import com.jenos.gestionecole.util.ThemeManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;

/**
 * HeaderController — Contrôleur de la barre supérieure.
 *
 * Responsabilités :
 * - Afficher l'identité de l'établissement et l'année scolaire active
 * - Afficher les informations de session (user, rôle, caisse)
 * - Badge de notifications temps-réel via NotificationService
 * - Accès rapide au profil utilisateur et aux paramètres
 * - Gérer la visibilité des montants (MoneyVisibilityManager)
 * - Confirmer avant déconnexion avec DialogManager
 */
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HeaderController implements NotificationService.NotificationObserver {

    private static final Logger logger = LoggerFactory.getLogger(HeaderController.class);

    // Identité
    @FXML private ImageView imgLogo;
    @FXML private Label lblSchoolName;
    @FXML private Label lblSystemName;
    @FXML private Label lblAcademicYear;

    // Session
    @FXML private Label lblUsername;
    @FXML private Label lblRole;
    @FXML private Label lblCashRegister;

    // Actions & Alertes
    @FXML private Button btnMoneyVisibility;
    @FXML private Button btnNotifications;
    @FXML private Label lblNotificationBadge;
    @FXML private Button btnProfile;
    @FXML private Button btnTheme;
    @FXML private Button btnSettings;
    @FXML private Button btnLogout;

    @FXML private Tooltip tooltipVisibility;
    @FXML private Tooltip tooltipTheme;

    private final ThemeManager.ThemeChangeListener themeListener = newTheme -> Platform.runLater(this::syncThemeButton);

    @FXML
    public void initialize() {
        applyPermissions();
        syncVisibilityButton();
        syncAcademicYear();
        syncThemeButton();
        ThemeManager.getInstance().addListener(themeListener);
        NotificationService.getInstance().addObserver(this);
        onNotificationsChanged();
    }

    public void setSchoolInfo(String schoolName) {
        if (lblSchoolName != null && schoolName != null) {
            lblSchoolName.setText(schoolName);
        }
    }

    public void setUserInfo(String username, String role, String cashRegister) {
        if (lblUsername != null) lblUsername.setText(username);
        if (lblRole != null) lblRole.setText(role);

        if (lblCashRegister != null) {
            if (cashRegister != null && !cashRegister.isBlank()) {
                lblCashRegister.setText("Caisse : " + cashRegister);
                lblCashRegister.setVisible(true);
                lblCashRegister.setManaged(true);
            } else {
                lblCashRegister.setVisible(false);
                lblCashRegister.setManaged(false);
            }
        }
        syncAcademicYear();
    }

    private void syncAcademicYear() {
        if (lblAcademicYear != null) {
            AcademicYear year = SessionManager.getInstance().getActiveAcademicYear();
            if (year != null) {
                lblAcademicYear.setText("Année : " + year.getCode());
                lblAcademicYear.setVisible(true);
                lblAcademicYear.setManaged(true);
            } else {
                lblAcademicYear.setText("Année : Non définie");
            }
        }
    }

    private void applyPermissions() {
        if (btnSettings != null) {
            boolean canManage = RoleGuard.canManageSettings();
            btnSettings.setVisible(canManage);
            btnSettings.setManaged(canManage);
        }
    }

    // ─── VISIBILITÉ DES MONTANTS ───────────────────────────────────────────────

    @FXML
    private void toggleMoneyVisibility() {
        MoneyVisibilityManager mgr = MoneyVisibilityManager.getInstance();
        if (mgr.isVisible()) {
            mgr.hideAll();
        } else {
            mgr.showAll();
        }
        syncVisibilityButton();
    }

    private void syncVisibilityButton() {
        if (btnMoneyVisibility != null) {
            boolean visible = MoneyVisibilityManager.getInstance().isVisible();
            btnMoneyVisibility.setText(visible ? "🙈" : "👁");
            if (tooltipVisibility != null) {
                tooltipVisibility.setText(visible ? "Masquer les montants" : "Afficher les montants");
            }
        }
    }

    // ─── NOTIFICATIONS & PROFIL ───────────────────────────────────────────────

    @Override
    public void onNotificationsChanged() {
        Platform.runLater(() -> {
            if (lblNotificationBadge != null) {
                int unread = NotificationService.getInstance().getUnreadCount();
                if (unread > 0) {
                    lblNotificationBadge.setText(String.valueOf(unread));
                    lblNotificationBadge.setVisible(true);
                    lblNotificationBadge.setManaged(true);
                } else {
                    lblNotificationBadge.setVisible(false);
                    lblNotificationBadge.setManaged(false);
                }
            }
        });
    }

    @FXML
    private void openNotifications() {
        NavigationManager.getInstance().navigate(Route.NOTIFICATIONS);
    }

    @FXML
    private void openProfile() {
        NavigationManager.getInstance().navigate(Route.PROFILE);
    }

    // ─── THÈME & PARAMÈTRES ───────────────────────────────────────────────────

    @FXML
    private void toggleTheme() {
        ThemeManager.getInstance().toggleTheme();
    }

    private void syncThemeButton() {
        boolean isDark = ThemeManager.getInstance().isDarkMode();
        if (btnTheme != null) {
            btnTheme.setText(isDark ? "☾" : "☀");
            if (tooltipTheme != null) {
                tooltipTheme.setText(isDark ? "Désactiver le mode sombre (Mode clair)" : "Activer le mode sombre");
            }
        }
    }

    @FXML
    private void openSettings() {
        if (RoleGuard.canManageSettings()) {
            NavigationManager.getInstance().navigate(Route.SETTINGS);
        }
    }

    // ─── DÉCONNEXION SÉCURISÉE ────────────────────────────────────────────────

    @FXML
    private void confirmLogout() {
        DialogManager.getInstance().showConfirm(
                "Déconnexion du Système",
                "Êtes-vous sûr de vouloir fermer votre session de travail ?",
                confirmed -> {
                    if (confirmed) {
                        performLogout();
                    }
                }
        );
    }

    private void performLogout() {
        ThemeManager.getInstance().removeListener(themeListener);
        NotificationService.getInstance().removeObserver(this);
        SessionManager.getInstance().logout();

        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/fxml/login.fxml")
            );
            javafx.scene.Parent root = loader.load();
            if (NavigationManager.getInstance().getPrimaryStage() != null &&
                NavigationManager.getInstance().getPrimaryStage().getScene() != null) {
                NavigationManager.getInstance().getPrimaryStage().getScene().setRoot(root);
            }
        } catch (Exception e) {
            logger.error("Erreur lors de la redirection vers l'écran de connexion : {}", e.getMessage(), e);
        }
    }
}

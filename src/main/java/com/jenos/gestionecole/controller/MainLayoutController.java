package com.jenos.gestionecole.controller;

import com.jenos.gestionecole.util.NavigationManager;
import com.jenos.gestionecole.util.Route;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

/**
 * MainLayoutController — Contrôleur du layout principal.
 *
 * Responsabilités :
 * - Initialiser NavigationManager avec le contentContainer
 * - Initialiser Header avec les infos utilisateur
 * - Appliquer les permissions sur le menu Sidebar
 * - Charger le Dashboard au démarrage
 *
 * Ce contrôleur est le point de départ de toute la navigation.
 */
public class MainLayoutController {

    @FXML private StackPane contentContainer;

    // Contrôleurs inclus via fx:include
    @FXML private HeaderController headerController;
    @FXML private SidebarController sidebarController;

    @FXML
    public void initialize() {
        // 1. Donner le conteneur au gestionnaire de navigation
        NavigationManager.getInstance().setContentContainer(contentContainer);

        // 2. Appliquer les permissions de menu selon le rôle connecté
        if (sidebarController != null) {
            sidebarController.applyPermissions();
        }

        // 3. Charger la vue initiale : Dashboard
        NavigationManager.getInstance().navigate(Route.DASHBOARD);
    }

    /**
     * Initialise les informations de l'utilisateur dans le Header.
     * Appelé par LoginController après authentification réussie.
     *
     * @param username     Nom d'utilisateur
     * @param role         Libellé du rôle
     * @param cashRegister Nom de la caisse (null si aucune caisse associée)
     */
    public void initUserInfo(String username, String role, String cashRegister) {
        if (headerController != null) {
            headerController.setUserInfo(username, role, cashRegister);
        }
    }

    /**
     * Définit le nom de l'établissement dans le Header.
     */
    public void initSchoolInfo(String schoolName) {
        if (headerController != null) {
            headerController.setSchoolInfo(schoolName);
        }
    }
}

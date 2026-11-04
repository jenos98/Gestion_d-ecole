package com.jenos.gestionecole.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * NavigationManager — Gestionnaire central de navigation (Singleton).
 *
 * RÈGLE : Toutes les transitions de vues passent par cette classe.
 * Aucun contrôleur ne doit instancier FXMLLoader + Scene directement.
 *
 * Usage typique :
 *   NavigationManager.getInstance().navigate(Route.STUDENTS);
 *   NavigationManager.getInstance().goBack();
 */
public class NavigationManager {

    private static NavigationManager instance;

    private Stage primaryStage;
    private StackPane contentContainer;

    private final Deque<Route> history = new ArrayDeque<>();
    private Route currentRoute;

    private NavigationManager() {}

    public static NavigationManager getInstance() {
        if (instance == null) {
            instance = new NavigationManager();
        }
        return instance;
    }

    // -------------------------------------------------------------------------
    // INITIALISATION
    // -------------------------------------------------------------------------

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    public void setContentContainer(StackPane container) {
        this.contentContainer = container;
    }

    // -------------------------------------------------------------------------
    // NAVIGATION PAR ROUTE (méthode principale)
    // -------------------------------------------------------------------------

    /**
     * Navigue vers une route typée.
     * Enregistre la route précédente dans l'historique.
     */
    public void navigate(Route route) {
        if (currentRoute != null && currentRoute != route) {
            history.push(currentRoute);
        }
        loadIntoCenter(route.getFxmlPath());
        currentRoute = route;
    }

    /**
     * Retourne à la vue précédente.
     */
    public void goBack() {
        if (!history.isEmpty()) {
            Route previous = history.pop();
            loadIntoCenter(previous.getFxmlPath());
            currentRoute = previous;
        }
    }

    // -------------------------------------------------------------------------
    // NAVIGATION DIRECTE (compatibilité interne)
    // -------------------------------------------------------------------------

    /**
     * Navigue via chemin direct (pour compatibilité et cas spéciaux).
     * Préférer navigate(Route) pour les navigations normales.
     */
    public void goTo(String fxmlPath) {
        loadIntoCenter(fxmlPath);
    }

    /**
     * Remplace le contenu du centre sans enregistrer dans l'historique.
     * Utilisé pour les redirections (ex: logout → login).
     */
    public void replaceCenter(String fxmlPath) {
        loadIntoCenter(fxmlPath);
        history.clear();
        currentRoute = null;
    }

    // -------------------------------------------------------------------------
    // CHARGEMENT FXML
    // -------------------------------------------------------------------------

    private void loadIntoCenter(String fxmlPath) {
        if (contentContainer == null) {
            throw new IllegalStateException(
                "ContentContainer non initialisé. Appelez setContentContainer() depuis MainLayoutController."
            );
        }
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/" + fxmlPath)
            );
            Node node = loader.load();
            contentContainer.getChildren().setAll(node);
        } catch (IOException e) {
            throw new RuntimeException("Impossible de charger la vue : /fxml/" + fxmlPath, e);
        }
    }

    /**
     * Charge un FXML et retourne son contrôleur.
     * Utile pour injecter des données dans un dialogue ou un panneau.
     */
    public <T> T loadForController(String fxmlPath) throws IOException {
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/fxml/" + fxmlPath)
        );
        loader.load();
        return loader.getController();
    }

    // -------------------------------------------------------------------------
    // UTILITAIRES
    // -------------------------------------------------------------------------

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public StackPane getContentContainer() {
        return contentContainer;
    }

    public Route getCurrentRoute() {
        return currentRoute;
    }

    /** Réinitialise l'historique (à appeler à la déconnexion). */
    public void clearHistory() {
        history.clear();
        currentRoute = null;
    }
}

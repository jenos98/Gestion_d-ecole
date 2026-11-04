package cd.ecole.gestionscolaire;

import cd.ecole.gestionscolaire.config.AppConfig;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URL;

/**
 * MainApp — Initialisation de l'application JavaFX.
 *
 * Responsabilités :
 * - Hériter de javafx.application.Application
 * - Initialiser la configuration globale (AppConfig)
 * - Charger l'interface de connexion (fxml/login.fxml)
 * - Configurer la fenêtre principale
 * - Gérer l'arrêt propre des ressources
 * - Ne contient aucune logique métier ni requête SQL
 */
public class MainApp extends Application {

    private static final Logger logger = LoggerFactory.getLogger(MainApp.class);

    @Override
    public void init() throws Exception {
        logger.info("Initialisation de la configuration applicative...");
        AppConfig.getInstance().load();
    }

    @Override
    public void start(Stage primaryStage) {
        logger.info("Démarrage de l'interface utilisateur JavaFX...");

        Parent root = null;
        try {
            URL fxmlUrl = getClass().getResource("/fxml/login.fxml");
            if (fxmlUrl != null) {
                FXMLLoader loader = new FXMLLoader(fxmlUrl);
                root = loader.load();
            } else {
                root = createFallbackView();
            }
        } catch (Exception e) {
            logger.warn("Impossible de charger fxml/login.fxml directement : {}", e.getMessage());
            root = createFallbackView();
        }

        Scene scene = new Scene(root, 1366, 768);

        // Chargement du thème CSS principal
        try {
            URL cssUrl = getClass().getResource("/css/app.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }
        } catch (Exception e) {
            logger.warn("Erreur chargement app.css : {}", e.getMessage());
        }

        String title = AppConfig.getInstance().getProperty("app.name", "Gestion Scolaire")
                + " — " + AppConfig.getInstance().getProperty("app.school.name", "Complexe Scolaire Jenos");

        primaryStage.setTitle(title);
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(640);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Initialisation du gestionnaire central de navigation et du gestionnaire de thème
        com.jenos.gestionecole.util.NavigationManager.getInstance().setPrimaryStage(primaryStage);
        com.jenos.gestionecole.util.ThemeManager.getInstance().applyTheme(scene);

        logger.info("Interface principale affichée avec succès.");
    }

    private Parent createFallbackView() {
        StackPane pane = new StackPane();
        Label lbl = new Label("Gestion Scolaire — Prêt pour authentification");
        lbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        pane.getChildren().add(lbl);
        return pane;
    }

    @Override
    public void stop() throws Exception {
        logger.info("Fermeture propre de l'application et libération des ressources...");
        super.stop();
    }
}

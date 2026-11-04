package com.jenos.gestionecole;

import com.jenos.gestionecole.util.NavigationManager;
import com.jenos.gestionecole.util.ThemeManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Charger l'écran de connexion
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/fxml/login.fxml")
        );
        Parent root = loader.load();

        Scene scene = new Scene(root, 1366, 768);

        // Charger le thème via le fichier d'entrée unique
        scene.getStylesheets().add(
            getClass().getResource("/css/app.css").toExternalForm()
        );

        primaryStage.setTitle("Gestion Scolaire");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(600);
        primaryStage.show();

        // Initialiser le gestionnaire de navigation et le thème
        NavigationManager.getInstance().setPrimaryStage(primaryStage);
        ThemeManager.getInstance().applyTheme(scene);
    }

    public static void main(String[] args) {
        launch(args);
    }
}

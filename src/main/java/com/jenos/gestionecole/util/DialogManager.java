package com.jenos.gestionecole.util;

import com.jenos.gestionecole.controller.dialog.ConfirmDialogController;
import com.jenos.gestionecole.controller.dialog.ErrorDialogController;
import com.jenos.gestionecole.model.AppErrorDTO;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.function.Consumer;

/**
 * DialogManager — Gestionnaire centralisé des boîtes de dialogue modales.
 *
 * Évite l'instanciation de boîtes de dialogue flottantes dispersées et assure
 * une présentation homogène des dialogues système.
 */
public class DialogManager {

    private static DialogManager instance;

    private DialogManager() {}

    public static DialogManager getInstance() {
        if (instance == null) {
            instance = new DialogManager();
        }
        return instance;
    }

    /**
     * Affiche un dialogue de confirmation modal.
     */
    public void showConfirm(String title, String message, Consumer<Boolean> callback) {
        runOnFx(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dialogs/confirm-dialog.fxml"));
                Parent root = loader.load();
                ConfirmDialogController controller = loader.getController();

                Stage stage = new Stage(StageStyle.UTILITY);
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.setTitle(title);
                Scene scene = new Scene(root);
                ThemeManager.getInstance().applyTheme(scene);
                stage.setScene(scene);

                controller.setup(title, message, confirmed -> {
                    if (callback != null) callback.accept(confirmed);
                });

                stage.showAndWait();
            } catch (Exception e) {
                // Fallback de secours si environnement headless
                if (callback != null) callback.accept(true);
            }
        });
    }

    /**
     * Affiche une boîte de dialogue d'erreur propre et détaillée.
     */
    public void showError(String title, String userMessage, String technicalDetails) {
        showError(title, userMessage, null, null, technicalDetails);
    }

    /**
     * Affiche une boîte de dialogue d'erreur enrichie avec cause probable, action corrective et statut d'opération.
     */
    public void showError(String title, String userMessage, String probableCause, String suggestedAction, String technicalDetails) {
        runOnFx(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dialogs/error-dialog.fxml"));
                Parent root = loader.load();
                ErrorDialogController controller = loader.getController();

                Stage stage = new Stage(StageStyle.UTILITY);
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.setTitle(title != null ? title : "Erreur Système");
                Scene scene = new Scene(root);
                ThemeManager.getInstance().applyTheme(scene);
                stage.setScene(scene);

                controller.setup(new AppErrorDTO("ERR-SYS-UI", userMessage, probableCause, suggestedAction, technicalDetails, true));
                stage.showAndWait();
            } catch (Exception e) {
                System.err.println("[DialogManager] Erreur affichage dialogue : " + userMessage + " (" + technicalDetails + ")");
            }
        });
    }

    /**
     * Affiche l'aide contextuelle non bloquante pour un module donné sans fermer ni bloquer l'écran en cours.
     */
    public void showContextualHelp(String moduleKey) {
        runOnFx(() -> {
            try {
                var optTopic = com.jenos.gestionecole.service.HelpService.getInstance().getTopicByModule(moduleKey);
                String title = optTopic.map(com.jenos.gestionecole.service.HelpService.HelpTopic::getTitle).orElse("Aide Contextuelle");
                String content = optTopic.map(com.jenos.gestionecole.service.HelpService.HelpTopic::getContent).orElse("Aucune aide contextuelle spécifique pour ce module.");
                String bestPractice = optTopic.map(com.jenos.gestionecole.service.HelpService.HelpTopic::getBestPractice).orElse("");

                Stage stage = new Stage(StageStyle.UTILITY);
                stage.initModality(Modality.NONE); // Non bloquant : l'utilisateur peut continuer à travailler
                stage.setTitle("Aide — " + title);

                javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(14);
                root.setStyle("-fx-padding: 20; -fx-background-color: -color-bg-default; -fx-pref-width: 480;");

                javafx.scene.control.Label lblTitle = new javafx.scene.control.Label(title);
                lblTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -color-text-primary;");

                javafx.scene.control.Label lblBody = new javafx.scene.control.Label(content);
                lblBody.setWrapText(true);
                lblBody.setStyle("-fx-font-size: 13px; -fx-text-fill: -color-text-secondary;");

                javafx.scene.layout.VBox boxAdvice = new javafx.scene.layout.VBox(6);
                boxAdvice.setStyle("-fx-background-color: rgba(59, 130, 246, 0.08); -fx-padding: 12; -fx-border-color: #3b82f6; -fx-border-width: 0 0 0 3;");
                javafx.scene.control.Label lblAdviceHeader = new javafx.scene.control.Label("Recommandation & Bonnes pratiques :");
                lblAdviceHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #2563eb;");
                javafx.scene.control.Label lblAdvice = new javafx.scene.control.Label(bestPractice.isBlank() ? "Respectez les procédures comptables officielles." : bestPractice);
                lblAdvice.setWrapText(true);
                lblAdvice.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-text-primary;");
                boxAdvice.getChildren().addAll(lblAdviceHeader, lblAdvice);

                javafx.scene.control.Button btnClose = new javafx.scene.control.Button("Compris");
                btnClose.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-padding: 6 16; -fx-background-radius: 4; -fx-cursor: hand;");
                btnClose.setOnAction(e -> stage.close());

                javafx.scene.layout.HBox actions = new javafx.scene.layout.HBox(btnClose);
                actions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

                root.getChildren().addAll(lblTitle, lblBody, boxAdvice, actions);

                Scene scene = new Scene(root);
                ThemeManager.getInstance().applyTheme(scene);
                stage.setScene(scene);
                stage.show();
            } catch (Exception e) {
                System.err.println("[DialogManager] Erreur affichage aide contextuelle : " + e.getMessage());
            }
        });
    }

    private void runOnFx(Runnable runnable) {
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            try {
                Platform.runLater(runnable);
            } catch (IllegalStateException e) {
                // Toolkit non initialisé (ex: headless test)
                runnable.run();
            }
        }
    }
}

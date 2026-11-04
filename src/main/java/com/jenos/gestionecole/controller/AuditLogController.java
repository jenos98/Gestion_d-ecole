package com.jenos.gestionecole.controller;

import com.jenos.gestionecole.model.AuditLog;
import com.jenos.gestionecole.service.AuditService;
import com.jenos.gestionecole.util.DialogManager;
import com.jenos.gestionecole.util.RoleGuard;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AuditLogController — Contrôleur du journal d'audit immuable.
 *
 * RÈGLES ABSOLUES DU BLOC 6 :
 * 1. Contrôle d'accès strict : réservé aux rôles autorisés (RoleGuard.canViewAuditLog()).
 * 2. Immutabilité absolue : lecture seule, aucune action de purge, modification ou suppression.
 * 3. Filtres multi-critères : par module, résultat (Succès/Échec), action et recherche par mot-clé.
 * 4. Export CSV sécurisé avec sanitization pour prévenir les injections.
 */
public class AuditLogController {

    @FXML private Button btnRefresh;
    @FXML private Button btnExport;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cmbModuleFilter;
    @FXML private ComboBox<String> cmbResultFilter;
    @FXML private ComboBox<String> cmbActionFilter;
    @FXML private Label lblTotalResults;

    @FXML private TableView<AuditLog> tableAudit;
    @FXML private TableColumn<AuditLog, String> colTimestamp;
    @FXML private TableColumn<AuditLog, String> colUser;
    @FXML private TableColumn<AuditLog, String> colModule;
    @FXML private TableColumn<AuditLog, String> colAction;
    @FXML private TableColumn<AuditLog, String> colResult;
    @FXML private TableColumn<AuditLog, String> colEntity;
    @FXML private TableColumn<AuditLog, String> colDetails;

    @FXML private VBox boxAccessDenied;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @FXML
    public void initialize() {
        if (!RoleGuard.canViewAuditLog() && !RoleGuard.can("audit.view")) {
            showAccessDenied();
            return;
        }

        initFilters();
        initTable();
        loadLogs();
    }

    private void showAccessDenied() {
        if (boxAccessDenied != null) {
            boxAccessDenied.setVisible(true);
            boxAccessDenied.setManaged(true);
        }
        if (tableAudit != null) {
            tableAudit.setVisible(false);
            tableAudit.setManaged(false);
        }
        if (btnRefresh != null) btnRefresh.setDisable(true);
        if (btnExport != null) btnExport.setDisable(true);
        if (txtSearch != null) txtSearch.setDisable(true);
        if (cmbModuleFilter != null) cmbModuleFilter.setDisable(true);
        if (cmbResultFilter != null) cmbResultFilter.setDisable(true);
        if (cmbActionFilter != null) cmbActionFilter.setDisable(true);
        if (lblTotalResults != null) lblTotalResults.setText("Accès non autorisé");
    }

    private void initFilters() {
        if (cmbModuleFilter != null) {
            List<String> modules = new java.util.ArrayList<>();
            modules.add("Tous les modules");
            for (AuditLog.AuditModule m : AuditLog.AuditModule.values()) {
                modules.add(m.getLabel());
            }
            cmbModuleFilter.setItems(FXCollections.observableArrayList(modules));
            cmbModuleFilter.getSelectionModel().selectFirst();
            cmbModuleFilter.setOnAction(e -> loadLogs());
        }

        if (cmbResultFilter != null) {
            cmbResultFilter.setItems(FXCollections.observableArrayList(
                    "Tous les résultats",
                    "Succès",
                    "Échec"
            ));
            cmbResultFilter.getSelectionModel().selectFirst();
            cmbResultFilter.setOnAction(e -> loadLogs());
        }

        if (cmbActionFilter != null) {
            cmbActionFilter.setItems(FXCollections.observableArrayList(
                    "Toutes les actions",
                    "CONNEXION",
                    "DÉCONNEXION",
                    "PAIEMENT",
                    "DEPENSE",
                    "SALAIRE",
                    "REDUCTION",
                    "EXEMPTION",
                    "AFFECTATION",
                    "MODIFICATION",
                    "BLOQUÉ",
                    "ÉCHEC"
            ));
            cmbActionFilter.getSelectionModel().selectFirst();
            cmbActionFilter.setOnAction(e -> loadLogs());
        }

        if (txtSearch != null) {
            txtSearch.textProperty().addListener((obs, oldVal, newVal) -> loadLogs());
        }
    }

    private void initTable() {
        if (tableAudit == null) return;

        colTimestamp.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getTimestamp() != null ? data.getValue().getTimestamp().format(FORMATTER) : ""
        ));

        colUser.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUsername()));

        if (colModule != null) {
            colModule.setCellValueFactory(data -> new SimpleStringProperty(
                    data.getValue().getModule() != null ? data.getValue().getModule().getLabel() : "Général"
            ));
        }

        colAction.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAction()));

        if (colResult != null) {
            colResult.setCellValueFactory(data -> new SimpleStringProperty(
                    data.getValue().getResult() != null ? data.getValue().getResult().getLabel() : "Succès"
            ));
            colResult.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setGraphic(null);
                        setText(null);
                    } else {
                        Label badge = new Label(item);
                        badge.getStyleClass().add("badge");
                        if ("Échec".equalsIgnoreCase(item)) {
                            badge.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #dc2626; -fx-font-weight: bold; -fx-padding: 3 8 3 8; -fx-background-radius: 6;");
                        } else {
                            badge.setStyle("-fx-background-color: #d1fae5; -fx-text-fill: #059669; -fx-font-weight: bold; -fx-padding: 3 8 3 8; -fx-background-radius: 6;");
                        }
                        setGraphic(badge);
                        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                        setAlignment(Pos.CENTER);
                    }
                }
            });
        }

        colEntity.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEntityName()));

        colDetails.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getDetails() != null ? data.getValue().getDetails() :
                        (data.getValue().getNewValue() != null ? data.getValue().getNewValue() : "—")
        ));
    }

    private void loadLogs() {
        if (!RoleGuard.canViewAuditLog() && !RoleGuard.can("audit.view")) {
            return;
        }

        List<AuditLog> list = AuditService.getInstance().getAllLogs();

        // 1. Filtre par module
        if (cmbModuleFilter != null) {
            String mod = cmbModuleFilter.getSelectionModel().getSelectedItem();
            if (mod != null && !"Tous les modules".equalsIgnoreCase(mod)) {
                list = list.stream()
                        .filter(l -> l.getModule() != null && l.getModule().getLabel().equalsIgnoreCase(mod))
                        .collect(Collectors.toList());
            }
        }

        // 2. Filtre par résultat
        if (cmbResultFilter != null) {
            String res = cmbResultFilter.getSelectionModel().getSelectedItem();
            if (res != null && !"Tous les résultats".equalsIgnoreCase(res)) {
                AuditLog.AuditResult target = "Succès".equalsIgnoreCase(res) ? AuditLog.AuditResult.SUCCES : AuditLog.AuditResult.ECHEC;
                list = list.stream()
                        .filter(l -> l.getResult() == target)
                        .collect(Collectors.toList());
            }
        }

        // 3. Filtre par action
        if (cmbActionFilter != null) {
            String act = cmbActionFilter.getSelectionModel().getSelectedItem();
            if (act != null && !"Toutes les actions".equalsIgnoreCase(act)) {
                list = list.stream()
                        .filter(l -> l.getAction() != null && l.getAction().toUpperCase().contains(act.toUpperCase()))
                        .collect(Collectors.toList());
            }
        }

        // 4. Filtre par mot-clé
        if (txtSearch != null) {
            String kw = txtSearch.getText();
            if (kw != null && !kw.isBlank()) {
                String lower = kw.toLowerCase().trim();
                list = list.stream()
                        .filter(log -> (log.getUsername() != null && log.getUsername().toLowerCase().contains(lower))
                                || (log.getAction() != null && log.getAction().toLowerCase().contains(lower))
                                || (log.getEntityName() != null && log.getEntityName().toLowerCase().contains(lower))
                                || (log.getDetails() != null && log.getDetails().toLowerCase().contains(lower))
                                || (log.getNewValue() != null && log.getNewValue().toLowerCase().contains(lower)))
                        .collect(Collectors.toList());
            }
        }

        if (tableAudit != null) {
            tableAudit.setItems(FXCollections.observableArrayList(list));
        }
        if (lblTotalResults != null) {
            lblTotalResults.setText("Total : " + list.size() + " événement" + (list.size() > 1 ? "s" : "") + " enregistré" + (list.size() > 1 ? "s" : ""));
        }
    }

    @FXML
    private void handleRefresh() {
        loadLogs();
    }

    @FXML
    private void handleExport() {
        if (!RoleGuard.can("audit.export") && !RoleGuard.canViewAuditLog()) {
            DialogManager.getInstance().showError("Accès Refusé", "Vous n'avez pas l'autorisation d'exporter le journal d'audit.", "Rôle non habilité.");
            return;
        }

        List<AuditLog> list = tableAudit != null ? tableAudit.getItems() : null;
        if (list == null || list.isEmpty()) {
            DialogManager.getInstance().showError("Export Impossible", "Le journal d'audit est vide pour les critères sélectionnés.", "Aucune donnée à exporter.");
            return;
        }

        try {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Exporter le journal d'audit");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier CSV (*.csv)", "*.csv"));
            chooser.setInitialFileName("journal_audit_" + System.currentTimeMillis() + ".csv");

            File file = null;
            if (btnExport != null && btnExport.getScene() != null && btnExport.getScene().getWindow() != null) {
                file = chooser.showSaveDialog(btnExport.getScene().getWindow());
            }

            if (file != null) {
                try (PrintWriter writer = new PrintWriter(file)) {
                    writer.println("Date;Utilisateur;Module;Action;Resultat;Entite;Details");
                    for (AuditLog log : list) {
                        String ts = log.getTimestamp() != null ? log.getTimestamp().format(FORMATTER) : "";
                        String mod = log.getModule() != null ? log.getModule().getLabel() : "Général";
                        String res = log.getResult() != null ? log.getResult().getLabel() : "Succès";
                        writer.printf("%s;%s;%s;%s;%s;%s;%s%n",
                                ts,
                                sanitize(log.getUsername()),
                                sanitize(mod),
                                sanitize(log.getAction()),
                                sanitize(res),
                                sanitize(log.getEntityName()),
                                sanitize(log.getDetails() != null ? log.getDetails() : log.getNewValue())
                        );
                    }
                }
            }
        } catch (Exception e) {
            DialogManager.getInstance().showError("Erreur d'exportation", "Impossible d'exporter le fichier CSV : " + e.getMessage(), e.toString());
        }
    }

    private String sanitize(String s) {
        if (s == null) return "";
        return s.replace(";", ",").replace("\n", " ").replace("\r", "");
    }
}

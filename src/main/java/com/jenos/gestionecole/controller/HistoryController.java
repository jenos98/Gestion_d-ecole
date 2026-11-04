package com.jenos.gestionecole.controller;

import com.jenos.gestionecole.controller.dialog.OperationDetailsController;
import com.jenos.gestionecole.model.*;
import com.jenos.gestionecole.service.AcademicStructureService;
import com.jenos.gestionecole.service.CashClosureService;
import com.jenos.gestionecole.service.HistoryService;
import com.jenos.gestionecole.util.MoneyFormatter;
import com.jenos.gestionecole.util.MoneyVisibilityManager;
import com.jenos.gestionecole.util.NavigationManager;
import com.jenos.gestionecole.util.Route;
import com.jenos.gestionecole.util.ThemeManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * HistoryController — Gestion de la traçabilité complète, de l'audit et du filtrage scolaire.
 *
 * RÈGLE ABSOLUE :
 * Filtrage scolaire en cascade descendante (Section → Promotion → Option → Classe),
 * avec l'Option d'étude comme citoyen indépendant de 1er rang.
 */
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HistoryController implements MoneyVisibilityManager.VisibilityObserver {

    private static final Logger logger = LoggerFactory.getLogger(HistoryController.class);

    // ── En-tête & Export ──
    @FXML private Label lblResultCount;
    @FXML private Button btnToggleAmounts;
    @FXML private Button btnRefresh;
    @FXML private Button btnExport;

    // ── Filtres de Période & Recherche textuelle ──
    @FXML private TextField txtSearch;
    @FXML private DatePicker dpStart;
    @FXML private DatePicker dpEnd;
    @FXML private Button btnSearch;

    // ── Filtres Opérationnels ──
    @FXML private ComboBox<String> cmbType;
    @FXML private ComboBox<String> cmbStatus;
    @FXML private ComboBox<String> cmbCashRegister;
    @FXML private TextField txtUser;

    // ── Cascade Scolaire ──
    @FXML private ComboBox<Section> cmbSection;
    @FXML private ComboBox<Promotion> cmbPromotion;
    @FXML private ComboBox<AcademicOption> cmbOption;
    @FXML private ComboBox<Classe> cmbClasse;
    @FXML private Button btnReset;

    // ── TableView ──
    @FXML private TableView<Operation> tableHistory;
    @FXML private TableColumn<Operation, String> colDateTime;
    @FXML private TableColumn<Operation, String> colRef;
    @FXML private TableColumn<Operation, String> colType;
    @FXML private TableColumn<Operation, String> colBeneficiary;
    @FXML private TableColumn<Operation, String> colSection;
    @FXML private TableColumn<Operation, String> colPromotion;
    @FXML private TableColumn<Operation, String> colOption;
    @FXML private TableColumn<Operation, String> colClasse;
    @FXML private TableColumn<Operation, String> colCur;
    @FXML private TableColumn<Operation, String> colAmount;
    @FXML private TableColumn<Operation, String> colMethod;
    @FXML private TableColumn<Operation, String> colCashRegister;
    @FXML private TableColumn<Operation, String> colUser;
    @FXML private TableColumn<Operation, String> colStatus;
    @FXML private TableColumn<Operation, Void> colActions;

    // ── Pagination ──
    @FXML private Label lblPageInfo;
    @FXML private Pagination pagination;

    private final HistoryService historyService = HistoryService.getInstance();
    private final AcademicStructureService academicService = AcademicStructureService.getInstance();
    private final CashClosureService cashService = CashClosureService.getInstance();

    private final ObservableList<Operation> operationList = FXCollections.observableArrayList();
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private List<Operation> lastFilteredResults = new ArrayList<>();

    @FXML
    public void initialize() {
        MoneyVisibilityManager.getInstance().registerObserver(this);

        setupFilters();
        setupAcademicCascade();
        setupTable();
        search();
    }

    private void setupFilters() {
        // Types
        List<String> types = new ArrayList<>();
        types.add("Tous les types");
        for (Operation.OperationType t : Operation.OperationType.values()) {
            types.add(t.name());
        }
        cmbType.setItems(FXCollections.observableArrayList(types));
        cmbType.setValue("Tous les types");

        // Statuts
        List<String> statuses = new ArrayList<>();
        statuses.add("Tous les statuts");
        for (Operation.OperationStatus s : Operation.OperationStatus.values()) {
            statuses.add(s.name());
        }
        cmbStatus.setItems(FXCollections.observableArrayList(statuses));
        cmbStatus.setValue("Tous les statuts");

        // Caisses
        List<String> registers = new ArrayList<>();
        registers.add("Toutes les caisses");
        for (CashRegister cr : cashService.getAllRegisters()) {
            registers.add(cr.getName());
        }
        cmbCashRegister.setItems(FXCollections.observableArrayList(registers));
        cmbCashRegister.setValue("Toutes les caisses");

        dpStart.setValue(LocalDate.now().minusDays(30));
        dpEnd.setValue(LocalDate.now());

        // Listeners
        cmbType.valueProperty().addListener((obs, o, n) -> search());
        cmbStatus.valueProperty().addListener((obs, o, n) -> search());
        cmbCashRegister.valueProperty().addListener((obs, o, n) -> search());
        dpStart.valueProperty().addListener((obs, o, n) -> search());
        dpEnd.valueProperty().addListener((obs, o, n) -> search());
        txtSearch.textProperty().addListener((obs, o, n) -> search());
        txtUser.textProperty().addListener((obs, o, n) -> search());
    }

    private void setupAcademicCascade() {
        cmbSection.setItems(FXCollections.observableArrayList(academicService.findAllSections()));

        // Section → Promotion
        cmbSection.valueProperty().addListener((obs, o, section) -> {
            if (section != null) {
                cmbPromotion.setItems(FXCollections.observableArrayList(academicService.findPromotionsBySection(section)));
            } else {
                cmbPromotion.setItems(FXCollections.observableArrayList());
            }
            cmbPromotion.setValue(null);
            cmbOption.setValue(null);
            cmbClasse.setValue(null);
            search();
        });

        // Promotion → Option
        cmbPromotion.valueProperty().addListener((obs, o, promo) -> {
            if (promo != null) {
                cmbOption.setItems(FXCollections.observableArrayList(academicService.findOptionsByPromotion(promo)));
            } else {
                cmbOption.setItems(FXCollections.observableArrayList());
            }
            cmbOption.setValue(null);
            cmbClasse.setValue(null);
            search();
        });

        // Option → Classe
        cmbOption.valueProperty().addListener((obs, o, option) -> {
            if (option != null) {
                cmbClasse.setItems(FXCollections.observableArrayList(academicService.findClassesByOption(option)));
            } else {
                cmbClasse.setItems(FXCollections.observableArrayList());
            }
            cmbClasse.setValue(null);
            search();
        });

        cmbClasse.valueProperty().addListener((obs, o, n) -> search());
    }

    private void setupTable() {
        colDateTime.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDateTime() != null ? c.getValue().getDateTime().format(dtf) : "—"));
        colRef.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getReferenceNumber()));
        colType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType().name()));
        colBeneficiary.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getBeneficiaryName() != null ? c.getValue().getBeneficiaryName() : c.getValue().getDescription()));
        colSection.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSectionName()));
        colPromotion.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPromotionName()));
        colOption.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getOptionName()));
        colClasse.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getClassName()));
        colCur.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCurrency()));
        colAmount.setCellValueFactory(c -> new SimpleStringProperty(
                MoneyFormatter.display(c.getValue().getAmount(), c.getValue().getCurrency())
        ));
        colMethod.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPaymentMethod()));
        colCashRegister.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCashRegisterName()));
        colUser.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCashierUsername()));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().name()));

        // Style badge pour le statut
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("VALIDEE".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: #198754; -fx-font-weight: bold;");
                    } else if ("ANNULEE".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;");
                    } else if ("CORRIGEE".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: #fd7e14; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        // Colonne Action : [👁 Voir]
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnView = new Button("👁");
            private final HBox box = new HBox(btnView);

            {
                btnView.getStyleClass().add("btn-cell-action");
                btnView.setTooltip(new Tooltip("Consulter le détail de l'opération"));
                box.setAlignment(Pos.CENTER);

                btnView.setOnAction(e -> {
                    Operation op = getTableView().getItems().get(getIndex());
                    if (op != null) {
                        openOperationDetails(op);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        tableHistory.setItems(operationList);
        tableHistory.setPlaceholder(new Label("Aucune opération trouvée pour ces critères."));
    }

    private void search() {
        LocalDate start = dpStart.getValue();
        LocalDate end = dpEnd.getValue();

        Operation.OperationType type = null;
        String tVal = cmbType.getValue();
        if (tVal != null && !"Tous les types".equalsIgnoreCase(tVal)) {
            try { type = Operation.OperationType.valueOf(tVal); } catch (Exception ignored) {}
        }

        Operation.OperationStatus status = null;
        String sVal = cmbStatus.getValue();
        if (sVal != null && !"Tous les statuts".equalsIgnoreCase(sVal)) {
            try { status = Operation.OperationStatus.valueOf(sVal); } catch (Exception ignored) {}
        }

        String reg = cmbCashRegister.getValue();
        if ("Toutes les caisses".equalsIgnoreCase(reg)) reg = null;

        String user = txtUser.getText() != null ? txtUser.getText().trim() : null;
        String kw = txtSearch.getText() != null ? txtSearch.getText().trim() : null;

        // Cascade scolaire
        String sec = cmbSection.getValue() != null ? cmbSection.getValue().getName() : null;
        String promo = cmbPromotion.getValue() != null ? cmbPromotion.getValue().getName() : null;
        String opt = cmbOption.getValue() != null ? cmbOption.getValue().getName() : null;
        String cl = cmbClasse.getValue() != null ? cmbClasse.getValue().getName() : null;

        List<Operation> results = historyService.search(start, end, type, status, user, reg, kw, sec, promo, opt, cl);
        this.lastFilteredResults = results;
        operationList.setAll(results);

        lblResultCount.setText(results.size() + " résultat(s)");
        if (lblPageInfo != null) {
            lblPageInfo.setText("Affichage 1 à " + results.size() + " sur " + results.size() + " opérations");
        }
    }

    private void openOperationDetails(Operation op) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dialogs/operation-detail-dialog.fxml"));
            ScrollPane root = loader.load();
            OperationDetailsController ctrl = loader.getController();
            ctrl.setOperation(op);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Détail de l'Opération — " + op.getReferenceNumber());
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            Scene scene = new Scene(root, 650, 520);
            ThemeManager.getInstance().applyTheme(scene);
            dialogStage.setScene(scene);
            dialogStage.showAndWait();

            search(); // Rafraîchir en cas d'annulation ou correction
        } catch (IOException e) {
            logger.error("Erreur ouverture dialogue détails opération : {}", e.getMessage(), e);
        }
    }

    @FXML
    private void handleSearch() {
        search();
    }

    @FXML
    private void handleRefresh() {
        search();
    }

    @FXML
    private void handleReset() {
        txtSearch.clear();
        txtUser.clear();
        cmbType.setValue("Tous les types");
        cmbStatus.setValue("Tous les statuts");
        cmbCashRegister.setValue("Toutes les caisses");
        cmbSection.setValue(null);
        cmbPromotion.setValue(null);
        cmbOption.setValue(null);
        cmbClasse.setValue(null);
        dpStart.setValue(LocalDate.now().minusDays(30));
        dpEnd.setValue(LocalDate.now());
        search();
    }

    @FXML
    private void handleToggleAmounts() {
        if (MoneyVisibilityManager.getInstance().isVisible()) {
            MoneyVisibilityManager.getInstance().hideAll();
        } else {
            MoneyVisibilityManager.getInstance().showAll();
        }
    }

    @FXML
    private void handleExport() {
        openExportDialog("Historique des Opérations", lastFilteredResults);
    }

    public static void openExportDialog(String reportTitle, List<?> filteredData) {
        try {
            FXMLLoader loader = new FXMLLoader(HistoryController.class.getResource("/fxml/dialogs/export-dialog.fxml"));
            DialogPane pane = loader.load();
            com.jenos.gestionecole.controller.dialog.ExportDialogController ctrl = loader.getController();
            ctrl.setupExportContext(reportTitle, filteredData);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Exportation de Données");
            dialog.setDialogPane(pane);
            var optResult = dialog.showAndWait();

            if (optResult.isPresent() && optResult.get().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                if (ctrl.isAuthorized()) {
                    String targetPath = ctrl.getExportFilePath();
                    File targetFile = new File(targetPath);
                    String ext = targetPath.contains(".") ? targetPath.substring(targetPath.lastIndexOf('.')) : ".csv";
                    boolean ok = com.jenos.gestionecole.service.ExportService.getInstance().exportList(reportTitle, filteredData, targetFile, ext);
                    if (ok) {
                        com.jenos.gestionecole.util.DialogManager.getInstance().showInfo("Export Réussi",
                                "Les données ont été exportées avec succès dans :\n" + targetFile.getAbsolutePath());
                    } else {
                        com.jenos.gestionecole.util.DialogManager.getInstance().showError("Erreur d'exportation",
                                "Impossible d'exporter le fichier vers " + targetFile.getAbsolutePath(),
                                "Vérifiez les permissions d'accès au dossier de destination.");
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Erreur lors de l'exportation de l'historique", e);
        }
    }

    @Override
    public void onShow() {
        tableHistory.refresh();
    }

    @Override
    public void onHide() {
        tableHistory.refresh();
    }
}

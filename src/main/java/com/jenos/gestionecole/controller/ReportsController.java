package com.jenos.gestionecole.controller;

import com.jenos.gestionecole.model.*;
import com.jenos.gestionecole.service.AcademicStructureService;
import com.jenos.gestionecole.service.CashClosureService;
import com.jenos.gestionecole.service.ReportService;
import com.jenos.gestionecole.util.NavigationManager;
import com.jenos.gestionecole.util.RoleGuard;
import com.jenos.gestionecole.util.Route;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ReportsController — Gestion du catalogue de rapports, des filtres dynamiques et de la génération.
 */
public class ReportsController {

    // ── En-tête ──
    @FXML private Label lblReportDescription;
    @FXML private Button btnViewHistory;

    // ── Catalogue ──
    @FXML private ListView<ReportType> lvReportCatalog;

    // ── Titre du rapport ──
    @FXML private Label lblSelectedReportTitle;
    @FXML private Label lblSelectedReportSubtitle;

    // ── Filtres académiques ──
    @FXML private VBox panelAcademicFilters;
    @FXML private ComboBox<String> cmbAnnee;
    @FXML private ComboBox<Section> cmbSection;
    @FXML private ComboBox<Promotion> cmbPromotion;
    @FXML private ComboBox<AcademicOption> cmbOption;
    @FXML private ComboBox<Classe> cmbClasse;
    @FXML private ComboBox<Student.StudentStatus> cmbStudentStatus;

    // ── Filtres financiers ──
    @FXML private VBox panelCashFilters;
    @FXML private DatePicker dpStartDate;
    @FXML private DatePicker dpEndDate;
    @FXML private ComboBox<String> cmbCurrency;
    @FXML private ComboBox<CashRegister> cmbCashRegister;
    @FXML private ComboBox<String> cmbPaymentMethod;

    // ── Journal d'audit des rapports ──
    @FXML private VBox panelReportLogs;
    @FXML private TableView<ReportLog> tblReportLogs;
    @FXML private TableColumn<ReportLog, String> colLogDate;
    @FXML private TableColumn<ReportLog, String> colLogId;
    @FXML private TableColumn<ReportLog, String> colLogType;
    @FXML private TableColumn<ReportLog, String> colLogUser;
    @FXML private TableColumn<ReportLog, String> colLogPeriod;
    @FXML private TableColumn<ReportLog, String> colLogStatus;

    // ── Actions ──
    @FXML private Button btnResetFilters;
    @FXML private Button btnExport;
    @FXML private Button btnGenerateAndPreview;

    private final ReportService reportService = ReportService.getInstance();
    private final AcademicStructureService academicService = AcademicStructureService.getInstance();
    private final CashClosureService cashService = CashClosureService.getInstance();

    private final ObservableList<ReportLog> reportLogsList = FXCollections.observableArrayList();
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        setupCatalog();
        setupAcademicCascade();
        setupFinancialFilters();
        setupLogsTable();

        // Sélectionner le 1er élément par défaut
        if (!lvReportCatalog.getItems().isEmpty()) {
            lvReportCatalog.getSelectionModel().select(0);
        }
    }

    private void setupCatalog() {
        ObservableList<ReportType> availableReports = FXCollections.observableArrayList();

        // Filtrage selon le rôle
        for (ReportType type : ReportType.values()) {
            if (type == ReportType.HISTORIQUE_RAPPORTS) {
                availableReports.add(type);
                continue;
            }

            // Contrôle par rôle
            boolean allowed = true;
            if (type == ReportType.RAPPORT_PERSONNEL && !RoleGuard.canAccessStaff()) {
                allowed = false;
            }
            if ((type == ReportType.RAPPORT_DEPENSES || type == ReportType.RAPPORT_RECETTES) && !RoleGuard.canViewOwnCashRegister()) {
                allowed = false;
            }
            if (allowed) {
                availableReports.add(type);
            }
        }

        lvReportCatalog.setItems(availableReports);
        lvReportCatalog.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                onReportTypeSelected(newVal);
            }
        });
    }

    private void onReportTypeSelected(ReportType type) {
        lblSelectedReportTitle.setText(type.getLabel());

        boolean isHistory = (type == ReportType.HISTORIQUE_RAPPORTS);
        panelReportLogs.setVisible(isHistory);
        panelReportLogs.setManaged(isHistory);

        if (isHistory) {
            lblSelectedReportSubtitle.setText("Registre officiel de tous les documents produits.");
            panelAcademicFilters.setVisible(false);
            panelAcademicFilters.setManaged(false);
            panelCashFilters.setVisible(false);
            panelCashFilters.setManaged(false);
            btnGenerateAndPreview.setDisable(true);
            loadReportLogs();
            return;
        }

        btnGenerateAndPreview.setDisable(false);

        boolean isAcademic = (type == ReportType.RAPPORT_ELEVES
                || type == ReportType.RAPPORT_PAR_CLASSE
                || type == ReportType.RAPPORT_PAR_OPTION
                || type == ReportType.LISTE_APPEL_CLASSE
                || type == ReportType.TABLEAU_EFFECTIFS
                || type == ReportType.RAPPORT_IMPAYES
                || type == ReportType.RAPPORT_PAIEMENTS);

        boolean isFinancial = (type == ReportType.RAPPORT_JOURNALIER
                || type == ReportType.ETAT_DE_CAISSE
                || type == ReportType.RAPPORT_PAR_CAISSE
                || type == ReportType.RAPPORT_PAR_CAISSIERE
                || type == ReportType.RAPPORT_DEPENSES
                || type == ReportType.RAPPORT_RECETTES
                || type == ReportType.RAPPORT_PAIEMENTS
                || type == ReportType.RAPPORT_IMPAYES
                || type == ReportType.RAPPORT_MENSUEL
                || type == ReportType.RAPPORT_ANNUEL);

        panelAcademicFilters.setVisible(isAcademic);
        panelAcademicFilters.setManaged(isAcademic);

        panelCashFilters.setVisible(isFinancial);
        panelCashFilters.setManaged(isFinancial);

        lblSelectedReportSubtitle.setText("Configurez les paramètres ci-dessous pour ce rapport.");
    }

    private void setupAcademicCascade() {
        cmbAnnee.setItems(FXCollections.observableArrayList(academicService.findAllAcademicYears()));
        if (!cmbAnnee.getItems().isEmpty()) cmbAnnee.setValue(cmbAnnee.getItems().get(0));

        cmbSection.setItems(FXCollections.observableArrayList(academicService.findAllSections()));

        cmbSection.valueProperty().addListener((obs, o, sec) -> {
            if (sec != null) {
                cmbPromotion.setItems(FXCollections.observableArrayList(academicService.findPromotionsBySection(sec)));
            } else {
                cmbPromotion.setItems(FXCollections.observableArrayList());
            }
            cmbPromotion.setValue(null);
            cmbOption.setValue(null);
            cmbClasse.setValue(null);
        });

        cmbPromotion.valueProperty().addListener((obs, o, promo) -> {
            if (promo != null) {
                cmbOption.setItems(FXCollections.observableArrayList(academicService.findOptionsByPromotion(promo)));
            } else {
                cmbOption.setItems(FXCollections.observableArrayList());
            }
            cmbOption.setValue(null);
            cmbClasse.setValue(null);
        });

        cmbOption.valueProperty().addListener((obs, o, opt) -> {
            if (opt != null) {
                cmbClasse.setItems(FXCollections.observableArrayList(academicService.findClassesByOption(opt)));
            } else {
                cmbClasse.setItems(FXCollections.observableArrayList());
            }
            cmbClasse.setValue(null);
        });

        cmbStudentStatus.setItems(FXCollections.observableArrayList(Student.StudentStatus.values()));
    }

    private void setupFinancialFilters() {
        dpStartDate.setValue(LocalDate.now().minusDays(30));
        dpEndDate.setValue(LocalDate.now());

        cmbCurrency.setItems(FXCollections.observableArrayList("Toutes", "USD", "CDF"));
        cmbCurrency.setValue("Toutes");

        cmbCashRegister.setItems(FXCollections.observableArrayList(cashService.getAllRegisters()));

        cmbPaymentMethod.setItems(FXCollections.observableArrayList(
                "Tous les modes", "ESPECES", "MOBILE_MONEY", "VIREMENT", "CHEQUE"
        ));
        cmbPaymentMethod.setValue("Tous les modes");
    }

    private void setupLogsTable() {
        colLogDate.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getGeneratedAt() != null ? c.getValue().getGeneratedAt().format(dtf) : "—"));
        colLogId.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getReportId()));
        colLogType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTypeLabel()));
        colLogUser.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getGeneratedBy()));
        colLogPeriod.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPeriod()));
        colLogStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().name()));

        tblReportLogs.setItems(reportLogsList);
    }

    private void loadReportLogs() {
        List<ReportLog> logs = reportService.getReportLogs();
        reportLogsList.setAll(logs);
    }

    @FXML
    private void handleGenerateAndPreview() {
        ReportType type = lvReportCatalog.getSelectionModel().getSelectedItem();
        if (type == null || type == ReportType.HISTORIQUE_RAPPORTS) return;

        Map<String, Object> params = new HashMap<>();
        params.put("annee", cmbAnnee.getValue());
        params.put("section", cmbSection.getValue());
        params.put("promotion", cmbPromotion.getValue());
        params.put("option", cmbOption.getValue());
        params.put("classe", cmbClasse.getValue());
        params.put("startDate", dpStartDate.getValue());
        params.put("endDate", dpEndDate.getValue());
        params.put("currency", cmbCurrency.getValue());
        params.put("cashRegister", cmbCashRegister.getValue());

        ReportDocument doc = reportService.generateReport(type, params);
        reportService.setLastGeneratedReport(doc);

        NavigationManager.getInstance().navigate(Route.REPORT_PREVIEW);
    }

    @FXML
    private void handleExport() {
        ReportType type = lvReportCatalog.getSelectionModel().getSelectedItem();
        String title = type != null ? type.getLabel() : "Rapport";
        HistoryController.openExportDialog(title, List.of());
    }

    @FXML
    private void handleResetFilters() {
        cmbSection.setValue(null);
        cmbPromotion.setValue(null);
        cmbOption.setValue(null);
        cmbClasse.setValue(null);
        cmbStudentStatus.setValue(null);
        cmbCurrency.setValue("Toutes");
        cmbPaymentMethod.setValue("Tous les modes");
        dpStartDate.setValue(LocalDate.now().minusDays(30));
        dpEndDate.setValue(LocalDate.now());
    }

    @FXML
    private void handleViewReportHistory() {
        lvReportCatalog.getSelectionModel().select(ReportType.HISTORIQUE_RAPPORTS);
    }
}

package com.jenos.gestionecole.controller;

import com.jenos.gestionecole.model.AcademicYear;
import com.jenos.gestionecole.service.AcademicYearService;
import com.jenos.gestionecole.util.RoleGuard;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * AcademicYearController — Contrôleur de gestion des années scolaires (academic-year.fxml).
 */
public class AcademicYearController {

    @FXML private Label lblCurrentActiveYear;
    @FXML private Button btnNewYear;
    @FXML private Label lblFormMessage;

    @FXML private TextField txtCode;
    @FXML private TextField txtName;
    @FXML private DatePicker dpStart;
    @FXML private DatePicker dpEnd;
    @FXML private Button btnReset;
    @FXML private Button btnSave;

    @FXML private TableView<AcademicYear> tableYears;
    @FXML private TableColumn<AcademicYear, String> tcCode;
    @FXML private TableColumn<AcademicYear, String> tcName;
    @FXML private TableColumn<AcademicYear, String> tcStart;
    @FXML private TableColumn<AcademicYear, String> tcEnd;
    @FXML private TableColumn<AcademicYear, String> tcStatus;
    @FXML private TableColumn<AcademicYear, Void> tcActions;

    private final AcademicYearService yearService = AcademicYearService.getInstance();
    private final ObservableList<AcademicYear> yearList = FXCollections.observableArrayList();
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {
        setupTable();
        loadYears();
    }

    private void setupTable() {
        tcCode.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCode()));
        tcName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        tcStart.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getStartDate() != null ? c.getValue().getStartDate().format(dtf) : "—"));
        tcEnd.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getEndDate() != null ? c.getValue().getEndDate().format(dtf) : "—"));

        tcStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().name()));
        tcStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("ACTIVE".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: #198754; -fx-font-weight: bold;");
                    } else if ("PLANIFIEE".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: #0d6efd; -fx-font-weight: bold;");
                    } else if ("CLOTUREE".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: #fd7e14; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #6c757d; -fx-font-weight: bold;");
                    }
                }
            }
        });

        tcActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnActivate = new Button("Activer");
            private final Button btnClose = new Button("Clôturer");
            private final HBox box = new HBox(6, btnActivate, btnClose);

            {
                box.setAlignment(Pos.CENTER);
                btnActivate.getStyleClass().add("btn-secondary");
                btnClose.getStyleClass().add("btn-secondary");

                btnActivate.setOnAction(e -> {
                    AcademicYear y = getTableView().getItems().get(getIndex());
                    if (y != null) {
                        if (!RoleGuard.can("academic.manage")) {
                            new Alert(Alert.AlertType.ERROR, "Permission insuffisante pour activer une année scolaire.").showAndWait();
                            return;
                        }
                        yearService.activateYear(y.getId());
                        loadYears();
                    }
                });

                btnClose.setOnAction(e -> {
                    AcademicYear y = getTableView().getItems().get(getIndex());
                    if (y != null) {
                        if (!RoleGuard.can("academic.manage")) {
                            new Alert(Alert.AlertType.ERROR, "Permission insuffisante pour clôturer une année scolaire.").showAndWait();
                            return;
                        }
                        yearService.closeYear(y.getId());
                        loadYears();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    AcademicYear y = getTableView().getItems().get(getIndex());
                    if (y != null) {
                        btnActivate.setDisable(y.getStatus() == AcademicYear.YearStatus.ACTIVE);
                        btnClose.setDisable(y.getStatus() != AcademicYear.YearStatus.ACTIVE);
                    }
                    setGraphic(box);
                }
            }
        });

        tableYears.setItems(yearList);
    }

    private void loadYears() {
        yearList.setAll(yearService.getAllYears());
        AcademicYear active = yearService.getActiveYear();
        if (active != null) {
            lblCurrentActiveYear.setText("Année Active : " + active.getCode());
        } else {
            lblCurrentActiveYear.setText("Aucune Année Active");
        }
    }

    @FXML
    private void handleNewYear() {
        txtCode.clear();
        txtName.clear();
        dpStart.setValue(LocalDate.now());
        dpEnd.setValue(LocalDate.now().plusMonths(10));
        hideMessage();
    }

    @FXML
    private void handleSaveYear() {
        if (!RoleGuard.can("academic.manage")) {
            showMessage("⛔ Permission insuffisante pour planifier une année scolaire.", false);
            return;
        }

        String code = txtCode.getText() != null ? txtCode.getText().trim() : "";
        String name = txtName.getText() != null ? txtName.getText().trim() : "";
        LocalDate start = dpStart.getValue();
        LocalDate end = dpEnd.getValue();

        if (code.isBlank() || name.isBlank() || start == null || end == null) {
            showMessage("Veuillez renseigner tous les champs obligatoires.", false);
            return;
        }

        if (end.isBefore(start)) {
            showMessage("La date de clôture ne peut pas précéder la date d'ouverture.", false);
            return;
        }

        try {
            yearService.createYear(code, name, start, end);
            loadYears();
            handleResetForm();
            showMessage("✓ L'année scolaire a été planifiée avec succès.", true);
        } catch (Exception ex) {
            showMessage(ex.getMessage(), false);
        }
    }

    @FXML
    private void handleResetForm() {
        txtCode.clear();
        txtName.clear();
        dpStart.setValue(null);
        dpEnd.setValue(null);
        hideMessage();
    }

    private void showMessage(String msg, boolean success) {
        lblFormMessage.setText(msg);
        lblFormMessage.setVisible(true);
        lblFormMessage.setManaged(true);
        if (success) {
            lblFormMessage.setStyle("-fx-background-color: #d1e7dd; -fx-text-fill: #0f5132; -fx-padding: 8; -fx-background-radius: 4;");
        } else {
            lblFormMessage.setStyle("-fx-background-color: #f8d7da; -fx-text-fill: #842029; -fx-padding: 8; -fx-background-radius: 4;");
        }
    }

    private void hideMessage() {
        lblFormMessage.setVisible(false);
        lblFormMessage.setManaged(false);
    }
}

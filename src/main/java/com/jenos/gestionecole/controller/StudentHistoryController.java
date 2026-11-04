package com.jenos.gestionecole.controller;

import com.jenos.gestionecole.model.Operation;
import com.jenos.gestionecole.model.Student;
import com.jenos.gestionecole.service.HistoryService;
import com.jenos.gestionecole.util.MoneyFormatter;
import com.jenos.gestionecole.util.NavigationManager;
import com.jenos.gestionecole.util.Route;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * StudentHistoryController — Historique financier et administratif dédié à un élève.
 */
public class StudentHistoryController {

    @FXML private Label lblStudentName;
    @FXML private Label lblStudentInfo;

    @FXML private TableView<Operation> tableHistory;
    @FXML private TableColumn<Operation, String> colDate;
    @FXML private TableColumn<Operation, String> colRef;
    @FXML private TableColumn<Operation, String> colLabel;
    @FXML private TableColumn<Operation, String> colAmount;
    @FXML private TableColumn<Operation, String> colMethod;
    @FXML private TableColumn<Operation, String> colStatus;

    private Student student;
    private final HistoryService historyService = HistoryService.getInstance();
    private final ObservableList<Operation> studentOperations = FXCollections.observableArrayList();

    public void setStudent(Student s) {
        this.student = s;
        if (s != null) {
            lblStudentName.setText(s.getFullName());
            lblStudentInfo.setText("Matricule : " + s.getMatricule() + " | Classe : " + s.getClassName() + " | Option : " + s.getOptionName());
            loadStudentHistory();
        }
    }

    @FXML
    public void initialize() {
        setupTable();
    }

    private void setupTable() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        colDate.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDateTime().format(dtf)));
        colRef.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getReferenceNumber()));
        colLabel.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescription()));
        colAmount.setCellValueFactory(c -> new SimpleStringProperty(
                MoneyFormatter.display(c.getValue().getAmount(), c.getValue().getCurrency())
        ));
        colMethod.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPaymentMethod()));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().name()));

        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label lbl = new Label(item);
                    lbl.getStyleClass().add("badge");
                    if ("VALIDEE".equalsIgnoreCase(item)) lbl.getStyleClass().add("badge-success");
                    else if ("ANNULEE".equalsIgnoreCase(item)) lbl.getStyleClass().add("badge-danger");
                    else lbl.getStyleClass().add("badge-warning");
                    setGraphic(lbl);
                }
            }
        });

        tableHistory.setItems(studentOperations);
    }

    private void loadStudentHistory() {
        if (student == null) return;
        List<Operation> all = historyService.search(null, null, Operation.OperationType.PAIEMENT_ELEVE, null, null, null, student.getNom());
        studentOperations.setAll(all);
    }

    @FXML
    private void handleBack() {
        NavigationManager.getInstance().navigate(Route.STUDENTS);
    }
}

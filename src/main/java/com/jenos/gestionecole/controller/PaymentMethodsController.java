package com.jenos.gestionecole.controller;

import com.jenos.gestionecole.model.PaymentMethodConfig;
import com.jenos.gestionecole.service.PaymentMethodService;
import com.jenos.gestionecole.util.RoleGuard;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

/**
 * PaymentMethodsController — Contrôleur de gestion des modes de paiement (payment-methods.fxml).
 */
public class PaymentMethodsController {

    @FXML private Label lblFormTitle;
    @FXML private Label lblMessage;
    @FXML private TextField txtCode;
    @FXML private TextField txtName;
    @FXML private CheckBox chkRequiresReference;
    @FXML private Button btnReset;
    @FXML private Button btnSave;

    @FXML private TableView<PaymentMethodConfig> tableMethods;
    @FXML private TableColumn<PaymentMethodConfig, String> tcCode;
    @FXML private TableColumn<PaymentMethodConfig, String> tcName;
    @FXML private TableColumn<PaymentMethodConfig, String> tcRequiresRef;
    @FXML private TableColumn<PaymentMethodConfig, String> tcStatus;
    @FXML private TableColumn<PaymentMethodConfig, Void> tcActions;

    private final PaymentMethodService methodService = PaymentMethodService.getInstance();
    private final ObservableList<PaymentMethodConfig> methodList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTable();
        loadMethods();
    }

    private void setupTable() {
        tcCode.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCode()));
        tcName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        tcRequiresRef.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isRequiresReference() ? "OUI" : "NON"));
        tcRequiresRef.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("OUI".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: #fd7e14; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #6c757d;");
                    }
                }
            }
        });

        tcStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isActive() ? "Actif" : "Inactif"));
        tcStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("Actif".equalsIgnoreCase(item)
                            ? "-fx-text-fill: #198754; -fx-font-weight: bold;"
                            : "-fx-text-fill: #dc3545; -fx-font-weight: bold;");
                }
            }
        });

        tcActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnToggle = new Button("Statut");
            private final HBox box = new HBox(btnToggle);

            {
                box.setAlignment(Pos.CENTER);
                btnToggle.getStyleClass().add("btn-secondary");

                btnToggle.setOnAction(e -> {
                    PaymentMethodConfig m = getTableView().getItems().get(getIndex());
                    if (m != null) {
                        if (!RoleGuard.can("payment_method.manage")) {
                            new Alert(Alert.AlertType.ERROR, "Permission insuffisante pour modifier les modes de paiement.").showAndWait();
                            return;
                        }
                        methodService.toggleActive(m.getId());
                        loadMethods();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        tableMethods.setItems(methodList);
    }

    private void loadMethods() {
        methodList.setAll(methodService.getAllMethods());
    }

    @FXML
    private void handleSave() {
        if (!RoleGuard.can("payment_method.manage")) {
            showMessage("⛔ Permission insuffisante pour ajouter des modes de paiement.", false);
            return;
        }

        String code = txtCode.getText() != null ? txtCode.getText().trim() : "";
        String name = txtName.getText() != null ? txtName.getText().trim() : "";
        boolean reqRef = chkRequiresReference.isSelected();

        if (code.isBlank() || name.isBlank()) {
            showMessage("Le code et le libellé sont obligatoires.", false);
            return;
        }

        try {
            methodService.addMethod(code, name, reqRef);
            handleReset();
            loadMethods();
            showMessage("✓ Mode de paiement enregistré avec succès.", true);
        } catch (Exception ex) {
            showMessage(ex.getMessage(), false);
        }
    }

    @FXML
    private void handleReset() {
        txtCode.clear();
        txtName.clear();
        chkRequiresReference.setSelected(false);
        hideMessage();
    }

    private void showMessage(String msg, boolean success) {
        lblMessage.setText(msg);
        lblMessage.setVisible(true);
        lblMessage.setManaged(true);
        if (success) {
            lblMessage.setStyle("-fx-background-color: #d1e7dd; -fx-text-fill: #0f5132; -fx-padding: 8; -fx-background-radius: 4;");
        } else {
            lblMessage.setStyle("-fx-background-color: #f8d7da; -fx-text-fill: #842029; -fx-padding: 8; -fx-background-radius: 4;");
        }
    }

    private void hideMessage() {
        lblMessage.setVisible(false);
        lblMessage.setManaged(false);
    }
}

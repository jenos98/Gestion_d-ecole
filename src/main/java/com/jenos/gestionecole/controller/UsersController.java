package com.jenos.gestionecole.controller;

import com.jenos.gestionecole.model.User;
import com.jenos.gestionecole.service.CashClosureService;
import com.jenos.gestionecole.service.UserService;
import com.jenos.gestionecole.util.NavigationManager;
import com.jenos.gestionecole.util.RoleGuard;
import com.jenos.gestionecole.util.Route;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * UsersController — Contrôleur de l'écran de consultation et gestion des utilisateurs (users.fxml).
 */
public class UsersController {

    @FXML private Label lblUserCount;
    @FXML private Button btnAddUser;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cmbRole;
    @FXML private ComboBox<String> cmbStatus;
    @FXML private ComboBox<String> cmbCashRegister;
    @FXML private TableView<User> tableUsers;

    @FXML private TableColumn<User, String> tcName;
    @FXML private TableColumn<User, String> tcUsername;
    @FXML private TableColumn<User, String> tcRole;
    @FXML private TableColumn<User, String> tcCashRegister;
    @FXML private TableColumn<User, String> tcStatus;
    @FXML private TableColumn<User, String> tcLastLogin;
    @FXML private TableColumn<User, Void> tcActions;
    @FXML private Label lblStatusInfo;

    private final UserService userService = UserService.getInstance();
    private final ObservableList<User> userList = FXCollections.observableArrayList();
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        setupFilters();
        setupTable();
        loadUsers();
    }

    private void setupFilters() {
        cmbRole.setItems(FXCollections.observableArrayList("Tous les rôles",
                RoleGuard.Role.PROMOTEUR.name(),
                RoleGuard.Role.CAISSIERE.name(),
                RoleGuard.Role.DIRECTEUR.name(),
                RoleGuard.Role.SECRETARIAT.name(),
                RoleGuard.Role.ADMIN_TECHNIQUE.name()));
        cmbRole.setValue("Tous les rôles");

        cmbStatus.setItems(FXCollections.observableArrayList("Tous les statuts",
                User.UserStatus.ACTIF.name(),
                User.UserStatus.BLOQUE.name(),
                User.UserStatus.DESACTIVE.name()));
        cmbStatus.setValue("Tous les statuts");

        var registers = CashClosureService.getInstance().getAllRegisters();
        var regNames = FXCollections.observableArrayList("Toutes les caisses");
        for (var r : registers) regNames.add(r.getName());
        cmbCashRegister.setItems(regNames);
        cmbCashRegister.setValue("Toutes les caisses");

        txtSearch.textProperty().addListener((obs, o, n) -> filterUsers());
        cmbRole.valueProperty().addListener((obs, o, n) -> filterUsers());
        cmbStatus.valueProperty().addListener((obs, o, n) -> filterUsers());
        cmbCashRegister.valueProperty().addListener((obs, o, n) -> filterUsers());
    }

    private void setupTable() {
        tcName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFullName()));
        tcUsername.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUsername()));
        tcRole.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRole() != null ? c.getValue().getRole().name() : "—"));
        tcCashRegister.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCashRegisterCode() != null ? c.getValue().getCashRegisterCode() : "—"));

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
                    if ("ACTIF".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: #198754; -fx-font-weight: bold;");
                    } else if ("BLOQUE".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: #fd7e14; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;");
                    }
                }
            }
        });

        tcLastLogin.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getLastLogin() != null ? c.getValue().getLastLogin().format(dtf) : "Jamais"));

        tcActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("✏️");
            private final Button btnResetPwd = new Button("🔑");
            private final Button btnToggleStatus = new Button("🔒");
            private final HBox box = new HBox(6, btnEdit, btnResetPwd, btnToggleStatus);

            {
                box.setAlignment(Pos.CENTER);
                btnEdit.setTooltip(new Tooltip("Modifier les informations de l'utilisateur"));
                btnResetPwd.setTooltip(new Tooltip("Réinitialiser le mot de passe"));
                btnToggleStatus.setTooltip(new Tooltip("Activer / Désactiver le compte"));

                btnEdit.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    if (u != null) {
                        UserFormController.setUserToEdit(u);
                        NavigationManager.getInstance().navigate(Route.USER_FORM);
                    }
                });

                btnResetPwd.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    if (u != null) {
                        TextInputDialog dlg = new TextInputDialog("Pass2026!");
                        dlg.setTitle("Réinitialisation Mot de Passe");
                        dlg.setHeaderText("Nouveau mot de passe temporaire pour : " + u.getUsername());
                        dlg.setContentText("Mot de passe :");
                        dlg.showAndWait().ifPresent(newPass -> {
                            if (!newPass.isBlank()) {
                                userService.resetPassword(u.getId(), newPass);
                                new Alert(Alert.AlertType.INFORMATION, "Mot de passe réinitialisé avec succès.").showAndWait();
                            }
                        });
                    }
                });

                btnToggleStatus.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    if (u != null) {
                        if (u.getStatus() == User.UserStatus.ACTIF) {
                            userService.deactivateUser(u.getId(), "Désactivation par l'administrateur");
                        } else {
                            userService.activateUser(u.getId());
                        }
                        filterUsers();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    User u = getTableView().getItems().get(getIndex());
                    if (u != null && u.getStatus() == User.UserStatus.ACTIF) {
                        btnToggleStatus.setText("⛔");
                        btnToggleStatus.setStyle("-fx-text-fill: #dc3545;");
                    } else {
                        btnToggleStatus.setText("✓");
                        btnToggleStatus.setStyle("-fx-text-fill: #198754;");
                    }
                    setGraphic(box);
                }
            }
        });

        tableUsers.setItems(userList);
        tableUsers.setPlaceholder(new Label("Aucun compte utilisateur trouvé."));
    }

    private void loadUsers() {
        filterUsers();
    }

    private void filterUsers() {
        String kw = txtSearch.getText();
        RoleGuard.Role role = null;
        String rStr = cmbRole.getValue();
        if (rStr != null && !"Tous les rôles".equalsIgnoreCase(rStr)) {
            try { role = RoleGuard.Role.valueOf(rStr); } catch (Exception ignored) {}
        }

        User.UserStatus status = null;
        String sStr = cmbStatus.getValue();
        if (sStr != null && !"Tous les statuts".equalsIgnoreCase(sStr)) {
            try { status = User.UserStatus.valueOf(sStr); } catch (Exception ignored) {}
        }

        String reg = cmbCashRegister.getValue();
        List<User> filtered = userService.search(kw, role, status, reg);
        userList.setAll(filtered);
        lblUserCount.setText(filtered.size() + " utilisateur(s)");
    }

    @FXML
    private void handleNewUser() {
        UserFormController.setUserToEdit(null);
        NavigationManager.getInstance().navigate(Route.USER_FORM);
    }

    @FXML
    private void handleResetFilters() {
        txtSearch.clear();
        cmbRole.setValue("Tous les rôles");
        cmbStatus.setValue("Tous les statuts");
        cmbCashRegister.setValue("Toutes les caisses");
        filterUsers();
    }
}

package com.jenos.gestionecole.controller;

import com.jenos.gestionecole.model.SystemNotification;
import com.jenos.gestionecole.service.NotificationService;
import com.jenos.gestionecole.util.DialogManager;
import com.jenos.gestionecole.util.NavigationManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * NotificationsController — Contrôleur de l'écran des notifications.
 *
 * RÈGLE ABSOLUE :
 * Les notifications critiques ne peuvent être ignorées sans résolution de la situation de blocage.
 */
public class NotificationsController implements NotificationService.NotificationObserver {

    @FXML private Label lblUnreadCount;
    @FXML private Button btnMarkAllRead;
    @FXML private Button btnRefresh;
    @FXML private ComboBox<String> cmbPriorityFilter;
    @FXML private CheckBox chkUnreadOnly;
    @FXML private Label lblTotalResults;

    @FXML private TableView<SystemNotification> tableNotifications;
    @FXML private TableColumn<SystemNotification, String> colPriority;
    @FXML private TableColumn<SystemNotification, String> colTimestamp;
    @FXML private TableColumn<SystemNotification, String> colTitle;
    @FXML private TableColumn<SystemNotification, String> colMessage;
    @FXML private TableColumn<SystemNotification, String> colStatus;
    @FXML private TableColumn<SystemNotification, Void> colActions;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        initFilters();
        initTable();
        NotificationService.getInstance().addObserver(this);
        loadNotifications();
    }

    private void initFilters() {
        cmbPriorityFilter.setItems(FXCollections.observableArrayList(
                "Toutes les sévérités",
                "Critique",
                "Avertissement",
                "Information"
        ));
        cmbPriorityFilter.getSelectionModel().selectFirst();
        cmbPriorityFilter.setOnAction(e -> loadNotifications());
        chkUnreadOnly.setOnAction(e -> loadNotifications());
    }

    private void initTable() {
        colPriority.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPriority().getLabel()));
        colPriority.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add("badge");
                    if ("Critique".equalsIgnoreCase(item)) {
                        badge.getStyleClass().add("badge-danger");
                    } else if ("Avertissement".equalsIgnoreCase(item)) {
                        badge.getStyleClass().add("badge-warning");
                    } else {
                        badge.getStyleClass().add("badge-info");
                    }
                    setGraphic(badge);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        colTimestamp.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getTimestamp() != null ? data.getValue().getTimestamp().format(FORMATTER) : ""
        ));

        colTitle.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitle()));
        colMessage.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMessage()));

        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isRead() ? "Lue" : "Non lue"));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label lbl = new Label(item);
                    lbl.getStyleClass().add(item.equals("Non lue") ? "badge-danger" : "badge-secondary");
                    lbl.getStyleClass().add("badge");
                    setGraphic(lbl);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnAction = new Button("Traiter ➔");
            private final Button btnDismiss = new Button("✕");
            private final HBox box = new HBox(6, btnAction, btnDismiss);

            {
                btnAction.getStyleClass().add("btn-primary");
                btnAction.setStyle("-fx-padding: 3 8 3 8; -fx-font-size: 11px;");
                btnDismiss.getStyleClass().add("btn-secondary");
                btnDismiss.setStyle("-fx-padding: 3 6 3 6; -fx-font-size: 11px;");
                box.setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    SystemNotification notif = getTableRow().getItem();

                    btnAction.setVisible(notif.getActionRoute() != null);
                    btnAction.setOnAction(e -> {
                        NotificationService.getInstance().markAsRead(notif.getId());
                        if (notif.getActionRoute() != null) {
                            NavigationManager.getInstance().navigate(notif.getActionRoute());
                        }
                    });

                    btnDismiss.setOnAction(e -> {
                        try {
                            NotificationService.getInstance().dismissNotification(notif.getId());
                        } catch (IllegalStateException ex) {
                            DialogManager.getInstance().showError("Action Impossible", ex.getMessage(), "Priorité critique persistante.");
                        }
                    });

                    setGraphic(box);
                }
            }
        });
    }

    private void loadNotifications() {
        NotificationService service = NotificationService.getInstance();
        List<SystemNotification> list = service.getAllNotifications();

        String priority = cmbPriorityFilter.getSelectionModel().getSelectedItem();
        if ("Critique".equalsIgnoreCase(priority)) {
            list = list.stream().filter(n -> n.getPriority() == SystemNotification.Priority.CRITICAL).collect(Collectors.toList());
        } else if ("Avertissement".equalsIgnoreCase(priority)) {
            list = list.stream().filter(n -> n.getPriority() == SystemNotification.Priority.WARNING).collect(Collectors.toList());
        } else if ("Information".equalsIgnoreCase(priority)) {
            list = list.stream().filter(n -> n.getPriority() == SystemNotification.Priority.INFO).collect(Collectors.toList());
        }

        if (chkUnreadOnly.isSelected()) {
            list = list.stream().filter(n -> !n.isRead()).collect(Collectors.toList());
        }

        tableNotifications.setItems(FXCollections.observableArrayList(list));

        int unread = service.getUnreadCount();
        lblUnreadCount.setText(unread + " alerte" + (unread > 1 ? "s non lues" : " non lue"));
        lblTotalResults.setText("Total : " + list.size() + " alerte" + (list.size() > 1 ? "s" : ""));
    }

    @Override
    public void onNotificationsChanged() {
        loadNotifications();
    }

    @FXML
    private void handleMarkAllRead() {
        NotificationService.getInstance().markAllAsRead();
    }

    @FXML
    private void handleRefresh() {
        loadNotifications();
    }
}

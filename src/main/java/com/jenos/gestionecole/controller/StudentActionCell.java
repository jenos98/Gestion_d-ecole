package com.jenos.gestionecole.controller;

import com.jenos.gestionecole.model.Student;
import com.jenos.gestionecole.util.RoleGuard;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;

/**
 * StudentActionCell — Cellule d'actions pour le TableView des étudiants.
 *
 * Boutons contextuels :
 * [Voir] [Modifier] [Historique] [Désactiver]
 *
 * RÈGLE ABSOLUE : Pas de bouton "Supprimer" physique.
 */
public class StudentActionCell extends TableCell<Student, Void> {

    private final HBox container;
    private final Button btnView;
    private final Button btnEdit;
    private final Button btnHistory;
    private final Button btnDeactivate;
    private final Button btnArchive;

    public interface ActionHandler {
        void onView(Student student);
        void onEdit(Student student);
        void onHistory(Student student);
        void onDeactivate(Student student);
        void onArchive(Student student);
    }

    public StudentActionCell(ActionHandler handler) {
        btnView       = new Button("👁");
        btnEdit       = new Button("✏");
        btnHistory    = new Button("📋");
        btnDeactivate = new Button("🚫");
        btnArchive    = new Button("📦");

        btnView.setTooltip(new Tooltip("Voir la fiche étudiant (student-details.fxml)"));
        btnEdit.setTooltip(new Tooltip("Modifier les informations (student-form.fxml)"));
        btnHistory.setTooltip(new Tooltip("Historique des paiements & scolarité"));
        btnDeactivate.setTooltip(new Tooltip("Désactiver l'étudiant (non destructif)"));
        btnArchive.setTooltip(new Tooltip("Archiver l'étudiant (non destructif)"));

        btnView.getStyleClass().add("btn-cell-action");
        btnEdit.getStyleClass().add("btn-cell-action");
        btnHistory.getStyleClass().add("btn-cell-action");
        btnDeactivate.getStyleClass().addAll("btn-cell-action", "btn-cell-action-danger");
        btnArchive.getStyleClass().addAll("btn-cell-action", "btn-cell-action-neutral");

        btnView.setOnAction(e -> {
            Student s = getTableView().getItems().get(getIndex());
            if (s != null && handler != null) handler.onView(s);
        });

        btnEdit.setOnAction(e -> {
            Student s = getTableView().getItems().get(getIndex());
            if (s != null && handler != null) handler.onEdit(s);
        });

        btnHistory.setOnAction(e -> {
            Student s = getTableView().getItems().get(getIndex());
            if (s != null && handler != null) handler.onHistory(s);
        });

        btnDeactivate.setOnAction(e -> {
            Student s = getTableView().getItems().get(getIndex());
            if (s != null && handler != null) handler.onDeactivate(s);
        });

        btnArchive.setOnAction(e -> {
            Student s = getTableView().getItems().get(getIndex());
            if (s != null && handler != null) handler.onArchive(s);
        });

        // Contrôle des permissions de désactivation
        if (!RoleGuard.canDeactivateStudent()) {
            btnDeactivate.setVisible(false);
            btnDeactivate.setManaged(false);
            btnArchive.setVisible(false);
            btnArchive.setManaged(false);
        }

        container = new HBox(4, btnView, btnEdit, btnHistory, btnDeactivate, btnArchive);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(2, 4, 2, 4));
    }

    @Override
    protected void updateItem(Void item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || getIndex() >= getTableView().getItems().size()) {
            setGraphic(null);
        } else {
            Student s = getTableView().getItems().get(getIndex());
            if (s != null) {
                btnDeactivate.setDisable(s.getStatut() == Student.StudentStatus.INACTIF);
                btnArchive.setDisable(s.getStatut() == Student.StudentStatus.ARCHIVE);
            }
            setGraphic(container);
        }
    }
}

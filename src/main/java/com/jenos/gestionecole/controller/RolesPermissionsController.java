package com.jenos.gestionecole.controller;

import com.jenos.gestionecole.util.RoleGuard;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;

/**
 * RolesPermissionsController — Contrôleur de la matrice des rôles et habilitations (roles-permissions.fxml).
 */
public class RolesPermissionsController {

    @FXML private ComboBox<RoleGuard.Role> cmbRoleSelector;

    // Élèves
    @FXML private CheckBox chkStudentView;
    @FXML private CheckBox chkStudentCreate;
    @FXML private CheckBox chkStudentEdit;
    @FXML private CheckBox chkStudentArchive;

    // Personnel
    @FXML private CheckBox chkStaffView;
    @FXML private CheckBox chkStaffCreate;
    @FXML private CheckBox chkStaffEdit;
    @FXML private CheckBox chkStaffArchive;

    // Caisse
    @FXML private CheckBox chkCashOpen;
    @FXML private CheckBox chkCashViewOwn;
    @FXML private CheckBox chkCashViewAll;
    @FXML private CheckBox chkCashReportGen;
    @FXML private CheckBox chkCashReportPrint;
    @FXML private CheckBox chkCashClose;
    @FXML private CheckBox chkCashForceClose;

    // Paiements
    @FXML private CheckBox chkPaymentCreate;
    @FXML private CheckBox chkPaymentViewOwn;
    @FXML private CheckBox chkPaymentViewAll;
    @FXML private CheckBox chkPaymentCorrect;
    @FXML private CheckBox chkPaymentCancel;

    // Dépenses
    @FXML private CheckBox chkExpenseCreate;
    @FXML private CheckBox chkExpenseViewOwn;
    @FXML private CheckBox chkExpenseViewAll;
    @FXML private CheckBox chkExpenseApprove;
    @FXML private CheckBox chkExpenseCancel;

    // Rapports
    @FXML private CheckBox chkReportView;
    @FXML private CheckBox chkReportFinancial;
    @FXML private CheckBox chkReportStudent;
    @FXML private CheckBox chkReportStaff;
    @FXML private CheckBox chkReportExport;

    // Administration
    @FXML private CheckBox chkUserManage;
    @FXML private CheckBox chkRoleView;
    @FXML private CheckBox chkSchoolEdit;
    @FXML private CheckBox chkAcademicManage;
    @FXML private CheckBox chkFeeManage;
    @FXML private CheckBox chkPaymentMethodManage;
    @FXML private CheckBox chkBudgetManage;

    @FXML
    public void initialize() {
        cmbRoleSelector.setItems(FXCollections.observableArrayList(RoleGuard.Role.values()));
        cmbRoleSelector.setValue(RoleGuard.Role.CAISSIERE);
        cmbRoleSelector.valueProperty().addListener((obs, oldVal, role) -> updatePermissionMatrix(role));

        updatePermissionMatrix(RoleGuard.Role.CAISSIERE);
    }

    private void updatePermissionMatrix(RoleGuard.Role role) {
        if (role == null) return;

        RoleGuard.Role previous = RoleGuard.getCurrentRole();
        try {
            RoleGuard.setCurrentRole(role);

            // Élèves
            chkStudentView.setSelected(RoleGuard.can("student.view"));
            chkStudentCreate.setSelected(RoleGuard.can("student.create"));
            chkStudentEdit.setSelected(RoleGuard.can("student.edit"));
            chkStudentArchive.setSelected(RoleGuard.can("student.archive"));

            // Personnel
            chkStaffView.setSelected(RoleGuard.can("staff.view"));
            chkStaffCreate.setSelected(RoleGuard.can("staff.create"));
            chkStaffEdit.setSelected(RoleGuard.can("staff.edit"));
            chkStaffArchive.setSelected(RoleGuard.can("staff.archive"));

            // Caisse
            chkCashOpen.setSelected(RoleGuard.can("cash.open"));
            chkCashViewOwn.setSelected(RoleGuard.can("cash.view.own"));
            chkCashViewAll.setSelected(RoleGuard.can("cash.view.all"));
            chkCashReportGen.setSelected(RoleGuard.can("cash.report.generate"));
            chkCashReportPrint.setSelected(RoleGuard.can("cash.report.print"));
            chkCashClose.setSelected(RoleGuard.can("cash.close"));
            chkCashForceClose.setSelected(RoleGuard.can("cash.forceclose"));

            // Paiements
            chkPaymentCreate.setSelected(RoleGuard.can("payment.create"));
            chkPaymentViewOwn.setSelected(RoleGuard.can("payment.view.own"));
            chkPaymentViewAll.setSelected(RoleGuard.can("payment.view.all"));
            chkPaymentCorrect.setSelected(RoleGuard.can("payment.correct"));
            chkPaymentCancel.setSelected(RoleGuard.can("payment.cancel"));

            // Dépenses
            chkExpenseCreate.setSelected(RoleGuard.can("expense.create"));
            chkExpenseViewOwn.setSelected(RoleGuard.can("expense.view.own"));
            chkExpenseViewAll.setSelected(RoleGuard.can("expense.view.all"));
            chkExpenseApprove.setSelected(RoleGuard.can("expense.approve"));
            chkExpenseCancel.setSelected(RoleGuard.can("expense.cancel"));

            // Rapports
            chkReportView.setSelected(RoleGuard.can("report.view"));
            chkReportFinancial.setSelected(RoleGuard.can("report.financial"));
            chkReportStudent.setSelected(RoleGuard.can("report.student"));
            chkReportStaff.setSelected(RoleGuard.can("report.staff"));
            chkReportExport.setSelected(RoleGuard.can("report.export"));

            // Administration
            chkUserManage.setSelected(RoleGuard.can("user.manage"));
            chkRoleView.setSelected(RoleGuard.can("role.view"));
            chkSchoolEdit.setSelected(RoleGuard.can("school.edit"));
            chkAcademicManage.setSelected(RoleGuard.can("academic.manage"));
            chkFeeManage.setSelected(RoleGuard.can("fee.manage"));
            chkPaymentMethodManage.setSelected(RoleGuard.can("payment_method.manage"));
            chkBudgetManage.setSelected(RoleGuard.can("budget.manage"));
        } finally {
            RoleGuard.setCurrentRole(previous);
        }
    }
}

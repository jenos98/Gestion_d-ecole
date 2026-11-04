package com.jenos.gestionecole.controller;

import com.jenos.gestionecole.model.Payslip;
import com.jenos.gestionecole.service.StaffService;
import com.jenos.gestionecole.util.NavigationManager;
import com.jenos.gestionecole.util.Route;
import javafx.fxml.FXML;
import javafx.print.PrinterJob;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;

/**
 * PayslipPreviewController — Contrôleur de l'aperçu et de l'impression du bulletin de paie officiel.
 *
 * RÈGLE ABSOLUE : Les montants USD et CDF sont traités séparément,
 * JAMAIS additionnés ni convertis implicitement.
 */
public class PayslipPreviewController {

    @FXML private VBox payslipSheet;
    @FXML private Label lblPayslipNumber;
    @FXML private Label lblPeriod;
    @FXML private Label lblDate;

    @FXML private Label lblStaffName;
    @FXML private Label lblStaffMatricule;
    @FXML private Label lblStaffRole;

    @FXML private Label lblBaseSalary;
    @FXML private Label lblAllowances;
    @FXML private Label lblAdvances;
    @FXML private Label lblDeductions;
    @FXML private Label lblNetPayable;

    @FXML private Button btnClose;
    @FXML private Button btnPrint;

    private Payslip currentPayslip;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {
        if (currentPayslip == null) {
            Payslip last = StaffService.getInstance().getLastGeneratedPayslip();
            if (last != null) {
                setPayslip(last);
            }
        }
    }

    public void setPayslip(Payslip p) {
        this.currentPayslip = p;
        if (p == null) return;

        lblPayslipNumber.setText(p.getPayslipNumber() != null ? p.getPayslipNumber() : "—");
        lblPeriod.setText("Période : " + p.getPeriodLabel());
        lblDate.setText("Date : " + (p.getPaymentDate() != null ? p.getPaymentDate().format(dtf) : "—"));

        if (p.getStaff() != null) {
            lblStaffName.setText(p.getStaff().getFullName());
            lblStaffMatricule.setText(p.getStaff().getMatricule() != null ? p.getStaff().getMatricule() : "—");
            lblStaffRole.setText(p.getStaff().getCategorie() + " - " + p.getStaff().getFonction());
        }

        String cur = p.getCurrency() != null ? p.getCurrency() : "USD";
        if ("USD".equalsIgnoreCase(cur)) {
            lblBaseSalary.setText(String.format("%.2f USD", p.getBaseSalary()));
            lblAllowances.setText(String.format("+ %.2f USD", p.getAllowances()));
            lblAdvances.setText(String.format("- %.2f USD", p.getAdvancesDeducted()));
            lblDeductions.setText(String.format("- %.2f USD", p.getOtherDeductions()));
            lblNetPayable.setText(String.format("%.2f USD", p.getNetPayable()));
        } else {
            lblBaseSalary.setText(String.format("%,.0f CDF", p.getBaseSalary()));
            lblAllowances.setText(String.format("+ %,.0f CDF", p.getAllowances()));
            lblAdvances.setText(String.format("- %,.0f CDF", p.getAdvancesDeducted()));
            lblDeductions.setText(String.format("- %,.0f CDF", p.getOtherDeductions()));
            lblNetPayable.setText(String.format("%,.0f CDF", p.getNetPayable()));
        }
    }

    @FXML
    private void handlePrint() {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null) {
            boolean success = job.printPage(payslipSheet);
            if (success) {
                job.endJob();
            }
        }
    }

    @FXML
    private void handleClose() {
        NavigationManager.getInstance().navigate(Route.STAFF);
    }
}

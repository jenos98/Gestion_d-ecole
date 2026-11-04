package com.jenos.gestionecole.controller;

import com.jenos.gestionecole.model.Payslip;
import com.jenos.gestionecole.model.SalaryRecord;
import com.jenos.gestionecole.model.Staff;
import com.jenos.gestionecole.service.StaffPayrollService;
import com.jenos.gestionecole.service.StaffService;
import com.jenos.gestionecole.util.NavigationManager;
import com.jenos.gestionecole.util.RoleGuard;
import com.jenos.gestionecole.util.Route;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * StaffDetailsController — Contrôleur de la fiche détaillée d'un membre du personnel.
 *
 * RÈGLE ABSOLUE : Les montants USD et CDF sont traités séparément,
 * JAMAIS additionnés ni convertis implicitement.
 */
public class StaffDetailsController {

    private static final Logger logger = LoggerFactory.getLogger(StaffDetailsController.class);

    // En-tête
    @FXML private Label lblFullName;
    @FXML private Label lblFonctionHeader;
    @FXML private Label lblMatricule;
    @FXML private Label lblStatus;
    @FXML private Button btnBack;

    // Informations personnelles
    @FXML private Label lblName;
    @FXML private Label lblPrenom;
    @FXML private Label lblGenre;
    @FXML private Label lblDateEmbauche;
    @FXML private Label lblQualification;
    @FXML private Label lblTelephone;
    @FXML private Label lblEmail;
    @FXML private Label lblAdresse;

    // Affectation
    @FXML private Label lblCategorie;
    @FXML private Label lblFonction;
    @FXML private Label lblPoste;

    // Rémunération & Paie
    @FXML private VBox cardPayroll;
    @FXML private Label lblCurrentPeriod;
    @FXML private Label lblBaseSalary;
    @FXML private Label lblAllowances;
    @FXML private Label lblAdvances;
    @FXML private Label lblNetPayable;

    // Actions
    @FXML private Button btnEdit;
    @FXML private Button btnPayslip;
    @FXML private Button btnPay;

    private Staff currentStaff;
    private final StaffService staffService = StaffService.getInstance();

    @FXML
    public void initialize() {
        applyPermissions();
    }

    private void applyPermissions() {
        boolean canViewSalary = RoleGuard.canViewSalaries();
        if (cardPayroll != null) {
            cardPayroll.setVisible(canViewSalary);
            cardPayroll.setManaged(canViewSalary);
        }
        if (btnPayslip != null) {
            btnPayslip.setVisible(canViewSalary);
            btnPayslip.setManaged(canViewSalary);
        }
    }

    public void setStaff(Staff staff) {
        this.currentStaff = staff;
        if (staff == null) return;

        lblFullName.setText(staff.getFullName());
        lblFonctionHeader.setText(staff.getFonction() != null ? staff.getFonction() : "Personnel");
        lblMatricule.setText(staff.getMatricule() != null ? staff.getMatricule() : "—");

        Staff.StaffStatus st = staff.getStatut() != null ? staff.getStatut() : Staff.StaffStatus.ACTIF;
        lblStatus.setText(st.getLabel());
        lblStatus.getStyleClass().removeAll("badge-success", "badge-warning", "badge-danger", "badge-neutral");
        if (st == Staff.StaffStatus.ACTIF) {
            lblStatus.getStyleClass().add("badge-success");
        } else if (st == Staff.StaffStatus.EN_CONGE) {
            lblStatus.getStyleClass().add("badge-warning");
        } else {
            lblStatus.getStyleClass().add("badge-danger");
        }

        lblName.setText((staff.getNom() != null ? staff.getNom().toUpperCase() : "") + " " + (staff.getPostnom() != null ? staff.getPostnom() : ""));
        lblPrenom.setText(staff.getPrenom() != null ? staff.getPrenom() : "—");
        lblGenre.setText("F".equalsIgnoreCase(staff.getGenre()) ? "Féminin" : "Masculin");
        lblDateEmbauche.setText(staff.getDateEmbauche() != null ? staff.getDateEmbauche().toString() : "—");
        lblQualification.setText(staff.getQualification() != null && !staff.getQualification().isBlank() ? staff.getQualification() : "—");
        lblTelephone.setText(staff.getTelephone() != null && !staff.getTelephone().isBlank() ? staff.getTelephone() : "—");
        lblEmail.setText(staff.getEmail() != null && !staff.getEmail().isBlank() ? staff.getEmail() : "—");
        lblAdresse.setText(staff.getAdresse() != null && !staff.getAdresse().isBlank() ? staff.getAdresse() : "—");

        lblCategorie.setText(staff.getCategorie() != null ? staff.getCategorie() : "—");
        lblFonction.setText(staff.getFonction() != null ? staff.getFonction() : "—");
        lblPoste.setText(staff.getPoste() != null ? staff.getPoste() : "—");

        // Rémunération calculée via StaffPayrollService
        LocalDate now = LocalDate.now();
        int month = now.getMonthValue();
        int year = now.getYear();
        lblCurrentPeriod.setText("Période : " + month + "/" + year);

        StaffPayrollService payroll = StaffPayrollService.getInstance();
        double activeAdvances = payroll.getTotalActiveAdvances(staff.getId(), staff.getDeviseSalaire());
        double base = staff.getSalaireBase();
        double net = Math.max(0, base - activeAdvances);
        String cur = staff.getDeviseSalaire();

        if ("USD".equalsIgnoreCase(cur)) {
            lblBaseSalary.setText(String.format("%.0f USD", base));
            lblAllowances.setText("0 USD");
            lblAdvances.setText(String.format("- %.0f USD", activeAdvances));
            lblNetPayable.setText(String.format("%.0f USD", net));
        } else {
            lblBaseSalary.setText(String.format("%,.0f CDF", base));
            lblAllowances.setText("0 CDF");
            lblAdvances.setText(String.format("- %,.0f CDF", activeAdvances));
            lblNetPayable.setText(String.format("%,.0f CDF", net));
        }
    }

    @FXML
    private void handleBack() {
        NavigationManager.getInstance().navigate(Route.STAFF);
    }

    @FXML
    private void handleEdit() {
        if (currentStaff == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/staff-form.fxml"));
            Node node = loader.load();
            StaffFormController ctrl = loader.getController();
            ctrl.setStaff(currentStaff);
            NavigationManager.getInstance().getContentContainer().getChildren().setAll(node);
        } catch (IOException e) {
            logger.error("Erreur lors de l'ouverture du formulaire d'édition du personnel", e);
        }
    }

    @FXML
    private void handleGeneratePayslip() {
        if (currentStaff == null) return;
        if (btnPayslip != null) btnPayslip.setDisable(true);

        try {
            StaffPayrollService payroll = StaffPayrollService.getInstance();
            LocalDate now = LocalDate.now();
            String period = now.getMonthValue() + "/" + now.getYear();

            List<SalaryRecord> existing = payroll.getSalaryRepository().findRecordsByStaff(currentStaff.getId()).stream()
                    .filter(r -> period.equals(r.getPeriod()))
                    .toList();

            SalaryRecord rec;
            if (!existing.isEmpty()) {
                rec = existing.get(0);
            } else {
                double activeAdvances = payroll.getTotalActiveAdvances(currentStaff.getId(), currentStaff.getDeviseSalaire());
                rec = payroll.prepareSalary(currentStaff.getId(), period, now.getMonthValue(), now.getYear(),
                        SalaryRecord.Frequency.MENSUELLE, 0, 0, 0, activeAdvances);
                payroll.validateSalary(rec.getId(), "Directeur");
            }
            Payslip p = payroll.generatePayslip(rec.getId());
            staffService.setLastGeneratedPayslip(p);
            NavigationManager.getInstance().navigate(Route.PAYSLIP_PREVIEW);
        } catch (Exception e) {
            logger.error("Erreur lors de la génération du bulletin de paie", e);
        } finally {
            if (btnPayslip != null) btnPayslip.setDisable(false);
        }
    }

    @FXML
    private void handleMakePayment() {
        NavigationManager.getInstance().navigate(Route.PAYMENTS);
    }
}

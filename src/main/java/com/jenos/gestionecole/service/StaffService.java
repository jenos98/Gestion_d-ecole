package com.jenos.gestionecole.service;

import com.jenos.gestionecole.model.Payslip;
import com.jenos.gestionecole.model.Staff;
import com.jenos.gestionecole.repository.StaffRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * StaffService — Service métier pour la gestion des ressources humaines et des rémunérations.
 *
 * RÈGLE ABSOLUE : Les montants USD et CDF sont traités séparément,
 * JAMAIS additionnés ni convertis implicitement.
 */
public class StaffService {

    private static StaffService instance;
    private final StaffRepository staffRepository;
    private Payslip lastGeneratedPayslip;

    public static class PayrollSummary {
        public double baseSalary = 0.0;
        public double allowances = 0.0;
        public double advancesDeducted = 0.0;
        public double otherDeductions = 0.0;
        public double netPayable = 0.0;
        public String currency = "USD";
    }

    private StaffService() {
        this.staffRepository = new StaffRepository();
    }

    public static StaffService getInstance() {
        if (instance == null) {
            instance = new StaffService();
        }
        return instance;
    }

    public List<Staff> search(String keyword, String categorie, String fonction, Staff.StaffStatus statut) {
        return staffRepository.search(keyword, categorie, fonction, statut);
    }

    public Staff findById(int id) {
        return staffRepository.findById(id);
    }

    public Staff findByMatricule(String matricule) {
        return staffRepository.findByMatricule(matricule);
    }

    public void save(Staff staff) throws IllegalArgumentException {
        if (staff == null) {
            throw new IllegalArgumentException("La fiche employé ne peut pas être null.");
        }

        if (staff.getNom() == null || staff.getNom().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom de famille est obligatoire.");
        }

        if (staff.getPostnom() == null || staff.getPostnom().trim().isEmpty()) {
            throw new IllegalArgumentException("Le postnom est obligatoire.");
        }

        if (staff.getSalaireBase() < 0) {
            throw new IllegalArgumentException("Le salaire de base ne peut pas être négatif.");
        }

        if (staff.getDeviseSalaire() == null || (!"USD".equalsIgnoreCase(staff.getDeviseSalaire()) && !"CDF".equalsIgnoreCase(staff.getDeviseSalaire()))) {
            throw new IllegalArgumentException("La devise salariale doit être obligatoirement USD ou CDF.");
        }

        staffRepository.save(staff);
    }

    public void updateStatus(int staffId, Staff.StaffStatus status) {
        staffRepository.updateStatus(staffId, status);
    }

    public PayrollSummary getPayrollSummary(Staff staff, int month, int year) {
        PayrollSummary summary = new PayrollSummary();
        if (staff == null) return summary;

        summary.baseSalary = staff.getSalaireBase();
        summary.currency = staff.getDeviseSalaire();

        // Primes et avances simulées cohérentes ou issues des décaissements
        if (staff.getId() == 1) {
            summary.allowances = 50.0; // Prime de titularisation
            summary.advancesDeducted = 100.0; // Avance perçue
        } else if (staff.getId() == 2) {
            summary.allowances = 100.0; // Indemnité de direction
            summary.advancesDeducted = 0.0;
        }

        summary.otherDeductions = 0.0;
        summary.netPayable = Math.max(0, summary.baseSalary + summary.allowances - summary.advancesDeducted - summary.otherDeductions);
        return summary;
    }

    public Payslip generatePayslip(Staff staff, int month, int year, double allowances, double deductions, String remarks) {
        if (staff == null) return null;

        PayrollSummary sum = getPayrollSummary(staff, month, year);
        if (allowances > 0) sum.allowances = allowances;
        if (deductions > 0) sum.otherDeductions = deductions;
        sum.netPayable = Math.max(0, sum.baseSalary + sum.allowances - sum.advancesDeducted - sum.otherDeductions);

        Payslip p = new Payslip();
        p.setId((int) (System.currentTimeMillis() % 100000));
        p.setPayslipNumber(String.format("PAY-%d-%02d-%03d", year, month, staff.getId()));
        p.setStaff(staff);
        p.setMonth(month);
        p.setYear(year);
        p.setBaseSalary(sum.baseSalary);
        p.setAllowances(sum.allowances);
        p.setAdvancesDeducted(sum.advancesDeducted);
        p.setOtherDeductions(sum.otherDeductions);
        p.setNetPayable(sum.netPayable);
        p.setCurrency(sum.currency);
        p.setRemarks(remarks);
        p.setPaymentDate(LocalDate.now());
        p.setGeneratedAt(LocalDateTime.now());
        p.setCashierUsername("Jenos");

        this.lastGeneratedPayslip = p;
        return p;
    }

    public Payslip getLastGeneratedPayslip() {
        if (lastGeneratedPayslip == null) {
            List<Staff> list = staffRepository.findAll();
            if (!list.isEmpty()) {
                generatePayslip(list.get(0), LocalDate.now().getMonthValue(), LocalDate.now().getYear(), 50.0, 0.0, "Règlement régulier");
            }
        }
        return lastGeneratedPayslip;
    }

    public void setLastGeneratedPayslip(Payslip payslip) {
        this.lastGeneratedPayslip = payslip;
    }

    public List<String> findAllCategories() {
        return Arrays.asList("Toutes", "Enseignant", "Administratif", "Direction", "Ouvrier", "Technique");
    }

    public List<String> findAllFonctions() {
        return Arrays.asList("Toutes", "Professeur de Mathématiques", "Professeur de Français", "Comptable",
                             "Caissière", "Secrétaire", "Directrice des Études", "Technicien de Maintenance");
    }

    /** Archive un membre du personnel (non destructif). */
    public boolean archive(int staffId) {
        return staffRepository.archive(staffId);
    }

    /** Retourne uniquement les membres non archivés. */
    public List<Staff> findAllActive() {
        return staffRepository.findAllActive();
    }

    /** Retourne les départements distincts (basé sur la catégorie). */
    public List<String> findAllDepartments() {
        List<String> depts = new java.util.ArrayList<>();
        depts.add("Tous");
        depts.addAll(staffRepository.findAllDepartments());
        return depts;
    }
}

package com.jenos.gestionecole.service;

import com.jenos.gestionecole.model.*;
import com.jenos.gestionecole.repository.SalaryRepository;
import com.jenos.gestionecole.repository.StaffRepository;
import com.jenos.gestionecole.util.RoleGuard;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * StaffPayrollService — Service central pour la gestion du personnel, des rémunérations,
 * des avances sur salaire, des paiements échelonnés et de la documentation (PDF).
 *
 * RÈGLES DU CAHIER DES CHARGES DU BLOC 2 :
 * 1. Formule officielle :
 *    Salaire net = Salaire de base + Primes fixes + Primes variables - Retenues - Avances déduites
 * 2. Arrondi à l'unité monétaire.
 * 3. Cloisonnement strict USD et CDF sans aucune conversion automatique.
 * 4. Aucune proratisation automatique (ajustement manuel par personne autorisée).
 * 5. Immuabilité des rémunérations validées (conservent leurs montants de calcul historiques).
 * 6. Suivi individuel des avances avec protection anti-double déduction.
 * 7. Versements complets et partiels avec recalcul dynamique du solde restant.
 * 8. Traçabilité complète des opérations dans AuditService.
 */
public class StaffPayrollService {

    private static final Logger logger = LoggerFactory.getLogger(StaffPayrollService.class);

    private static StaffPayrollService instance;

    private final StaffRepository staffRepository;
    private final SalaryRepository salaryRepository;

    public record PayrollReport(
        String period,
        List<SalaryRecord> records,
        double totalBaseUSD,
        double totalNetUSD,
        double totalPaidUSD,
        double totalBalanceUSD,
        double totalBaseCDF,
        double totalNetCDF,
        double totalPaidCDF,
        double totalBalanceCDF
    ) {}

    private StaffPayrollService() {
        this.staffRepository = new StaffRepository();
        this.salaryRepository = new SalaryRepository();
    }

    public static synchronized StaffPayrollService getInstance() {
        if (instance == null) {
            instance = new StaffPayrollService();
        }
        return instance;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. GESTION DES DOSSIERS EMPLOYÉS
    // ─────────────────────────────────────────────────────────────────────────

    public List<Staff> findAllStaff() {
        return staffRepository.findAll();
    }

    public List<Staff> findAllActiveStaff() {
        return staffRepository.findAllActive();
    }

    public Staff findStaffById(int id) {
        return staffRepository.findById(id);
    }

    public Staff findStaffByMatricule(String matricule) {
        return staffRepository.findByMatricule(matricule);
    }

    public void saveStaff(Staff staff, String userPerforming) {
        if (staff == null) throw new IllegalArgumentException("Le membre du personnel ne peut pas être null.");
        if (staff.getNom() == null || staff.getNom().isBlank()) throw new IllegalArgumentException("Le nom est obligatoire.");
        if (staff.getPostnom() == null || staff.getPostnom().isBlank()) throw new IllegalArgumentException("Le postnom est obligatoire.");
        if (staff.getSalaireBase() < 0) throw new IllegalArgumentException("Le salaire de base ne peut pas être négatif.");

        String cur = staff.getDeviseSalaire();
        if (cur == null || (!cur.equalsIgnoreCase("USD") && !cur.equalsIgnoreCase("CDF"))) {
            throw new IllegalArgumentException("La devise du salaire doit obligatoirement être USD ou CDF.");
        }
        staff.setDeviseSalaire(cur.toUpperCase().trim());
        staff.setSalaireBase(Math.round(staff.getSalaireBase())); // Arrondi à l'unité

        boolean isNew = (staff.getId() == 0);
        staffRepository.save(staff);

        String action = isNew ? "CRÉATION_PERSONNEL" : "MODIFICATION_PERSONNEL";
        AuditService.getInstance().logAction(
                userPerforming != null ? userPerforming : "système",
                action,
                "Staff",
                null,
                "Fiche employé " + staff.getMatricule() + " (" + staff.getFullName() + ")"
        );
    }

    public boolean archiveStaff(int staffId, String userPerforming) {
        Staff s = staffRepository.findById(staffId);
        if (s == null) return false;
        boolean ok = staffRepository.archive(staffId);
        if (ok) {
            AuditService.getInstance().logAction(
                    userPerforming != null ? userPerforming : "système",
                    "ARCHIVAGE_PERSONNEL",
                    "Staff",
                    null,
                    "Archivage de l'employé " + s.getMatricule() + " (" + s.getFullName() + ") - Historique préservé"
            );
        }
        return ok;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. GESTION DES AVANCES SUR SALAIRE
    // ─────────────────────────────────────────────────────────────────────────

    public SalaryAdvance grantAdvance(int staffId, double amount, String currency, String reason, String approvedBy) {
        Staff staff = staffRepository.findById(staffId);
        if (staff == null) {
            throw new IllegalArgumentException("Membre du personnel introuvable.");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Le montant de l'avance doit être strictement positif.");
        }
        if (currency == null || (!currency.equalsIgnoreCase("USD") && !currency.equalsIgnoreCase("CDF"))) {
            throw new IllegalArgumentException("La devise de l'avance doit être USD ou CDF.");
        }

        String cur = currency.toUpperCase().trim();
        // Vérification de cohérence de devise avec le salaire de l'employé
        if (!cur.equalsIgnoreCase(staff.getDeviseSalaire())) {
            throw new IllegalArgumentException(String.format(
                "La monnaie de l'avance (%s) doit correspondre à la monnaie de rémunération de l'employé (%s).",
                cur, staff.getDeviseSalaire()
            ));
        }

        // RÈGLE MÉTIER STRICTE : L'avance ne peut pas dépasser 50% du salaire de base
        double maxAllowedAdvance = staff.getSalaireBase() * 0.50;
        if (amount > maxAllowedAdvance) {
            throw new IllegalArgumentException(String.format(
                "Le montant de l'avance (%.2f %s) dépasse le plafond autorisé de 50%% du salaire de base (%.2f %s).",
                amount, cur, maxAllowedAdvance, cur
            ));
        }

        SalaryAdvance advance = new SalaryAdvance(
                0,
                staff.getId(),
                staff.getMatricule(),
                staff.getFullName(),
                Math.round(amount),
                cur,
                LocalDate.now(),
                reason,
                approvedBy != null ? approvedBy : "Direction"
        );

        salaryRepository.saveAdvance(advance);

        AuditService.getInstance().logAction(
                approvedBy != null ? approvedBy : "système",
                "OCTROI_AVANCE_SALAIRE",
                "SalaryAdvance",
                null,
                String.format("Avance de %.0f %s accordée à %s (%s)", advance.getInitialAmount(), cur, staff.getFullName(), staff.getMatricule())
        );

        return advance;
    }

    public List<SalaryAdvance> getActiveAdvancesForStaff(int staffId, String currency) {
        return salaryRepository.findActiveAdvancesByStaff(staffId, currency);
    }

    public double getTotalActiveAdvances(int staffId, String currency) {
        return getActiveAdvancesForStaff(staffId, currency).stream()
                .mapToDouble(SalaryAdvance::getRemainingBalance)
                .sum();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. CYCLE DE PAIE : PRÉPARATION & CALCUL (Étapes 1 & 2)
    // ─────────────────────────────────────────────────────────────────────────

    public SalaryRecord prepareSalary(int staffId, String period, int month, int year,
                                     SalaryRecord.Frequency frequency, double fixedAllowances,
                                     double variableAllowances, double deductions, double advancesToDeduct) {
        Staff staff = staffRepository.findById(staffId);
        if (staff == null) {
            throw new IllegalArgumentException("Employé introuvable pour la préparation de la paie.");
        }

        // Vérifier les avances disponibles
        double maxAdvancesAvailable = getTotalActiveAdvances(staffId, staff.getDeviseSalaire());
        double actualAdvancesToDeduct = Math.min(Math.round(advancesToDeduct), maxAdvancesAvailable);

        int seq = salaryRepository.countRecords() + 1;
        String recordNum = String.format("SAL-%d-%02d-%04d", year, month, seq);

        SalaryRecord record = new SalaryRecord(
                0,
                recordNum,
                staff,
                period != null ? period : (month + "/" + year),
                month,
                year,
                frequency != null ? frequency : SalaryRecord.Frequency.MENSUELLE,
                staff.getSalaireBase(),
                fixedAllowances,
                variableAllowances,
                deductions,
                actualAdvancesToDeduct
        );

        salaryRepository.saveRecord(record);
        logger.info("Rémunération préparée pour {} : Net = {} {}", staff.getFullName(), record.getNetSalary(), record.getCurrency());
        return record;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. CYCLE DE PAIE : VALIDATION (Étape 3)
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized SalaryRecord validateSalary(int salaryRecordId, String validatorUsername) {
        SalaryRecord record = salaryRepository.findRecordById(salaryRecordId)
                .orElseThrow(() -> new IllegalArgumentException("Rémunération introuvable."));

        if (record.getValidationStatus() != SalaryRecord.ValidationStatus.PREPAREE) {
            throw new IllegalStateException("Seule une rémunération en état 'Préparée' peut être validée.");
        }

        // Déduction formelle des avances actives
        double remainingToDeduct = record.getAdvancesDeducted();
        if (remainingToDeduct > 0) {
            List<SalaryAdvance> activeAdvances = salaryRepository.findActiveAdvancesByStaff(record.getStaffId(), record.getCurrency());
            for (SalaryAdvance adv : activeAdvances) {
                if (remainingToDeduct <= 0) break;
                double deducted = adv.applyDeduction(
                        remainingToDeduct,
                        record.getRecordNumber(),
                        "Déduction sur salaire période " + record.getPeriod()
                );
                salaryRepository.saveAdvance(adv);
                remainingToDeduct -= deducted;
            }
        }

        record.setValidationStatus(SalaryRecord.ValidationStatus.VALIDEE);
        record.setValidatedBy(validatorUsername != null ? validatorUsername : "Direction");
        record.setValidatedAt(LocalDateTime.now());
        record.recalculateNet(); // Fige et recalcule le solde initial

        salaryRepository.saveRecord(record);

        AuditService.getInstance().logAction(
                validatorUsername != null ? validatorUsername : "Direction",
                "VALIDATION_RÉMUNÉRATION",
                "SalaryRecord",
                null,
                String.format("Rémunération %s validée : Net %.0f %s pour %s",
                        record.getRecordNumber(), record.getNetSalary(), record.getCurrency(), record.getStaffFullName())
        );

        return record;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. CYCLE DE PAIE : PAIEMENT COMPLET OU ÉCHELONNÉ (Étape 4)
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized SalaryPayment recordPayment(int salaryRecordId, double amount, String currency,
                                                   String paymentMethod, String reference,
                                                   String recordedByUsername, String remarks) {
        SalaryRecord record = salaryRepository.findRecordById(salaryRecordId)
                .orElseThrow(() -> new IllegalArgumentException("Rémunération introuvable."));

        if (record.getValidationStatus() != SalaryRecord.ValidationStatus.VALIDEE &&
            record.getValidationStatus() != SalaryRecord.ValidationStatus.CLOTUREE) {
            throw new IllegalStateException("Paiement impossible : la rémunération doit être validée préalablement.");
        }

        if (currency == null || !currency.equalsIgnoreCase(record.getCurrency())) {
            throw new IllegalArgumentException(String.format(
                    "Devise non conforme : la rémunération est en %s, versement reçu en %s (conversion interdite).",
                    record.getCurrency(), currency
            ));
        }

        double paymentAmount = Math.round(amount);
        if (paymentAmount <= 0) {
            throw new IllegalArgumentException("Le montant du versement doit être supérieur à zéro.");
        }

        if (paymentAmount > record.getRemainingBalance()) {
            throw new IllegalArgumentException(String.format(
                    "Le versement (%.0f %s) excède le solde restant dû (%.0f %s).",
                    paymentAmount, record.getCurrency(), record.getRemainingBalance(), record.getCurrency()
            ));
        }

        SalaryPayment payment = new SalaryPayment(
                0,
                record.getId(),
                record.getStaffId(),
                record.getStaffMatricule(),
                record.getStaffFullName(),
                record.getPeriod(),
                paymentAmount,
                record.getCurrency(),
                LocalDate.now(),
                paymentMethod != null ? paymentMethod : "ESPECES",
                reference,
                recordedByUsername != null ? recordedByUsername : "Caisse",
                remarks
        );

        record.recordPayment(payment);
        salaryRepository.savePayment(payment);
        salaryRepository.saveRecord(record);

        AuditService.getInstance().logAction(
                recordedByUsername != null ? recordedByUsername : "Caisse",
                "PAIEMENT_SALAIRE",
                "SalaryPayment",
                null,
                String.format("Versement de %.0f %s sur %s (%s). Solde restant : %.0f %s",
                        paymentAmount, record.getCurrency(), record.getRecordNumber(), record.getStaffFullName(),
                        record.getRemainingBalance(), record.getCurrency())
        );

        return payment;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. DOCUMENTATION : BULLETINS & ÉTATS RÉCAPITULATIFS (Étape 5)
    // ─────────────────────────────────────────────────────────────────────────

    public Payslip generatePayslip(int salaryRecordId) {
        SalaryRecord record = salaryRepository.findRecordById(salaryRecordId)
                .orElseThrow(() -> new IllegalArgumentException("Rémunération introuvable."));

        Staff staff = staffRepository.findById(record.getStaffId());

        Payslip p = new Payslip();
        p.setId(record.getId());
        p.setPayslipNumber(record.getRecordNumber());
        p.setStaff(staff);
        p.setMonth(record.getMonth());
        p.setYear(record.getYear());
        p.setBaseSalary(record.getBaseSalary());
        p.setAllowances(record.getFixedAllowances() + record.getVariableAllowances());
        p.setAdvancesDeducted(record.getAdvancesDeducted());
        p.setOtherDeductions(record.getDeductions());
        p.setNetPayable(record.getNetSalary());
        p.setCurrency(record.getCurrency());
        p.setPaymentDate(LocalDate.now());
        p.setGeneratedAt(LocalDateTime.now());
        p.setCashierUsername(record.getValidatedBy() != null ? record.getValidatedBy() : "Direction");
        p.setRemarks(String.format("Payé : %.0f %s | Solde restant : %.0f %s | Statut : %s",
                record.getTotalPaid(), record.getCurrency(),
                record.getRemainingBalance(), record.getCurrency(),
                record.getPaymentStatus().getLabel()));

        return p;
    }

    public File exportPayslipPdf(int salaryRecordId, File destinationFile) throws IOException {
        SalaryRecord record = salaryRepository.findRecordById(salaryRecordId)
                .orElseThrow(() -> new IllegalArgumentException("Rémunération introuvable."));

        File target = destinationFile != null ? destinationFile : new File("target/bulletin_" + record.getRecordNumber() + ".pdf");
        if (target.getParentFile() != null && !target.getParentFile().exists()) {
            target.getParentFile().mkdirs();
        }

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

                float y = 780;

                SchoolSettings school = SchoolSettingsService.getInstance().getSettings();
                String schoolName = (school != null && school.getSchoolName() != null)
                        ? school.getSchoolName().toUpperCase() : "COMPLEXE SCOLAIRE JENOS";
                String country = (school != null && school.getCountry() != null)
                        ? school.getCountry() : "République Démocratique du Congo";

                // En-tête officiel de l'école
                cs.beginText();
                cs.setFont(bold, 16);
                cs.newLineAtOffset(50, y);
                cs.showText(schoolName + " — GESTION DU PERSONNEL");
                cs.endText();

                y -= 20;
                cs.beginText();
                cs.setFont(regular, 10);
                cs.newLineAtOffset(50, y);
                cs.showText(country + " | Direction des Ressources Humaines & Finances");
                cs.endText();

                y -= 25;
                cs.setLineWidth(1.0f);
                cs.moveTo(50, y);
                cs.lineTo(550, y);
                cs.stroke();

                y -= 30;
                cs.beginText();
                cs.setFont(bold, 13);
                cs.newLineAtOffset(50, y);
                cs.showText("BULLETIN DE PAIE OFFICIEL — RÉF : " + record.getRecordNumber());
                cs.endText();

                y -= 20;
                cs.beginText();
                cs.setFont(regular, 10);
                cs.newLineAtOffset(50, y);
                cs.showText("Période : " + record.getPeriod() + " | Fréquence : " + record.getFrequency().getLabel()
                        + " | Date d'émission : " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                cs.endText();

                y -= 30;
                cs.beginText();
                cs.setFont(bold, 11);
                cs.newLineAtOffset(50, y);
                cs.showText("1. IDENTIFICATION DU SALARIÉ");
                cs.endText();

                y -= 18;
                cs.beginText();
                cs.setFont(regular, 10);
                cs.newLineAtOffset(60, y);
                cs.showText("Nom & Postnom : " + record.getStaffFullName());
                cs.endText();

                y -= 15;
                cs.beginText();
                cs.setFont(regular, 10);
                cs.newLineAtOffset(60, y);
                cs.showText("Matricule : " + record.getStaffMatricule() + " | Département : " + record.getStaffDepartment()
                        + " | Fonction : " + record.getStaffFunction());
                cs.endText();

                y -= 30;
                cs.beginText();
                cs.setFont(bold, 11);
                cs.newLineAtOffset(50, y);
                cs.showText("2. DÉCOMPTE SALARIAL CONFORME (DEVISE : " + record.getCurrency() + ")");
                cs.endText();

                y -= 18;
                printLine(cs, regular, 60, y, "Salaire de base contractuel :", String.format("%.0f %s", record.getBaseSalary(), record.getCurrency()));
                y -= 15;
                printLine(cs, regular, 60, y, "(+) Primes fixes :", String.format("%.0f %s", record.getFixedAllowances(), record.getCurrency()));
                y -= 15;
                printLine(cs, regular, 60, y, "(+) Primes variables & exceptionnelles :", String.format("%.0f %s", record.getVariableAllowances(), record.getCurrency()));
                y -= 15;
                printLine(cs, regular, 60, y, "(-) Retenues diverses :", String.format("- %.0f %s", record.getDeductions(), record.getCurrency()));
                y -= 15;
                printLine(cs, regular, 60, y, "(-) Avances sur salaire déduites :", String.format("- %.0f %s", record.getAdvancesDeducted(), record.getCurrency()));

                y -= 20;
                cs.moveTo(50, y);
                cs.lineTo(550, y);
                cs.stroke();

                y -= 20;
                printLine(cs, bold, 50, y, "SALAIRE NET VALIDÉ :", String.format("%.0f %s", record.getNetSalary(), record.getCurrency()));
                y -= 16;
                printLine(cs, regular, 50, y, "Montant total versé / payé :", String.format("%.0f %s", record.getTotalPaid(), record.getCurrency()));
                y -= 16;
                printLine(cs, bold, 50, y, "SOLDE RESTANT DÛ :", String.format("%.0f %s", record.getRemainingBalance(), record.getCurrency()));
                y -= 16;
                printLine(cs, regular, 50, y, "Statut de liquidation :", record.getPaymentStatus().getLabel());

                y -= 40;
                cs.beginText();
                cs.setFont(regular, 9);
                cs.newLineAtOffset(50, y);
                cs.showText("Visa & Cachet de la Direction                                                   Signature du Salarié (Pour Acquit)");
                cs.endText();
            }

            doc.save(target);
        }
        return target;
    }

    private void printLine(PDPageContentStream cs, PDType1Font font, float x, float y, String label, String value) throws IOException {
        cs.beginText();
        cs.setFont(font, 10);
        cs.newLineAtOffset(x, y);
        cs.showText(label);
        cs.endText();

        cs.beginText();
        cs.setFont(font, 10);
        cs.newLineAtOffset(420, y);
        cs.showText(value);
        cs.endText();
    }

    public PayrollReport getPayrollReport(String period, String department, SalaryRecord.PaymentStatus statusFilter) {
        List<SalaryRecord> list = salaryRepository.findAllRecords().stream()
                .filter(r -> period == null || period.isBlank() || period.equalsIgnoreCase(r.getPeriod()))
                .filter(r -> department == null || department.isBlank() || "Tous".equalsIgnoreCase(department)
                        || department.equalsIgnoreCase(r.getStaffDepartment()))
                .filter(r -> statusFilter == null || statusFilter == r.getPaymentStatus())
                .collect(Collectors.toList());

        double baseUSD = 0, netUSD = 0, paidUSD = 0, balUSD = 0;
        double baseCDF = 0, netCDF = 0, paidCDF = 0, balCDF = 0;

        for (SalaryRecord r : list) {
            if ("USD".equalsIgnoreCase(r.getCurrency())) {
                baseUSD += r.getBaseSalary();
                netUSD += r.getNetSalary();
                paidUSD += r.getTotalPaid();
                balUSD += r.getRemainingBalance();
            } else if ("CDF".equalsIgnoreCase(r.getCurrency())) {
                baseCDF += r.getBaseSalary();
                netCDF += r.getNetSalary();
                paidCDF += r.getTotalPaid();
                balCDF += r.getRemainingBalance();
            }
        }

        return new PayrollReport(period, list, baseUSD, netUSD, paidUSD, balUSD, baseCDF, netCDF, paidCDF, balCDF);
    }

    public SalaryRepository getSalaryRepository() {
        return salaryRepository;
    }
}

package com.jenos.gestionecole.service;

import com.jenos.gestionecole.model.*;
import com.jenos.gestionecole.util.RoleGuard;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SchoolFeeService — Service central de gestion des frais scolaires.
 *
 * Responsabilités :
 * 1. Affectation automatique des frais applicables lors de la validation d'inscription.
 * 2. Génération des échéances selon la fréquence configurée (ANNUEL / TRIMESTRIEL / MENSUEL).
 * 3. Enregistrement des paiements avec contrôle anti-trop-perçu.
 * 4. Gestion des réductions et exemptions avec autorisation RoleGuard.
 * 5. Calcul des situations financières réelles par élève.
 * 6. Production de reçus et états financiers en PDF.
 * 7. Conservation de l'historique (immutabilité des affectations).
 *
 * RÈGLE ABSOLUE : Les montants USD et CDF sont traités séparément,
 * JAMAIS additionnés ni convertis implicitement.
 */
public class SchoolFeeService {

    private static final Logger logger = LoggerFactory.getLogger(SchoolFeeService.class);

    private static SchoolFeeService instance;

    private final List<StudentFeeAssignment> assignments = new ArrayList<>();
    private final FeeService feeService;
    private int nextAssignmentId = 1;
    private int nextInstallmentId = 1;

    // ── Résultat structuré : situation financière d'un élève ─────

    /** Situation financière complète d'un élève pour une année scolaire. */
    public static class StudentFeeSituation {
        public int studentId;
        public String studentName;
        public String studentMatricule;
        public String academicYear;
        public double totalDueUSD = 0.0;
        public double totalPaidUSD = 0.0;
        public double balanceUSD = 0.0;
        public double totalDueCDF = 0.0;
        public double totalPaidCDF = 0.0;
        public double balanceCDF = 0.0;
        public int totalAssignments = 0;
        public int overdueCount = 0;
        public List<StudentFeeAssignment> assignments = new ArrayList<>();
    }

    // ── Singleton ───────────────────────────────────────────────

    private SchoolFeeService() {
        this.feeService = FeeService.getInstance();
    }

    public static SchoolFeeService getInstance() {
        if (instance == null) {
            instance = new SchoolFeeService();
        }
        return instance;
    }

    /** Réinitialisation pour les tests unitaires. */
    public static void resetInstance() {
        instance = null;
    }

    // =====================================================================
    //  1. AFFECTATION DES FRAIS
    // =====================================================================

    /**
     * Affecte automatiquement les frais applicables à un élève pour une année scolaire.
     * Les frais sont résolus selon la hiérarchie scolaire de l'élève
     * (GLOBAL ∪ SECTION ∪ PROMOTION ∪ OPTION ∪ CLASSE).
     *
     * Chaque affectation capture un snapshot immutable du tarif catalogue.
     *
     * @param student      l'élève inscrit
     * @param academicYear année scolaire (ex: "2024-2025")
     * @param assignedBy   utilisateur effectuant l'affectation
     * @return liste des affectations créées
     */
    public List<StudentFeeAssignment> assignFeesToStudent(Student student, String academicYear,
                                                          String assignedBy) {
        if (student == null) {
            throw new IllegalArgumentException("L'élève ne peut pas être null.");
        }
        if (academicYear == null || academicYear.isBlank()) {
            throw new IllegalArgumentException("L'année scolaire est obligatoire.");
        }

        // Anti-doublon : vérifier si déjà affecté pour cette année
        boolean alreadyAssigned = assignments.stream()
                .anyMatch(a -> a.getStudentId() == student.getId()
                        && academicYear.equals(a.getAcademicYear()));
        if (alreadyAssigned) {
            throw new IllegalStateException(
                    "Les frais ont déjà été affectés à " + student.getFullName()
                            + " pour l'année " + academicYear + ".");
        }

        // Résolution hiérarchique des frais applicables
        List<Fee> applicableFees = feeService.getFeesForStudent(student);
        if (applicableFees.isEmpty()) {
            logger.warn("Aucun frais applicable pour {} [{}]",
                    student.getFullName(), student.getMatricule());
            return Collections.emptyList();
        }

        List<StudentFeeAssignment> created = new ArrayList<>();

        for (Fee fee : applicableFees) {
            StudentFeeAssignment assignment = new StudentFeeAssignment();
            assignment.setId(nextAssignmentId++);
            assignment.setStudentId(student.getId());
            assignment.setStudentName(student.getFullName());
            assignment.setStudentMatricule(student.getMatricule());
            assignment.setFeeId(fee.getId());
            assignment.setFeeName(fee.getName());
            assignment.setFeeCategory(fee.getCategory());
            assignment.setClasseId(student.getClasse() != null ? student.getClasse().getId() : 0);
            assignment.setClassName(student.getClassName());
            assignment.setAcademicYear(academicYear);
            assignment.setOriginalAmount(fee.getAmount());  // snapshot immutable
            assignment.setCurrency(fee.getCurrency());
            assignment.setAssignedBy(assignedBy);

            // Génération des échéances selon la fréquence
            List<FeeInstallment> installments = generateInstallments(
                    assignment, fee.getFrequency(), fee.getDateEcheance(), academicYear);
            assignment.setInstallments(installments);

            assignments.add(assignment);
            created.add(assignment);

            logger.info("Frais '{}' ({} {}) affecté à {} [{}] — {} échéance(s)",
                    fee.getName(), String.format("%.2f", fee.getAmount()), fee.getCurrency(),
                    student.getFullName(), student.getMatricule(), installments.size());
        }

        AuditService.getInstance().logAction(assignedBy, "AFFECTATION_FRAIS",
                "Élève " + student.getFullName(),
                null,
                created.size() + " frais affectés pour " + academicYear);

        DashboardService.getInstance().notifyDataChanged();

        return created;
    }

    // =====================================================================
    //  2. GÉNÉRATION DES ÉCHÉANCES
    // =====================================================================

    /**
     * Génère les échéances pour une affectation de frais selon la fréquence configurée.
     * ANNUEL → 1 échéance | TRIMESTRIEL → 3 échéances | MENSUEL → 10 échéances
     */
    private List<FeeInstallment> generateInstallments(StudentFeeAssignment assignment,
                                                      Fee.PaymentFrequency frequency,
                                                      LocalDate feeDueDate,
                                                      String academicYear) {
        if (frequency == null) frequency = Fee.PaymentFrequency.ANNUEL;

        int[] years = parseAcademicYear(academicYear);
        int startYear = years[0];
        int endYear = years[1];
        double totalAmount = assignment.getAdjustedAmount();

        List<FeeInstallment> installments = new ArrayList<>();

        switch (frequency) {
            case ANNUEL -> {
                LocalDate due = feeDueDate != null ? feeDueDate : LocalDate.of(endYear, 6, 30);
                installments.add(new FeeInstallment(
                        nextInstallmentId++, assignment.getId(), 1,
                        "Paiement annuel", due, totalAmount, assignment.getCurrency()));
            }
            case TRIMESTRIEL -> {
                double each = Math.floor(totalAmount / 3.0 * 100.0) / 100.0;
                double last = Math.round((totalAmount - each * 2) * 100.0) / 100.0;

                installments.add(new FeeInstallment(nextInstallmentId++, assignment.getId(), 1,
                        "1er Trimestre", LocalDate.of(startYear, 10, 31),
                        each, assignment.getCurrency()));
                installments.add(new FeeInstallment(nextInstallmentId++, assignment.getId(), 2,
                        "2ème Trimestre", LocalDate.of(endYear, 1, 31),
                        each, assignment.getCurrency()));
                installments.add(new FeeInstallment(nextInstallmentId++, assignment.getId(), 3,
                        "3ème Trimestre", LocalDate.of(endYear, 4, 30),
                        last, assignment.getCurrency()));
            }
            case MENSUEL -> {
                double each = Math.floor(totalAmount / 10.0 * 100.0) / 100.0;
                double sumFirst9 = each * 9;
                double lastMonth = Math.round((totalAmount - sumFirst9) * 100.0) / 100.0;

                int[][] months = {
                        {startYear, 9}, {startYear, 10}, {startYear, 11}, {startYear, 12},
                        {endYear, 1}, {endYear, 2}, {endYear, 3}, {endYear, 4},
                        {endYear, 5}, {endYear, 6}
                };
                String[] labels = {
                        "Septembre", "Octobre", "Novembre", "Décembre",
                        "Janvier", "Février", "Mars", "Avril", "Mai", "Juin"
                };

                for (int i = 0; i < 10; i++) {
                    double amt = (i < 9) ? each : lastMonth;
                    YearMonth ym = YearMonth.of(months[i][0], months[i][1]);
                    LocalDate due = ym.atEndOfMonth();
                    installments.add(new FeeInstallment(
                            nextInstallmentId++, assignment.getId(), i + 1,
                            labels[i] + " " + months[i][0], due,
                            amt, assignment.getCurrency()));
                }
            }
        }

        return installments;
    }

    /** Parse "2024-2025" → [2024, 2025]. */
    private int[] parseAcademicYear(String academicYear) {
        try {
            String[] parts = academicYear.split("-");
            if (parts.length == 2) {
                return new int[]{Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim())};
            }
        } catch (NumberFormatException ignored) {}
        int year = LocalDate.now().getYear();
        return new int[]{year, year + 1};
    }

    // =====================================================================
    //  3. ENREGISTREMENT DES PAIEMENTS
    // =====================================================================

    /**
     * Enregistre un paiement sur une échéance spécifique.
     *
     * Règles :
     * - Le montant ne peut pas dépasser le solde restant de l'échéance ni de l'affectation.
     * - La devise doit correspondre à celle du frais.
     * - Le paiement est traité via PaymentService (impact caisse, reçu, audit).
     *
     * @return le reçu officiel généré
     */
    public Receipt recordFeePayment(int assignmentId, int installmentId, double amount,
                                    String currency, Payment.PaymentMethod method,
                                    String reference, String cashierUsername,
                                    Student student) {
        // 1. Trouver l'affectation
        StudentFeeAssignment assignment = findAssignmentById(assignmentId);
        if (assignment == null) {
            throw new IllegalArgumentException(
                    "Affectation de frais introuvable (ID: " + assignmentId + ").");
        }

        // 2. Vérifier la devise
        if (!assignment.getCurrency().equalsIgnoreCase(currency)) {
            throw new IllegalArgumentException(
                    "Devise incompatible : le frais est en " + assignment.getCurrency()
                            + ", paiement en " + currency + ".");
        }

        // 3. Trouver l'échéance
        FeeInstallment installment = assignment.getInstallments().stream()
                .filter(i -> i.getId() == installmentId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Échéance introuvable (ID: " + installmentId + ")."));

        // 4. Valider le montant
        if (amount <= 0) {
            throw new IllegalArgumentException("Le montant du paiement doit être supérieur à zéro.");
        }

        double maxPayable = Math.min(installment.getBalance(), assignment.getBalance());
        if (amount > maxPayable + 0.009) { // tolérance arrondi
            throw new IllegalArgumentException(
                    "Le montant (" + String.format("%.2f", amount) + " " + currency
                            + ") dépasse le solde restant ("
                            + String.format("%.2f", maxPayable) + " " + currency + ").");
        }

        // 5. Appliquer le paiement sur l'échéance
        double previouslyPaid = assignment.getTotalPaid();
        installment.addPayment(amount);
        assignment.recalculateTotalPaid();

        // 6. Créer le Payment et traiter via PaymentService (caisse + reçu + audit)
        Payment payment = new Payment();
        payment.setPaymentType(Payment.PaymentType.ELEVE);
        payment.setStudent(student);
        payment.setFee(feeService.findById(assignment.getFeeId()));
        payment.setAmount(amount);
        payment.setCurrency(currency);
        payment.setPaymentMethod(method != null ? method : Payment.PaymentMethod.ESPECES);
        payment.setReference(reference);
        payment.setCashierUsername(cashierUsername);

        Receipt receipt = PaymentService.getInstance().processPayment(payment);

        // 7. Corriger les données du reçu avec les montants réels de l'affectation
        receipt.setFeeTotalDue(assignment.getAdjustedAmount());
        receipt.setFeePreviouslyPaid(previouslyPaid);
        receipt.setBalanceRemaining(assignment.getBalance());

        logger.info("Paiement {} {} — {} — Frais '{}', Échéance '{}' — Solde: {} {}",
                String.format("%.2f", amount), currency,
                assignment.getStudentName(), assignment.getFeeName(),
                installment.getLabel(),
                String.format("%.2f", assignment.getBalance()), currency);

        return receipt;
    }

    // =====================================================================
    //  4. RÉDUCTIONS ET EXEMPTIONS
    // =====================================================================

    /**
     * Accorde une réduction sur un frais affecté.
     * Nécessite l'autorisation RoleGuard (DIRECTEUR ou PROMOTEUR).
     * Conserve le montant initial et l'historique de la décision.
     */
    public void grantReduction(int assignmentId, double amount, String motif,
                               String authorUsername) {
        if (!RoleGuard.canGrantFeeReduction()) {
            throw new SecurityException(
                    "Vous n'avez pas l'autorisation d'accorder une réduction de frais.");
        }

        StudentFeeAssignment assignment = findAssignmentById(assignmentId);
        if (assignment == null) {
            throw new IllegalArgumentException(
                    "Affectation de frais introuvable (ID: " + assignmentId + ").");
        }

        if (motif == null || motif.isBlank()) {
            throw new IllegalArgumentException("Le motif de la réduction est obligatoire.");
        }

        assignment.applyReduction(amount, motif, authorUsername);

        AuditService.getInstance().logAction(authorUsername, "REDUCTION_FRAIS",
                assignment.getFeeName() + " — " + assignment.getStudentName(),
                String.format("%.2f %s", assignment.getOriginalAmount(), assignment.getCurrency()),
                String.format("Réduction: %.2f %s — Motif: %s",
                        amount, assignment.getCurrency(), motif));

        logger.info("Réduction {} {} par {} — {} — '{}' — Motif: {}",
                String.format("%.2f", amount), assignment.getCurrency(),
                authorUsername, assignment.getStudentName(),
                assignment.getFeeName(), motif);

        DashboardService.getInstance().notifyDataChanged();
    }

    /**
     * Accorde une exemption totale sur un frais affecté.
     * Nécessite l'autorisation RoleGuard (DIRECTEUR ou PROMOTEUR).
     */
    public void grantExemption(int assignmentId, String motif, String authorUsername) {
        if (!RoleGuard.canGrantFeeReduction()) {
            throw new SecurityException(
                    "Vous n'avez pas l'autorisation d'accorder une exemption de frais.");
        }

        StudentFeeAssignment assignment = findAssignmentById(assignmentId);
        if (assignment == null) {
            throw new IllegalArgumentException(
                    "Affectation de frais introuvable (ID: " + assignmentId + ").");
        }

        if (motif == null || motif.isBlank()) {
            throw new IllegalArgumentException("Le motif de l'exemption est obligatoire.");
        }

        assignment.exempt(motif, authorUsername);

        AuditService.getInstance().logAction(authorUsername, "EXEMPTION_FRAIS",
                assignment.getFeeName() + " — " + assignment.getStudentName(),
                String.format("%.2f %s", assignment.getOriginalAmount(), assignment.getCurrency()),
                "EXEMPTÉ — Motif: " + motif);

        logger.info("Exemption totale par {} — {} — '{}' — Motif: {}",
                authorUsername, assignment.getStudentName(),
                assignment.getFeeName(), motif);

        DashboardService.getInstance().notifyDataChanged();
    }

    // =====================================================================
    //  5. REQUÊTES ET SITUATIONS FINANCIÈRES
    // =====================================================================

    /** Recherche une affectation par son identifiant. */
    public StudentFeeAssignment findAssignmentById(int id) {
        return assignments.stream()
                .filter(a -> a.getId() == id)
                .findFirst().orElse(null);
    }

    /** Retourne toutes les affectations d'un élève. */
    public List<StudentFeeAssignment> getAssignmentsByStudent(int studentId) {
        return assignments.stream()
                .filter(a -> a.getStudentId() == studentId)
                .collect(Collectors.toList());
    }

    /** Retourne les affectations d'un élève pour une année scolaire spécifique. */
    public List<StudentFeeAssignment> getAssignmentsByStudent(int studentId, String academicYear) {
        return assignments.stream()
                .filter(a -> a.getStudentId() == studentId
                        && academicYear.equals(a.getAcademicYear()))
                .collect(Collectors.toList());
    }

    /** Retourne les échéances en retard d'un élève. */
    public List<FeeInstallment> getOverdueInstallments(int studentId) {
        return assignments.stream()
                .filter(a -> a.getStudentId() == studentId
                        && a.getStatus() == StudentFeeAssignment.AssignmentStatus.ACTIF)
                .flatMap(a -> a.getInstallments().stream())
                .filter(FeeInstallment::isOverdue)
                .collect(Collectors.toList());
    }

    /**
     * Calcule la situation financière complète d'un élève pour une année scolaire.
     * Les totaux USD et CDF sont strictement séparés.
     */
    public StudentFeeSituation getStudentSituation(int studentId, String academicYear) {
        StudentFeeSituation sit = new StudentFeeSituation();
        sit.studentId = studentId;
        sit.academicYear = academicYear;

        List<StudentFeeAssignment> studentAssignments =
                getAssignmentsByStudent(studentId, academicYear);
        sit.assignments = studentAssignments;
        sit.totalAssignments = studentAssignments.size();

        for (StudentFeeAssignment a : studentAssignments) {
            if (a.getStatus() == StudentFeeAssignment.AssignmentStatus.EXEMPTE) continue;

            if ("USD".equalsIgnoreCase(a.getCurrency())) {
                sit.totalDueUSD += a.getAdjustedAmount();
                sit.totalPaidUSD += a.getTotalPaid();
            } else if ("CDF".equalsIgnoreCase(a.getCurrency())) {
                sit.totalDueCDF += a.getAdjustedAmount();
                sit.totalPaidCDF += a.getTotalPaid();
            }
        }

        sit.balanceUSD = Math.max(0, sit.totalDueUSD - sit.totalPaidUSD);
        sit.balanceCDF = Math.max(0, sit.totalDueCDF - sit.totalPaidCDF);

        sit.overdueCount = (int) studentAssignments.stream()
                .filter(a -> a.getStatus() == StudentFeeAssignment.AssignmentStatus.ACTIF)
                .flatMap(a -> a.getInstallments().stream())
                .filter(FeeInstallment::isOverdue)
                .count();

        if (!studentAssignments.isEmpty()) {
            sit.studentName = studentAssignments.get(0).getStudentName();
            sit.studentMatricule = studentAssignments.get(0).getStudentMatricule();
        }

        return sit;
    }

    /** Retourne toutes les affectations enregistrées. */
    public List<StudentFeeAssignment> getAllAssignments() {
        return new ArrayList<>(assignments);
    }

    // =====================================================================
    //  6. GÉNÉRATION PDF — ÉTAT DES FRAIS
    // =====================================================================

    /**
     * Génère un PDF d'état des frais scolaires pour un élève et une année scolaire.
     * Le document détaille chaque frais, ses échéances, paiements et réductions.
     * Les totaux USD et CDF sont strictement séparés.
     */
    public File generateStudentFeeStatementPdf(int studentId, String academicYear)
            throws IOException {

        StudentFeeSituation situation = getStudentSituation(studentId, academicYear);

        File outputDir = new File(System.getProperty("user.home"),
                "Documents/GestionScolaire/Etats");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        String filename = "Etat_Frais_" + studentId + "_"
                + academicYear.replace("-", "_") + ".pdf";
        File outputFile = new File(outputDir, filename);

        PDFont fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDFont fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                float y = 780;
                float margin = 50;

                // ── En-tête officiel de l'établissement ──
                SchoolSettings school = SchoolSettingsService.getInstance().getSettings();
                String schoolName = (school != null && school.getSchoolName() != null)
                        ? school.getSchoolName().toUpperCase() : "COMPLEXE SCOLAIRE JENOS";

                cs.setFont(fontBold, 15);
                cs.beginText();
                cs.newLineAtOffset(margin, y);
                cs.showText(schoolName);
                cs.endText();
                y -= 20;

                cs.setFont(fontBold, 13);
                cs.beginText();
                cs.newLineAtOffset(margin, y);
                cs.showText("ETAT DES FRAIS SCOLAIRES");
                cs.endText();
                y -= 25;

                cs.setFont(fontRegular, 11);
                String[][] info = {
                        {"Eleve", situation.studentName != null ? situation.studentName : "—"},
                        {"Matricule", situation.studentMatricule != null ? situation.studentMatricule : "—"},
                        {"Annee scolaire", academicYear},
                        {"Date", LocalDate.now().format(dateFmt)}
                };
                for (String[] line : info) {
                    cs.beginText();
                    cs.newLineAtOffset(margin, y);
                    cs.showText(line[0] + " : " + line[1]);
                    cs.endText();
                    y -= 16;
                }
                y -= 14;

                // ── Résumé financier ──
                cs.setFont(fontBold, 12);
                cs.beginText();
                cs.newLineAtOffset(margin, y);
                cs.showText("RESUME FINANCIER");
                cs.endText();
                y -= 20;

                cs.setFont(fontRegular, 10);
                if (situation.totalDueUSD > 0 || situation.totalPaidUSD > 0) {
                    cs.beginText();
                    cs.newLineAtOffset(margin, y);
                    cs.showText(String.format(
                            "USD — Du : %.2f | Paye : %.2f | Solde : %.2f",
                            situation.totalDueUSD, situation.totalPaidUSD, situation.balanceUSD));
                    cs.endText();
                    y -= 15;
                }
                if (situation.totalDueCDF > 0 || situation.totalPaidCDF > 0) {
                    cs.beginText();
                    cs.newLineAtOffset(margin, y);
                    cs.showText(String.format(
                            "CDF — Du : %.2f | Paye : %.2f | Solde : %.2f",
                            situation.totalDueCDF, situation.totalPaidCDF, situation.balanceCDF));
                    cs.endText();
                    y -= 15;
                }
                y -= 15;

                // ── Détail des frais ──
                cs.setFont(fontBold, 12);
                cs.beginText();
                cs.newLineAtOffset(margin, y);
                cs.showText("DETAIL DES FRAIS");
                cs.endText();
                y -= 20;

                for (StudentFeeAssignment a : situation.assignments) {
                    if (y < 80) break; // sécurité : ne pas écrire hors page

                    cs.setFont(fontBold, 10);
                    cs.beginText();
                    cs.newLineAtOffset(margin, y);
                    cs.showText(a.getFeeName() + " (" + a.getCurrency() + ")");
                    cs.endText();
                    y -= 15;

                    cs.setFont(fontRegular, 9);
                    cs.beginText();
                    cs.newLineAtOffset(margin + 15, y);
                    cs.showText(String.format(
                            "Initial : %.2f | Reduction : %.2f | Ajuste : %.2f | Paye : %.2f | Solde : %.2f",
                            a.getOriginalAmount(), a.getReductionAmount(),
                            a.getAdjustedAmount(), a.getTotalPaid(), a.getBalance()));
                    cs.endText();
                    y -= 13;

                    if (a.getReductionMotif() != null && !a.getReductionMotif().isEmpty()) {
                        cs.beginText();
                        cs.newLineAtOffset(margin + 15, y);
                        cs.showText("Motif reduction : " + a.getReductionMotif()
                                + " (par " + a.getReductionAuthor() + ")");
                        cs.endText();
                        y -= 13;
                    }

                    for (FeeInstallment inst : a.getInstallments()) {
                        if (y < 60) break;
                        cs.beginText();
                        cs.newLineAtOffset(margin + 30, y);
                        cs.showText(String.format(
                                "- %s — Du : %.2f | Paye : %.2f | %s | Echeance : %s",
                                inst.getLabel(), inst.getAmount(), inst.getAmountPaid(),
                                inst.getStatus().getLabel(),
                                inst.getDueDate() != null ? inst.getDueDate().format(dateFmt) : "—"));
                        cs.endText();
                        y -= 13;
                    }

                    y -= 10;
                }
            }

            document.save(outputFile);
        }

        logger.info("PDF état des frais généré : {}", outputFile.getAbsolutePath());
        return outputFile;
    }
}

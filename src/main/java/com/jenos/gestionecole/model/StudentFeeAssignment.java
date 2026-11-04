package com.jenos.gestionecole.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * StudentFeeAssignment — Frais scolaire affecté à un élève spécifique.
 *
 * Ce modèle capture un snapshot du tarif au moment de l'affectation (immutabilité).
 * Une modification ultérieure du tarif catalogue (Fee) n'altère PAS
 * les frais déjà affectés aux élèves.
 *
 * RÈGLE ABSOLUE : Les montants USD et CDF sont traités séparément,
 * JAMAIS additionnés ni convertis implicitement.
 */
public class StudentFeeAssignment {

    public enum AssignmentStatus {
        ACTIF("Actif"),
        EXEMPTE("Exempté");

        private final String label;

        AssignmentStatus(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private int id;

    // ── Rattachements ────────────────────────────────────────────
    private int studentId;
    private String studentName;
    private String studentMatricule;
    private int feeId;
    private String feeName;
    private Fee.FeeCategory feeCategory;
    private int classeId;
    private String className;
    private String academicYear;

    // ── Snapshot financier (immutable après affectation) ─────────
    private double originalAmount;
    private String currency; // "USD" ou "CDF"

    // ── Réduction / Exemption ───────────────────────────────────
    private double reductionAmount;
    private String reductionMotif;
    private String reductionAuthor;
    private LocalDate reductionDate;

    // ── Suivi des paiements ─────────────────────────────────────
    private double totalPaid;
    private AssignmentStatus status;

    // ── Échéances ───────────────────────────────────────────────
    private List<FeeInstallment> installments = new ArrayList<>();

    // ── Traçabilité ─────────────────────────────────────────────
    private LocalDateTime createdAt;
    private String assignedBy;

    public StudentFeeAssignment() {
        this.status = AssignmentStatus.ACTIF;
        this.reductionAmount = 0.0;
        this.totalPaid = 0.0;
        this.createdAt = LocalDateTime.now();
    }

    // ── Méthodes métier ──────────────────────────────────────────

    /** Montant total dû après réduction. */
    public double getAdjustedAmount() {
        return Math.max(0, originalAmount - reductionAmount);
    }

    /** Solde restant à payer. */
    public double getBalance() {
        return Math.max(0, getAdjustedAmount() - totalPaid);
    }

    /** L'élève a tout payé pour ce frais. */
    public boolean isFullyPaid() {
        return getBalance() <= 0.001;
    }

    /** Recalcule le totalPaid à partir des échéances. */
    public void recalculateTotalPaid() {
        this.totalPaid = installments.stream()
                .mapToDouble(FeeInstallment::getAmountPaid)
                .sum();
    }

    /**
     * Applique une réduction (ne peut pas dépasser le montant original).
     * Conserve le montant initial et l'historique de la réduction.
     */
    public void applyReduction(double amount, String motif, String author) {
        if (amount < 0) {
            throw new IllegalArgumentException("Le montant de la réduction ne peut pas être négatif.");
        }
        if (amount > originalAmount) {
            throw new IllegalArgumentException("La réduction ne peut pas dépasser le montant initial ("
                    + String.format("%.2f", originalAmount) + " " + currency + ").");
        }
        this.reductionAmount = amount;
        this.reductionMotif = motif;
        this.reductionAuthor = author;
        this.reductionDate = LocalDate.now();
    }

    /** Exempte entièrement l'élève de ce frais. */
    public void exempt(String motif, String author) {
        this.reductionAmount = this.originalAmount;
        this.reductionMotif = motif;
        this.reductionAuthor = author;
        this.reductionDate = LocalDate.now();
        this.status = AssignmentStatus.EXEMPTE;
    }

    // ── Getters & Setters ────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentMatricule() { return studentMatricule; }
    public void setStudentMatricule(String studentMatricule) { this.studentMatricule = studentMatricule; }

    public int getFeeId() { return feeId; }
    public void setFeeId(int feeId) { this.feeId = feeId; }

    public String getFeeName() { return feeName; }
    public void setFeeName(String feeName) { this.feeName = feeName; }

    public Fee.FeeCategory getFeeCategory() { return feeCategory; }
    public void setFeeCategory(Fee.FeeCategory feeCategory) { this.feeCategory = feeCategory; }

    public int getClasseId() { return classeId; }
    public void setClasseId(int classeId) { this.classeId = classeId; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }

    public double getOriginalAmount() { return originalAmount; }
    public void setOriginalAmount(double originalAmount) { this.originalAmount = originalAmount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public double getReductionAmount() { return reductionAmount; }
    public void setReductionAmount(double reductionAmount) { this.reductionAmount = reductionAmount; }

    public String getReductionMotif() { return reductionMotif; }
    public void setReductionMotif(String reductionMotif) { this.reductionMotif = reductionMotif; }

    public String getReductionAuthor() { return reductionAuthor; }
    public void setReductionAuthor(String reductionAuthor) { this.reductionAuthor = reductionAuthor; }

    public LocalDate getReductionDate() { return reductionDate; }
    public void setReductionDate(LocalDate reductionDate) { this.reductionDate = reductionDate; }

    public double getTotalPaid() { return totalPaid; }
    public void setTotalPaid(double totalPaid) { this.totalPaid = totalPaid; }

    public AssignmentStatus getStatus() { return status; }
    public void setStatus(AssignmentStatus status) { this.status = status; }

    public List<FeeInstallment> getInstallments() { return installments; }
    public void setInstallments(List<FeeInstallment> installments) { this.installments = installments; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getAssignedBy() { return assignedBy; }
    public void setAssignedBy(String assignedBy) { this.assignedBy = assignedBy; }

    @Override
    public String toString() {
        return feeName + " — " + studentName
                + " (" + String.format("%.2f", getAdjustedAmount()) + " " + currency + ")";
    }
}

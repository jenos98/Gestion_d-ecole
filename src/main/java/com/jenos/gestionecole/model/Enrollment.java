package com.jenos.gestionecole.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Enrollment — Enregistrement officiel de l'admission d'un étudiant pour une année scolaire.
 */
public class Enrollment {

    public enum EnrollmentType {
        PRIMO_INSCRIPTION("Nouvelle Inscription"),
        REINSCRIPTION("Réinscription / Passage de classe");

        private final String label;

        EnrollmentType(String label) {
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
    private Student student;
    private String academicYear;
    private Classe classe;
    private EnrollmentType type;
    private LocalDate enrollmentDate;
    private LocalDateTime createdAt;
    private boolean paidImmediately;
    private Payment payment;
    private String agentUsername;
    private String notes;

    public Enrollment() {
        this.enrollmentDate = LocalDate.now();
        this.createdAt = LocalDateTime.now();
        this.type = EnrollmentType.PRIMO_INSCRIPTION;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }

    public Classe getClasse() { return classe; }
    public void setClasse(Classe classe) { this.classe = classe; }

    public EnrollmentType getType() { return type; }
    public void setType(EnrollmentType type) { this.type = type; }

    public LocalDate getEnrollmentDate() { return enrollmentDate; }
    public void setEnrollmentDate(LocalDate enrollmentDate) { this.enrollmentDate = enrollmentDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isPaidImmediately() { return paidImmediately; }
    public void setPaidImmediately(boolean paidImmediately) { this.paidImmediately = paidImmediately; }

    public Payment getPayment() { return payment; }
    public void setPayment(Payment payment) { this.payment = payment; }

    public String getAgentUsername() { return agentUsername; }
    public void setAgentUsername(String agentUsername) { this.agentUsername = agentUsername; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}

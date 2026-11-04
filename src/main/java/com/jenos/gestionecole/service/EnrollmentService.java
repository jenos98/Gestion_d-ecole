package com.jenos.gestionecole.service;

import com.jenos.gestionecole.model.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * EnrollmentService — Service métier d'admission et réinscription des étudiants.
 */
public class EnrollmentService {

    private static EnrollmentService instance;
    private final List<Enrollment> enrollments = new ArrayList<>();
    private final StudentService studentService = StudentService.getInstance();
    private final PaymentService paymentService = PaymentService.getInstance();

    private EnrollmentService() {}

    public static EnrollmentService getInstance() {
        if (instance == null) {
            instance = new EnrollmentService();
        }
        return instance;
    }

    /**
     * Enrôlement d'un nouvel étudiant (Primo-inscription).
     */
    public Enrollment processNewEnrollment(Student student, Classe classe, String academicYear,
                                          boolean payNow, Fee fee, double amount, String currency,
                                          Payment.PaymentMethod method, String paymentRef,
                                          String agentUsername) {
        student.setClasse(classe);
        student.setAnneeScolaire(academicYear);
        student.setDateInscription(LocalDate.now());
        student.setStatut(Student.StudentStatus.ACTIF);

        // Validation
        StudentValidator.ValidationResult vResult = StudentValidator.validate(student, studentService);
        if (!vResult.isValid()) {
            throw new IllegalArgumentException(vResult.getFirstError());
        }

        // Sauvegarde de l'étudiant
        studentService.save(student);

        // Création de l'enrôlement
        Enrollment enr = new Enrollment();
        enr.setId(enrollments.size() + 1);
        enr.setStudent(student);
        enr.setClasse(classe);
        enr.setAcademicYear(academicYear);
        enr.setType(Enrollment.EnrollmentType.PRIMO_INSCRIPTION);
        enr.setPaidImmediately(payNow);
        enr.setAgentUsername(agentUsername);

        if (payNow && amount > 0) {
            Payment p = new Payment();
            p.setPaymentType(Payment.PaymentType.ELEVE);
            p.setStudent(student);
            p.setFee(fee);
            p.setAmount(amount);
            p.setCurrency(currency);
            p.setPaymentMethod(method != null ? method : Payment.PaymentMethod.ESPECES);
            p.setReference(paymentRef);
            p.setCashierUsername(agentUsername);
            p.setReceiptNumber("REC-ENR-" + (System.currentTimeMillis() % 100000));
            paymentService.processPayment(p);
            enr.setPayment(p);
        }

        enrollments.add(enr);
        return enr;
    }

    /**
     * Réinscription d'un étudiant déjà existant pour une nouvelle année scolaire.
     */
    public Enrollment processReEnrollment(Student student, Classe newClasse, String newAcademicYear,
                                         boolean payNow, Fee fee, double amount, String currency,
                                         Payment.PaymentMethod method, String paymentRef,
                                         String agentUsername) {
        if (student == null) {
            throw new IllegalArgumentException("Veuillez sélectionner un étudiant existant.");
        }
        if (newClasse == null) {
            throw new IllegalArgumentException("La classe de réaffectation est obligatoire.");
        }
        if (newAcademicYear == null || newAcademicYear.isBlank()) {
            throw new IllegalArgumentException("L'année scolaire est obligatoire.");
        }

        // Contrôle anti-doublon d'inscription sur la même année scolaire
        if (studentService.isAlreadyEnrolledInYear(student.getNom(), student.getPostnom(), newAcademicYear, student.getId())) {
            throw new IllegalArgumentException("L'étudiant " + student.getFullName() + " est déjà inscrit pour l'année scolaire " + newAcademicYear + ".");
        }

        // Mise à jour de l'étudiant
        student.setClasse(newClasse);
        student.setAnneeScolaire(newAcademicYear);
        student.setStatut(Student.StudentStatus.ACTIF);
        studentService.save(student);

        // Création de l'enrôlement
        Enrollment enr = new Enrollment();
        enr.setId(enrollments.size() + 1);
        enr.setStudent(student);
        enr.setClasse(newClasse);
        enr.setAcademicYear(newAcademicYear);
        enr.setType(Enrollment.EnrollmentType.REINSCRIPTION);
        enr.setPaidImmediately(payNow);
        enr.setAgentUsername(agentUsername);

        if (payNow && amount > 0) {
            Payment p = new Payment();
            p.setPaymentType(Payment.PaymentType.ELEVE);
            p.setStudent(student);
            p.setFee(fee);
            p.setAmount(amount);
            p.setCurrency(currency);
            p.setPaymentMethod(method != null ? method : Payment.PaymentMethod.ESPECES);
            p.setReference(paymentRef);
            p.setCashierUsername(agentUsername);
            p.setReceiptNumber("REC-REIN-" + (System.currentTimeMillis() % 100000));
            paymentService.processPayment(p);
            enr.setPayment(p);
        }

        enrollments.add(enr);
        return enr;
    }

    public List<Enrollment> getRecentEnrollments() {
        return new ArrayList<>(enrollments);
    }
}

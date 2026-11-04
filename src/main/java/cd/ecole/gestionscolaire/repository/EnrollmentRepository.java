package cd.ecole.gestionscolaire.repository;

import java.time.LocalDate;
import java.util.List;

/**
 * EnrollmentRepository — Contrat de gestion des inscriptions scolaires par promotion, option et classe.
 */
public interface EnrollmentRepository {

    long enrollStudent(long studentId, long classeId, String academicYear, LocalDate enrollmentDate);

    void transferClass(long studentId, long targetClasseId);

    List<Long> findStudentIdsByClasse(long classeId, String academicYear);
}

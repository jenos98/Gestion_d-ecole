package cd.ecole.gestionscolaire.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * StudentRepository — Contrat d'accès aux données des élèves (PostgreSQL).
 */
public interface StudentRepository {

    Optional<StudentRecord> findById(long id);

    Optional<StudentRecord> findByMatricule(String matricule);

    List<StudentRecord> findByClasse(long classeId, String academicYear);

    long save(StudentRecord student);

    void archive(long id);

    record StudentRecord(
            long id,
            String matricule,
            String firstName,
            String lastName,
            String postName,
            String gender,
            LocalDate birthDate,
            String address,
            String phone,
            long classeId,
            String academicYear,
            String status,
            String guardianName,
            String guardianPhone,
            String guardianRelation
    ) {}
}

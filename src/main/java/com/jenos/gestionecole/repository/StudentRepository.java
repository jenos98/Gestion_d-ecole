package com.jenos.gestionecole.repository;

import cd.ecole.gestionscolaire.config.DatabaseConfig;
import com.jenos.gestionecole.model.Student;
import com.jenos.gestionecole.model.StudentFilter;
import com.jenos.gestionecole.model.StudentSearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * StudentRepository — Accès aux données persistées des étudiants avec synchronisation PostgreSQL.
 * Aucune suppression physique n'est opérée (statut non destructif INACTIF / ARCHIVE).
 */
public class StudentRepository {

    private static final Logger logger = LoggerFactory.getLogger(StudentRepository.class);
    private final List<Student> storage = new ArrayList<>();

    public StudentRepository(ClasseRepository classeRepo) {
        initDefaultStudents(classeRepo);
    }

    private void initDefaultStudents(ClasseRepository classeRepo) {
        if (DatabaseConfig.isDatabaseReachable()) {
            loadFromDatabase(classeRepo);
            if (!storage.isEmpty()) {
                logger.info("{} étudiants chargés depuis PostgreSQL.", storage.size());
                return;
            }
        }
        var c5mpa = classeRepo.findById(1);   // 6e Math-Physique A (Scientifique)
        var c5mpb = classeRepo.findById(2);   // 6e Math-Physique B (Scientifique)
        var c5bioa = classeRepo.findById(3);  // 6e Bio-Chimie A (Scientifique)
        var c5comma = classeRepo.findById(4); // 5e Commerciale A (Commerciale)
        var c5commb = classeRepo.findById(5); // 5e Commerciale B (Commerciale)
        var cPrim6a = classeRepo.findById(6); // 6e Primaire A (Primaire sans option)
        var cPrim6b = classeRepo.findById(7); // 6e Primaire B (Primaire sans option)

        // 1. Secondaire Scientifique
        Student s1 = new Student(1, "2025-SEC-0001", "Mukendi", "Tshilumba", "Jonathan", "M", c5mpa, "2024-2025");
        s1.setDateNaissance(LocalDate.of(2007, 3, 14));
        s1.setTelephone("+243 81 234 5678");
        s1.setGuardianName("Tshilumba David");
        s1.setGuardianPhone("+243 81 999 0001");
        s1.setGuardianRelation("Père");
        s1.setDateInscription(LocalDate.of(2024, 9, 2));

        // 2. Secondaire Commerciale
        Student s2 = new Student(2, "2025-SEC-0002", "Ilunga", "Kapinga", "Esther", "F", c5comma, "2024-2025");
        s2.setDateNaissance(LocalDate.of(2008, 7, 21));
        s2.setTelephone("+243 82 345 6789");
        s2.setGuardianName("Kapinga Marie");
        s2.setGuardianPhone("+243 82 999 0002");
        s2.setGuardianRelation("Mère");
        s2.setDateInscription(LocalDate.of(2024, 9, 3));

        // 3. Secondaire Scientifique
        Student s3 = new Student(3, "2025-SEC-0003", "Kabange", "Mwamba", "David", "M", c5mpb, "2024-2025");
        s3.setDateNaissance(LocalDate.of(2007, 11, 5));
        s3.setTelephone("+243 99 456 7890");
        s3.setGuardianName("Mwamba Paul");
        s3.setGuardianPhone("+243 99 999 0003");
        s3.setGuardianRelation("Père");
        s3.setDateInscription(LocalDate.of(2024, 9, 4));

        // 4. Secondaire Commerciale
        Student s4 = new Student(4, "2025-SEC-0004", "Nyemba", "Biduaya", "Sarah", "F", c5commb, "2024-2025");
        s4.setDateNaissance(LocalDate.of(2007, 1, 18));
        s4.setTelephone("+243 90 567 8901");
        s4.setGuardianName("Biduaya Jacques");
        s4.setGuardianPhone("+243 90 999 0004");
        s4.setGuardianRelation("Oncle");
        s4.setDateInscription(LocalDate.of(2024, 9, 5));

        // 5. Secondaire Scientifique (Bio-Chimie)
        Student s5 = new Student(5, "2025-SEC-0005", "Tshisekedi", "Kalombo", "Daniel", "M", c5bioa, "2024-2025");
        s5.setDateNaissance(LocalDate.of(2007, 8, 9));
        s5.setTelephone("+243 81 678 9012");
        s5.setGuardianName("Kalombo Simon");
        s5.setGuardianPhone("+243 81 999 0005");
        s5.setGuardianRelation("Père");
        s5.setDateInscription(LocalDate.of(2024, 9, 6));

        // 6. Primaire (Sans option)
        Student s6 = new Student(6, "2025-PRI-0001", "Kabila", "Lumumba", "Joseph", "M", cPrim6a, "2024-2025");
        s6.setDateNaissance(LocalDate.of(2013, 5, 12));
        s6.setTelephone("+243 82 111 2233");
        s6.setGuardianName("Lumumba Patrice");
        s6.setGuardianPhone("+243 82 000 1122");
        s6.setGuardianRelation("Père");
        s6.setDateInscription(LocalDate.of(2024, 9, 1));

        // 7. Primaire (Sans option)
        Student s7 = new Student(7, "2025-PRI-0002", "Kasavubu", "Kanza", "Grace", "F", cPrim6b, "2024-2025");
        s7.setDateNaissance(LocalDate.of(2013, 9, 25));
        s7.setTelephone("+243 99 222 3344");
        s7.setGuardianName("Kanza Daniel");
        s7.setGuardianPhone("+243 99 111 2233");
        s7.setGuardianRelation("Tuteur");
        s7.setDateInscription(LocalDate.of(2024, 9, 1));

        storage.add(s1);
        storage.add(s2);
        storage.add(s3);
        storage.add(s4);
        storage.add(s5);
        storage.add(s6);
        storage.add(s7);
    }

    /**
     * Recherche unifiée basée sur l'objet StudentFilter, avec tri réel et pagination.
     */
    public StudentSearchResult search(StudentFilter filter) {
        List<Student> filtered = storage.stream()
                .filter(s -> matchesFilter(s, filter))
                .collect(Collectors.toList());

        int totalCount = filtered.size();

        // Application du tri réel
        applySorting(filtered, filter != null ? filter.getSortField() : StudentFilter.SortField.NOM,
                     filter != null && filter.isSortAscending());

        // Pagination
        int page = filter != null ? filter.getPage() : 1;
        int pageSize = filter != null ? filter.getPageSize() : 25;

        int fromIndex = Math.max(0, (page - 1) * pageSize);
        if (fromIndex >= filtered.size() && totalCount > 0) {
            fromIndex = 0;
            page = 1;
        }

        int toIndex = Math.min(filtered.size(), fromIndex + pageSize);
        List<Student> pagedItems = (fromIndex < filtered.size())
                ? new ArrayList<>(filtered.subList(fromIndex, toIndex))
                : new ArrayList<>();

        return new StudentSearchResult(pagedItems, totalCount, page, pageSize);
    }

    private boolean matchesFilter(Student s, StudentFilter f) {
        if (f == null) return true;

        // Statut
        if (f.getStatus() != null && s.getStatut() != f.getStatus()) {
            return false;
        }

        // Année scolaire
        if (f.getAcademicYear() != null && !f.getAcademicYear().isBlank()) {
            if (!f.getAcademicYear().equalsIgnoreCase(s.getAnneeScolaire())) return false;
        }

        // Section
        if (f.getSection() != null) {
            if (s.getSection() == null || s.getSection().getId() != f.getSection().getId()) return false;
        }

        // Promotion
        if (f.getPromotion() != null) {
            if (s.getPromotion() == null || s.getPromotion().getId() != f.getPromotion().getId()) return false;
        }

        // Option (quand filtrée)
        if (f.getOption() != null) {
            if (s.getOption() == null || s.getOption().getId() != f.getOption().getId()) return false;
        }

        // Classe
        if (f.getClasse() != null) {
            if (s.getClasse() == null || s.getClasse().getId() != f.getClasse().getId()) return false;
        }

        // Recherche textuelle combinée (nom, postnom, prénom, matricule)
        if (f.getSearchTerm() != null && !f.getSearchTerm().isBlank()) {
            String kw = f.getSearchTerm().toLowerCase().trim();
            boolean match = (s.getMatricule() != null && s.getMatricule().toLowerCase().contains(kw))
                    || (s.getNom() != null && s.getNom().toLowerCase().contains(kw))
                    || (s.getPostnom() != null && s.getPostnom().toLowerCase().contains(kw))
                    || (s.getPrenom() != null && s.getPrenom().toLowerCase().contains(kw))
                    || (s.getTelephone() != null && s.getTelephone().contains(kw));
            if (!match) return false;
        }

        return true;
    }

    private void applySorting(List<Student> list, StudentFilter.SortField field, boolean ascending) {
        if (field == null) field = StudentFilter.SortField.NOM;

        Comparator<Student> comp = switch (field) {
            case NOM -> Comparator.comparing(Student::getFullName, String.CASE_INSENSITIVE_ORDER);
            case MATRICULE -> Comparator.comparing(Student::getMatricule, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case PROMOTION -> Comparator.comparing(Student::getPromotionName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case OPTION -> Comparator.comparing(Student::getOptionName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case CLASSE -> Comparator.comparing(Student::getClassName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case ACADEMIC_YEAR -> Comparator.comparing(Student::getAnneeScolaire, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case STATUT -> Comparator.comparing(s -> s.getStatut().name());
        };

        if (!ascending) {
            comp = comp.reversed();
        }

        list.sort(comp);
    }

    public void save(Student student) {
        if (student == null) return;
        if (student.getId() == 0) {
            int maxId = storage.stream().mapToInt(Student::getId).max().orElse(0);
            student.setId(maxId + 1);
            storage.add(student);
            if (DatabaseConfig.isDatabaseReachable()) {
                saveToDatabase(student, true);
            }
        } else {
            for (int i = 0; i < storage.size(); i++) {
                if (storage.get(i).getId() == student.getId()) {
                    storage.set(i, student);
                    break;
                }
            }
            if (DatabaseConfig.isDatabaseReachable()) {
                saveToDatabase(student, false);
            }
        }
    }

    public void deactivate(int studentId) {
        for (Student s : storage) {
            if (s.getId() == studentId) {
                s.setStatut(Student.StudentStatus.INACTIF);
                updateStatusInDatabase(studentId, "INACTIF");
                return;
            }
        }
    }

    public void archive(int studentId) {
        for (Student s : storage) {
            if (s.getId() == studentId) {
                s.setStatut(Student.StudentStatus.ARCHIVE);
                updateStatusInDatabase(studentId, "ARCHIVE");
                return;
            }
        }
    }

    private void saveToDatabase(Student student, boolean isInsert) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            if (isInsert) {
                String sql = "INSERT INTO students (matricule, first_name, last_name, post_name, gender, birth_date, phone, classe_id, academic_year, status, guardian_name, guardian_phone, guardian_relation) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, student.getMatricule());
                    ps.setString(2, student.getPrenom() != null ? student.getPrenom() : "");
                    ps.setString(3, student.getNom() != null ? student.getNom() : "");
                    ps.setString(4, student.getPostnom() != null ? student.getPostnom() : "");
                    ps.setString(5, student.getSexe() != null ? student.getSexe() : "M");
                    ps.setDate(6, student.getDateNaissance() != null ? Date.valueOf(student.getDateNaissance()) : null);
                    ps.setString(7, student.getTelephone());
                    if (student.getClasse() != null) {
                        ps.setInt(8, student.getClasse().getId());
                    } else {
                        ps.setNull(8, Types.INTEGER);
                    }
                    ps.setString(9, student.getAnneeScolaire() != null ? student.getAnneeScolaire() : "2024-2025");
                    ps.setString(10, student.getStatut() != null ? student.getStatut().name() : "ACTIF");
                    ps.setString(11, student.getGuardianName());
                    ps.setString(12, student.getGuardianPhone());
                    ps.setString(13, student.getGuardianRelation());
                    ps.executeUpdate();
                }
            } else {
                String sql = "UPDATE students SET matricule = ?, first_name = ?, last_name = ?, post_name = ?, gender = ?, birth_date = ?, phone = ?, classe_id = ?, academic_year = ?, status = ?, guardian_name = ?, guardian_phone = ?, guardian_relation = ? WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, student.getMatricule());
                    ps.setString(2, student.getPrenom() != null ? student.getPrenom() : "");
                    ps.setString(3, student.getNom() != null ? student.getNom() : "");
                    ps.setString(4, student.getPostnom() != null ? student.getPostnom() : "");
                    ps.setString(5, student.getSexe() != null ? student.getSexe() : "M");
                    ps.setDate(6, student.getDateNaissance() != null ? Date.valueOf(student.getDateNaissance()) : null);
                    ps.setString(7, student.getTelephone());
                    if (student.getClasse() != null) {
                        ps.setInt(8, student.getClasse().getId());
                    } else {
                        ps.setNull(8, Types.INTEGER);
                    }
                    ps.setString(9, student.getAnneeScolaire() != null ? student.getAnneeScolaire() : "2024-2025");
                    ps.setString(10, student.getStatut() != null ? student.getStatut().name() : "ACTIF");
                    ps.setString(11, student.getGuardianName());
                    ps.setString(12, student.getGuardianPhone());
                    ps.setString(13, student.getGuardianRelation());
                    ps.setInt(14, student.getId());
                    ps.executeUpdate();
                }
            }
        } catch (Exception e) {
            logger.warn("Synchronisation PostgreSQL pour l'étudiant {} échouée : {}", student.getMatricule(), e.getMessage());
        }
    }

    private void updateStatusInDatabase(int id, String status) {
        if (!DatabaseConfig.isDatabaseReachable()) return;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE students SET status = ? WHERE id = ?")) {
            ps.setString(1, status);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (Exception e) {
            logger.warn("Erreur mise à jour statut PostgreSQL étudiant {} : {}", id, e.getMessage());
        }
    }

    private void loadFromDatabase(ClasseRepository classeRepo) {
        String sql = "SELECT id, matricule, first_name, last_name, post_name, gender, birth_date, phone, classe_id, academic_year, status, guardian_name, guardian_phone, guardian_relation FROM students ORDER BY id";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String mat = rs.getString("matricule");
                String prenom = rs.getString("first_name");
                String nom = rs.getString("last_name");
                String postnom = rs.getString("post_name");
                String sexe = rs.getString("gender");
                int classeId = rs.getInt("classe_id");
                String annee = rs.getString("academic_year");
                var classe = classeRepo != null ? classeRepo.findById(classeId) : null;

                Student s = new Student(id, mat, nom, postnom, prenom, sexe, classe, annee);
                Date bDate = rs.getDate("birth_date");
                if (bDate != null) s.setDateNaissance(bDate.toLocalDate());
                s.setTelephone(rs.getString("phone"));
                s.setGuardianName(rs.getString("guardian_name"));
                s.setGuardianPhone(rs.getString("guardian_phone"));
                s.setGuardianRelation(rs.getString("guardian_relation"));
                String statusStr = rs.getString("status");
                if (statusStr != null) {
                    try {
                        s.setStatut(Student.StudentStatus.valueOf(statusStr));
                    } catch (Exception ignored) {}
                }
                storage.add(s);
            }
        } catch (Exception e) {
            logger.warn("Impossible de lire les étudiants depuis PostgreSQL : {}", e.getMessage());
        }
    }

    public Student findById(int id) {
        for (Student s : storage) {
            if (s.getId() == id) return s;
        }
        return null;
    }

    public boolean existsByMatricule(String matricule, int excludeId) {
        if (matricule == null || matricule.isBlank()) return false;
        for (Student s : storage) {
            if (s.getId() != excludeId && matricule.equalsIgnoreCase(s.getMatricule())) {
                return true;
            }
        }
        return false;
    }

    public boolean isAlreadyEnrolledInYear(String nom, String postnom, String anneeScolaire, int excludeId) {
        if (nom == null || postnom == null || anneeScolaire == null) return false;
        for (Student s : storage) {
            if (s.getId() != excludeId
                    && nom.equalsIgnoreCase(s.getNom())
                    && postnom.equalsIgnoreCase(s.getPostnom())
                    && anneeScolaire.equalsIgnoreCase(s.getAnneeScolaire())) {
                return true;
            }
        }
        return false;
    }

    public int countTotal() {
        return storage.size();
    }
}

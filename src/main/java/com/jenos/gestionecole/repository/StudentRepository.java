package com.jenos.gestionecole.repository;

import com.jenos.gestionecole.model.Student;
import com.jenos.gestionecole.model.StudentFilter;
import com.jenos.gestionecole.model.StudentSearchResult;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * StudentRepository — Accès aux données persistées des étudiants.
 * Aucune suppression physique n'est opérée (statut non destructif INACTIF / ARCHIVE).
 */
public class StudentRepository {

    private final List<Student> storage = new ArrayList<>();

    public StudentRepository(ClasseRepository classeRepo) {
        initDefaultStudents(classeRepo);
    }

    private void initDefaultStudents(ClasseRepository classeRepo) {
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
        } else {
            for (int i = 0; i < storage.size(); i++) {
                if (storage.get(i).getId() == student.getId()) {
                    storage.set(i, student);
                    return;
                }
            }
        }
    }

    public void deactivate(int studentId) {
        for (Student s : storage) {
            if (s.getId() == studentId) {
                s.setStatut(Student.StudentStatus.INACTIF);
                return;
            }
        }
    }

    public void archive(int studentId) {
        for (Student s : storage) {
            if (s.getId() == studentId) {
                s.setStatut(Student.StudentStatus.ARCHIVE);
                return;
            }
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

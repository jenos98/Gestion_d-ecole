package com.jenos.gestionecole;

import com.jenos.gestionecole.model.*;
import com.jenos.gestionecole.service.AcademicStructureService;
import com.jenos.gestionecole.service.StudentService;
import com.jenos.gestionecole.service.StudentValidator;

import java.time.LocalDate;
import java.util.List;

public class StudentModuleTest {

    public static void main(String[] args) {
        System.out.println("=== DEBUT DES TESTS DU BLOC 2 : MODULE ÉLÈVES ===");

        StudentService studentService = StudentService.getInstance();
        AcademicStructureService structureService = AcademicStructureService.getInstance();

        // 1. Test Hiérarchie Section -> Promotion -> Option -> Classe
        List<Section> sections = structureService.findAllSections();
        assertFalse(sections.isEmpty(), "Au moins une section doit exister");
        System.out.println("✓ Sections trouvées : " + sections.size());

        Section secSecondaire = sections.stream()
                .filter(s -> s.getName().toLowerCase().contains("secondaire"))
                .findFirst().orElse(null);
        assertTrue(secSecondaire != null, "Section Secondaire présente");

        Section secPrimaire = sections.stream()
                .filter(s -> s.getName().toLowerCase().contains("primaire"))
                .findFirst().orElse(null);
        assertTrue(secPrimaire != null, "Section Primaire présente");

        // Promotions
        List<Promotion> promoSecondaire = structureService.findPromotionsBySection(secSecondaire);
        assertFalse(promoSecondaire.isEmpty(), "Promotions secondaire non vides");
        Promotion promo6e = promoSecondaire.get(0);
        assertTrue(structureService.promotionHasOptions(promo6e), "6e Secondaire doit avoir des options");

        List<Promotion> promoPrimaire = structureService.findPromotionsBySection(secPrimaire);
        assertFalse(promoPrimaire.isEmpty(), "Promotions primaire non vides");
        Promotion promoPrim6e = promoPrimaire.get(0);
        assertFalse(structureService.promotionHasOptions(promoPrim6e), "Primaire ne doit PAS avoir d'options (désactivé)");
        System.out.println("✓ Gestion des options : Secondaire=avec options, Primaire=sans options");

        // 2. Test StudentFilter et Tri réel par Option
        StudentFilter filter = new StudentFilter();
        filter.setSort(StudentFilter.SortField.OPTION, true); // Ascendant A -> Z
        StudentSearchResult resAsc = studentService.search(filter);
        System.out.println("✓ Recherche avec StudentFilter : total = " + resAsc.getTotalCount());
        assertTrue(resAsc.getTotalCount() > 0, "Résultats présents");

        List<Student> itemsAsc = resAsc.getItems();
        for (int i = 0; i < itemsAsc.size() - 1; i++) {
            String opt1 = itemsAsc.get(i).getOptionName();
            String opt2 = itemsAsc.get(i + 1).getOptionName();
            assertTrue(opt1.compareToIgnoreCase(opt2) <= 0, "Tri Option A->Z respecté : " + opt1 + " <= " + opt2);
        }
        System.out.println("✓ Tri réel Option A -> Z vérifié avec succès");

        filter.setSort(StudentFilter.SortField.OPTION, false); // Descendant Z -> A
        StudentSearchResult resDesc = studentService.search(filter);
        List<Student> itemsDesc = resDesc.getItems();
        for (int i = 0; i < itemsDesc.size() - 1; i++) {
            String opt1 = itemsDesc.get(i).getOptionName();
            String opt2 = itemsDesc.get(i + 1).getOptionName();
            assertTrue(opt1.compareToIgnoreCase(opt2) >= 0, "Tri Option Z->A respecté : " + opt1 + " >= " + opt2);
        }
        System.out.println("✓ Tri réel Option Z -> A vérifié avec succès");

        // 3. Test StudentValidator
        Student validStudent = new Student();
        validStudent.setNom("Tshilumba");
        validStudent.setPostnom("Kabeya");
        validStudent.setPrenom("Samuel");
        validStudent.setSexe("M");
        validStudent.setAnneeScolaire("2024-2025");
        validStudent.setSection(secPrimaire);
        validStudent.setPromotion(promoPrim6e);
        Classe classePrim = structureService.findClassesByPromotion(promoPrim6e).get(0);
        validStudent.setClasse(classePrim);
        validStudent.setMatricule("2025-PRI-9999");
        validStudent.setGuardianName("Kabeya Pierre");
        validStudent.setGuardianPhone("+243 81 000 9999");
        validStudent.setGuardianRelation("Père");

        StudentValidator.ValidationResult vRes = StudentValidator.validate(validStudent, studentService);
        assertTrue(vRes.isValid(), "L'élève valide doit passer la validation");
        System.out.println("✓ Validation élève conforme réussie");

        // Invalide : incohérence hiérarchique volontaire
        Student invalidStudent = new Student();
        invalidStudent.setNom("Invalide");
        invalidStudent.setPostnom("Test");
        invalidStudent.setPrenom("Jean");
        invalidStudent.setSexe("M");
        invalidStudent.setAnneeScolaire("2024-2025");
        invalidStudent.setSection(secPrimaire);
        invalidStudent.setPromotion(promo6e); // Promo secondaire dans section primaire !
        invalidStudent.setClasse(classePrim);
        invalidStudent.setMatricule("2025-ERR-0001");

        StudentValidator.ValidationResult vErr = StudentValidator.validate(invalidStudent, studentService);
        assertFalse(vErr.isValid(), "L'élève avec incohérence hiérarchique doit être rejeté");
        System.out.println("✓ Rejet de l'incohérence hiérarchique validé : " + vErr.getFirstError());

        // 4. Test Persistance et Archivage non destructif
        studentService.save(validStudent);
        Student saved = studentService.findById(validStudent.getId());
        assertTrue(saved != null, "Élève persisté");
        assertEquals(Student.StudentStatus.ACTIF, saved.getStatut(), "Statut initial ACTIF");

        // Archivage
        studentService.archive(saved.getId());
        Student archived = studentService.findById(saved.getId());
        assertEquals(Student.StudentStatus.ARCHIVE, archived.getStatut(), "Statut passé à ARCHIVE");
        System.out.println("✓ Archivage non destructif vérifié : statut = " + archived.getStatut());

        System.out.println("=== TOUS LES TESTS DU BLOC 2 SONT PASSÉS AVEC SUCCÈS ===");
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("ÉCHEC: " + message);
        }
    }

    private static void assertFalse(boolean condition, String message) {
        if (condition) {
            throw new AssertionError("ÉCHEC: " + message);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError("ÉCHEC: " + message + " (attendu: " + expected + ", obtenu: " + actual + ")");
        }
    }
}

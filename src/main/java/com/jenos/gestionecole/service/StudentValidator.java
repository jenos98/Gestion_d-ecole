package com.jenos.gestionecole.service;

import com.jenos.gestionecole.model.Student;

import java.util.ArrayList;
import java.util.List;

/**
 * StudentValidator — Validateur des données d'un élève avant persistance.
 *
 * RÈGLES FORMELLES :
 * - Nom, Postnom, Prénom obligatoires
 * - Sexe obligatoire
 * - Section, Promotion, Classe obligatoires
 * - Option obligatoire si la promotion en possède
 * - Matricule obligatoire et unique
 * - Un étudiant ne peut pas être inscrit deux fois dans la même année scolaire
 */
public class StudentValidator {

    private StudentValidator() {}

    public static class ValidationResult {
        private final List<String> errors = new ArrayList<>();

        public void addError(String field, String message) {
            errors.add(field + " : " + message);
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public List<String> getErrors() {
            return errors;
        }

        public String getFirstError() {
            return errors.isEmpty() ? null : errors.get(0);
        }
    }

    public static ValidationResult validate(Student student, StudentService service) {
        ValidationResult result = new ValidationResult();

        // ── 1. Identité ──────────────────────────────────────────────
        if (isBlank(student.getNom())) {
            result.addError("Nom", "Le nom est obligatoire.");
        }
        if (isBlank(student.getPostnom())) {
            result.addError("Postnom", "Le postnom est obligatoire.");
        }
        if (isBlank(student.getPrenom())) {
            result.addError("Prénom", "Le prénom est obligatoire.");
        }
        if (isBlank(student.getSexe())) {
            result.addError("Sexe", "Le sexe est obligatoire.");
        }

        // ── 2. Scolarité ─────────────────────────────────────────────
        if (isBlank(student.getAnneeScolaire())) {
            result.addError("Année scolaire", "L'année scolaire est obligatoire.");
        }
        if (student.getSection() == null) {
            result.addError("Section", "La section est obligatoire.");
        }
        if (student.getPromotion() == null) {
            result.addError("Promotion", "La promotion est obligatoire.");
        }

        // Vérification cohérence Section -> Promotion
        if (student.getSection() != null && student.getPromotion() != null) {
            if (student.getPromotion().getSection() != null &&
                student.getPromotion().getSection().getId() != student.getSection().getId()) {
                result.addError("Hiérarchie", "La promotion sélectionnée n'appartient pas à la section indiquée.");
            }
        }

        // Option : obligatoire seulement si la promotion en possède
        AcademicStructureService structService = AcademicStructureService.getInstance();
        if (student.getPromotion() != null) {
            boolean hasOptions = structService.promotionHasOptions(student.getPromotion());
            if (hasOptions && student.getOption() == null) {
                result.addError("Option", "L'option d'étude est obligatoire pour cette promotion.");
            }
            // Cohérence Promotion -> Option
            if (student.getOption() != null && student.getOption().getPromotion() != null &&
                student.getOption().getPromotion().getId() != student.getPromotion().getId()) {
                result.addError("Hiérarchie", "L'option sélectionnée n'appartient pas à la promotion indiquée.");
            }
        }

        if (student.getClasse() == null) {
            result.addError("Classe", "La classe est obligatoire.");
        } else {
            // Cohérence Option/Promotion -> Classe
            if (student.getOption() != null && student.getClasse().getOption() != null) {
                if (student.getClasse().getOption().getId() != student.getOption().getId()) {
                    result.addError("Hiérarchie", "La classe sélectionnée ne correspond pas à l'option d'étude choisie.");
                }
            } else if (student.getPromotion() != null && student.getClasse().getPromotion() != null) {
                if (student.getClasse().getPromotion().getId() != student.getPromotion().getId()) {
                    result.addError("Hiérarchie", "La classe sélectionnée ne correspond pas à la promotion choisie.");
                }
            }
        }

        // ── 3. Matricule ─────────────────────────────────────────────
        if (isBlank(student.getMatricule())) {
            result.addError("Matricule", "Le matricule est obligatoire.");
        } else if (!service.isMatriculeUnique(student.getMatricule(), student.getId())) {
            result.addError("Matricule", "Ce matricule est déjà attribué à un autre étudiant.");
        }

        // ── 4. Règle anti-doublon d'inscription par année ───────────
        if (!isBlank(student.getNom()) && !isBlank(student.getPostnom()) && !isBlank(student.getAnneeScolaire())) {
            if (service.isAlreadyEnrolledInYear(student.getNom(), student.getPostnom(), student.getAnneeScolaire(), student.getId())) {
                result.addError("Inscription", "Cet étudiant est déjà inscrit pour l'année scolaire " + student.getAnneeScolaire() + ".");
            }
        }

        return result;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}

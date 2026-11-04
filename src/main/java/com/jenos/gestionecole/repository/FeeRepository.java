package com.jenos.gestionecole.repository;

import com.jenos.gestionecole.model.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * FeeRepository — Gestion de la persistance de la grille tarifaire.
 *
 * RÈGLE ABSOLUE : Les montants USD et CDF sont traités séparément,
 * JAMAIS additionnés ni convertis implicitement.
 * Aucune suppression physique n'est opérée (statut non destructif).
 */
public class FeeRepository {

    private final List<Fee> storage = new ArrayList<>();

    public FeeRepository(SectionRepository secRepo, PromotionRepository promoRepo,
                         OptionRepository optRepo, ClasseRepository classeRepo) {
        initDefaultFees(secRepo, promoRepo, optRepo, classeRepo);
    }

    private void initDefaultFees(SectionRepository secRepo, PromotionRepository promoRepo,
                                OptionRepository optRepo, ClasseRepository classeRepo) {

        Section secPrimaire = secRepo.findById(1);   // Primaire (ID 1)
        Section secSecondaire = secRepo.findById(2); // Secondaire (ID 2)
        Promotion promo6Sec = promoRepo.findById(1); // 6e Secondaire (ID 1)
        AcademicOption optSc = optRepo.findById(1);   // Scientifique (ID 1)
        AcademicOption optCom = optRepo.findById(2);  // Commerciale & Gestion (ID 2)

        // 1. Frais Globaux (Toute l'école)
        Fee f1 = new Fee(1, "Frais de Dossier & Inscription", 50.0, "USD", true,
                Fee.FeeCategory.INSCRIPTION, Fee.ScopeLevel.GLOBAL);
        f1.setDateEcheance(LocalDate.of(2024, 9, 30));

        Fee f2 = new Fee(2, "Assurance & Carnet Scolaire", 15000.0, "CDF", true,
                Fee.FeeCategory.AUTRE, Fee.ScopeLevel.GLOBAL);
        f2.setDateEcheance(LocalDate.of(2024, 10, 15));

        // 2. Frais Section Primaire
        Fee f3 = new Fee(3, "Minerval 1er Trimestre - Primaire", 80.0, "USD", true,
                Fee.FeeCategory.MINERVAL_T1, Fee.ScopeLevel.SECTION);
        f3.setSection(secPrimaire);
        f3.setDateEcheance(LocalDate.of(2024, 10, 31));

        Fee f4 = new Fee(4, "Minerval 2ème Trimestre - Primaire", 80.0, "USD", true,
                Fee.FeeCategory.MINERVAL_T2, Fee.ScopeLevel.SECTION);
        f4.setSection(secPrimaire);
        f4.setDateEcheance(LocalDate.of(2025, 1, 31));

        Fee f5 = new Fee(5, "Minerval 3ème Trimestre - Primaire", 80.0, "USD", true,
                Fee.FeeCategory.MINERVAL_T3, Fee.ScopeLevel.SECTION);
        f5.setSection(secPrimaire);
        f5.setDateEcheance(LocalDate.of(2025, 4, 30));

        // 3. Frais Section Secondaire
        Fee f6 = new Fee(6, "Minerval 1er Trimestre - Secondaire", 150.0, "USD", true,
                Fee.FeeCategory.MINERVAL_T1, Fee.ScopeLevel.SECTION);
        f6.setSection(secSecondaire);
        f6.setDateEcheance(LocalDate.of(2024, 10, 31));

        Fee f7 = new Fee(7, "Minerval 2ème Trimestre - Secondaire", 150.0, "USD", true,
                Fee.FeeCategory.MINERVAL_T2, Fee.ScopeLevel.SECTION);
        f7.setSection(secSecondaire);
        f7.setDateEcheance(LocalDate.of(2025, 1, 31));

        Fee f8 = new Fee(8, "Minerval 3ème Trimestre - Secondaire", 150.0, "USD", true,
                Fee.FeeCategory.MINERVAL_T3, Fee.ScopeLevel.SECTION);
        f8.setSection(secSecondaire);
        f8.setDateEcheance(LocalDate.of(2025, 4, 30));

        // 4. Frais par Option Technique / Scientifique
        Fee f9 = new Fee(9, "Travaux Pratiques & Laboratoire Sciences", 35000.0, "CDF", true,
                Fee.FeeCategory.FRAIS_TECHNIQUES, Fee.ScopeLevel.OPTION);
        f9.setOption(optSc);
        f9.setDateEcheance(LocalDate.of(2024, 11, 15));

        Fee f10 = new Fee(10, "Atelier Informatique & Pratique Commerciale", 35000.0, "CDF", true,
                Fee.FeeCategory.FRAIS_TECHNIQUES, Fee.ScopeLevel.OPTION);
        f10.setOption(optCom);
        f10.setDateEcheance(LocalDate.of(2024, 11, 15));

        // 5. Frais Spécifiques Promotion Diplômante (6e Secondaire)
        Fee f11 = new Fee(11, "Participation aux Épreuves d'Examen d'État", 75.0, "USD", true,
                Fee.FeeCategory.EXAMEN_ETAT, Fee.ScopeLevel.PROMOTION);
        f11.setPromotion(promo6Sec);
        f11.setDateEcheance(LocalDate.of(2025, 2, 28));

        storage.add(f1);
        storage.add(f2);
        storage.add(f3);
        storage.add(f4);
        storage.add(f5);
        storage.add(f6);
        storage.add(f7);
        storage.add(f8);
        storage.add(f9);
        storage.add(f10);
        storage.add(f11);
    }

    public List<Fee> findAll() {
        return new ArrayList<>(storage);
    }

    public List<Fee> findAllActive() {
        return storage.stream().filter(Fee::isActive).collect(Collectors.toList());
    }

    public Fee findById(int id) {
        return storage.stream().filter(f -> f.getId() == id).findFirst().orElse(null);
    }

    /**
     * Résout l'ensemble des frais applicables à un élève selon son rattachement hiérarchique :
     * GLOBAL ∪ SECTION ∪ PROMOTION ∪ OPTION ∪ CLASSE
     */
    public List<Fee> findByStudent(Student student) {
        if (student == null) return new ArrayList<>();

        Classe classe = student.getClasse();
        AcademicOption option = student.getOption();
        Promotion promotion = student.getPromotion();
        Section section = student.getSection();

        return storage.stream()
                .filter(Fee::isActive)
                .filter(f -> isApplicableTo(f, section, promotion, option, classe))
                .collect(Collectors.toList());
    }

    private boolean isApplicableTo(Fee fee, Section sec, Promotion promo, AcademicOption opt, Classe cl) {
        if (fee.getScopeLevel() == null || fee.getScopeLevel() == Fee.ScopeLevel.GLOBAL) {
            return true;
        }

        switch (fee.getScopeLevel()) {
            case SECTION:
                return sec != null && fee.getSection() != null && fee.getSection().getId() == sec.getId();
            case PROMOTION:
                return promo != null && fee.getPromotion() != null && fee.getPromotion().getId() == promo.getId();
            case OPTION:
                return opt != null && fee.getOption() != null && fee.getOption().getId() == opt.getId();
            case CLASSE:
                return cl != null && fee.getClasse() != null && fee.getClasse().getId() == cl.getId();
            default:
                return false;
        }
    }

    public void save(Fee fee) {
        if (fee == null) return;
        if (fee.getId() == 0) {
            int maxId = storage.stream().mapToInt(Fee::getId).max().orElse(0);
            fee.setId(maxId + 1);
            storage.add(fee);
        } else {
            for (int i = 0; i < storage.size(); i++) {
                if (storage.get(i).getId() == fee.getId()) {
                    storage.set(i, fee);
                    return;
                }
            }
        }
    }

    public void toggleActive(int feeId) {
        Fee f = findById(feeId);
        if (f != null) {
            f.setActive(!f.isActive());
        }
    }
}

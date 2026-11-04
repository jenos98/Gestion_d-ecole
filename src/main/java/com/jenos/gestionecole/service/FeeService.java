package com.jenos.gestionecole.service;

import com.jenos.gestionecole.model.Fee;
import com.jenos.gestionecole.model.Student;
import com.jenos.gestionecole.repository.*;

import java.util.List;

/**
 * FeeService — Logique métier et validation de la grille tarifaire.
 *
 * RÈGLE ABSOLUE : Les montants USD et CDF sont traités séparément,
 * JAMAIS additionnés ni convertis implicitement.
 */
public class FeeService {

    private static FeeService instance;
    private final FeeRepository feeRepository;

    public static class FeeTotals {
        public double totalUSD = 0.0;
        public double totalCDF = 0.0;
    }

    private FeeService() {
        SectionRepository secRepo = new SectionRepository();
        PromotionRepository promoRepo = new PromotionRepository(secRepo);
        OptionRepository optRepo = new OptionRepository(promoRepo);
        ClasseRepository classeRepo = new ClasseRepository(optRepo);
        this.feeRepository = new FeeRepository(secRepo, promoRepo, optRepo, classeRepo);
    }

    public static FeeService getInstance() {
        if (instance == null) {
            instance = new FeeService();
        }
        return instance;
    }

    public List<Fee> findAll() {
        return feeRepository.findAll();
    }

    public List<Fee> findAllActive() {
        return feeRepository.findAllActive();
    }

    public Fee findById(int id) {
        return feeRepository.findById(id);
    }

    public List<Fee> getFeesForStudent(Student student) {
        return feeRepository.findByStudent(student);
    }

    public void save(Fee fee) throws IllegalArgumentException {
        if (fee == null) {
            throw new IllegalArgumentException("Le frais scolaire ne peut pas être null.");
        }

        if (fee.getName() == null || fee.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Le libellé du frais scolaire est obligatoire.");
        }

        if (fee.getAmount() <= 0) {
            throw new IllegalArgumentException("Le montant du frais scolaire doit être strictement supérieur à zéro.");
        }

        if (fee.getCurrency() == null || (!"USD".equalsIgnoreCase(fee.getCurrency()) && !"CDF".equalsIgnoreCase(fee.getCurrency()))) {
            throw new IllegalArgumentException("La devise doit obligatoirement être USD ou CDF.");
        }

        if (fee.getScopeLevel() != null) {
            switch (fee.getScopeLevel()) {
                case SECTION -> {
                    if (fee.getSection() == null) throw new IllegalArgumentException("Veuillez sélectionner la section concernée.");
                }
                case PROMOTION -> {
                    if (fee.getPromotion() == null) throw new IllegalArgumentException("Veuillez sélectionner la promotion concernée.");
                }
                case OPTION -> {
                    if (fee.getOption() == null) throw new IllegalArgumentException("Veuillez sélectionner l'option d'étude concernée.");
                }
                case CLASSE -> {
                    if (fee.getClasse() == null) throw new IllegalArgumentException("Veuillez sélectionner la classe concernée.");
                }
                case GLOBAL -> {}
            }
        }

        feeRepository.save(fee);
    }

    public void toggleActive(int feeId) {
        feeRepository.toggleActive(feeId);
    }

    /**
     * Calcule la somme des frais obligatoires exigibles pour un élève, séparés par devise.
     */
    public FeeTotals calculateStudentDueTotals(Student student) {
        FeeTotals totals = new FeeTotals();
        List<Fee> fees = getFeesForStudent(student);
        for (Fee f : fees) {
            if (f.isMandatory()) {
                if ("USD".equalsIgnoreCase(f.getCurrency())) {
                    totals.totalUSD += f.getAmount();
                } else if ("CDF".equalsIgnoreCase(f.getCurrency())) {
                    totals.totalCDF += f.getAmount();
                }
            }
        }
        return totals;
    }
}

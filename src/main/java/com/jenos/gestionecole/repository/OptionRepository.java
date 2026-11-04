package com.jenos.gestionecole.repository;

import com.jenos.gestionecole.model.AcademicOption;
import com.jenos.gestionecole.model.Promotion;

import java.util.ArrayList;
import java.util.List;

/**
 * OptionRepository — Accès aux données des filières / options d'étude.
 * Dépend strictement d'une Promotion.
 */
public class OptionRepository {

    private final List<AcademicOption> options = new ArrayList<>();

    public OptionRepository(PromotionRepository promoRepo) {
        initDefaultOptions(promoRepo);
    }

    private void initDefaultOptions(PromotionRepository promoRepo) {
        Promotion p6 = promoRepo.findById(1); // 6e Secondaire
        Promotion p5 = promoRepo.findById(2); // 5e Secondaire

        if (p6 != null) {
            options.add(new AcademicOption(1, "Scientifique", p6));
            options.add(new AcademicOption(2, "Commerciale", p6));
            options.add(new AcademicOption(3, "Littéraire", p6));
            options.add(new AcademicOption(4, "Pédagogie Générale", p6));
        }

        if (p5 != null) {
            options.add(new AcademicOption(5, "Scientifique", p5));
            options.add(new AcademicOption(6, "Commerciale", p5));
            options.add(new AcademicOption(7, "Littéraire", p5));
        }
    }

    public List<AcademicOption> findByPromotion(int promotionId) {
        List<AcademicOption> result = new ArrayList<>();
        for (AcademicOption opt : options) {
            if (opt.getPromotion() != null && opt.getPromotion().getId() == promotionId && opt.isActive()) {
                result.add(opt);
            }
        }
        return result;
    }

    public List<AcademicOption> findAll() {
        return new ArrayList<>(options);
    }

    public AcademicOption findById(int id) {
        for (AcademicOption opt : options) {
            if (opt.getId() == id) return opt;
        }
        return null;
    }

    public void save(AcademicOption opt) {
        if (opt.getId() == 0) {
            opt.setId(options.size() + 1);
            options.add(opt);
        } else {
            for (int i = 0; i < options.size(); i++) {
                if (options.get(i).getId() == opt.getId()) {
                    options.set(i, opt);
                    return;
                }
            }
        }
    }
}

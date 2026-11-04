package com.jenos.gestionecole.repository;

import com.jenos.gestionecole.model.Promotion;
import com.jenos.gestionecole.model.Section;

import java.util.ArrayList;
import java.util.List;

/**
 * PromotionRepository — Accès aux données des promotions (1e, 2e... 6e).
 * Dépend strictement d'une Section.
 */
public class PromotionRepository {

    private final List<Promotion> promotions = new ArrayList<>();

    public PromotionRepository(SectionRepository sectionRepo) {
        initDefaultPromotions(sectionRepo);
    }

    private void initDefaultPromotions(SectionRepository sectionRepo) {
        Section secondaire = sectionRepo.findById(2);
        Section primaire = sectionRepo.findById(1);

        if (secondaire != null) {
            promotions.add(new Promotion(1, "6e Secondaire", secondaire));
            promotions.add(new Promotion(2, "5e Secondaire", secondaire));
            promotions.add(new Promotion(3, "4e Secondaire", secondaire));
            promotions.add(new Promotion(4, "3e Secondaire", secondaire));
            promotions.add(new Promotion(5, "2e Secondaire", secondaire));
            promotions.add(new Promotion(6, "1e Secondaire", secondaire));
        }

        if (primaire != null) {
            promotions.add(new Promotion(7, "6e Primaire", primaire));
            promotions.add(new Promotion(8, "5e Primaire", primaire));
        }
    }

    public List<Promotion> findBySection(int sectionId) {
        List<Promotion> result = new ArrayList<>();
        for (Promotion p : promotions) {
            if (p.getSection() != null && p.getSection().getId() == sectionId) {
                result.add(p);
            }
        }
        return result;
    }

    public Promotion findById(int id) {
        for (Promotion p : promotions) {
            if (p.getId() == id) return p;
        }
        return null;
    }

    public List<Promotion> findAll() {
        return new ArrayList<>(promotions);
    }

    public void save(Promotion p) {
        if (p.getId() == 0) {
            p.setId(promotions.size() + 1);
            promotions.add(p);
        } else {
            for (int i = 0; i < promotions.size(); i++) {
                if (promotions.get(i).getId() == p.getId()) {
                    promotions.set(i, p);
                    return;
                }
            }
        }
    }
}

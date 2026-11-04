package com.jenos.gestionecole.repository;

import com.jenos.gestionecole.model.AcademicOption;
import com.jenos.gestionecole.model.Classe;
import com.jenos.gestionecole.model.Promotion;

import java.util.ArrayList;
import java.util.List;

/**
 * ClasseRepository — Accès aux données des classes.
 * Dépend strictement d'une Option (et par transitivité de Promotion et Section).
 */
public class ClasseRepository {

    private final List<Classe> classes = new ArrayList<>();

    public ClasseRepository(OptionRepository optionRepo) {
        this(optionRepo, null);
    }

    public ClasseRepository(OptionRepository optionRepo, PromotionRepository promoRepo) {
        initDefaultClasses(optionRepo, promoRepo);
    }

    private void initDefaultClasses(OptionRepository optionRepo, PromotionRepository promoRepo) {
        AcademicOption optSc5 = optionRepo != null ? optionRepo.findById(5) : null;
        AcademicOption optComm5 = optionRepo != null ? optionRepo.findById(6) : null;
        AcademicOption optSc6 = optionRepo != null ? optionRepo.findById(1) : null;

        if (optSc5 != null) {
            classes.add(new Classe(1, "5MP-A", optSc5));
            classes.add(new Classe(2, "5MP-B", optSc5));
            classes.add(new Classe(3, "5BIO-A", optSc5));
        }

        if (optComm5 != null) {
            classes.add(new Classe(4, "5COMM-A", optComm5));
            classes.add(new Classe(5, "5COMM-B", optComm5));
        }

        if (optSc6 != null) {
            classes.add(new Classe(6, "6MP-A", optSc6));
            classes.add(new Classe(7, "6MP-B", optSc6));
            classes.add(new Classe(8, "6BIO-A", optSc6));
        }

        // Classes pour promotions sans options (Primaire)
        if (promoRepo != null) {
            Promotion pPrim6 = promoRepo.findById(7); // 6e Primaire
            Promotion pPrim5 = promoRepo.findById(8); // 5e Primaire
            if (pPrim6 != null) {
                classes.add(new Classe(9, "6e Primaire A", pPrim6));
                classes.add(new Classe(10, "6e Primaire B", pPrim6));
            }
            if (pPrim5 != null) {
                classes.add(new Classe(11, "5e Primaire A", pPrim5));
                classes.add(new Classe(12, "5e Primaire B", pPrim5));
            }
        }
    }

    public List<Classe> findByOption(int optionId) {
        List<Classe> result = new ArrayList<>();
        for (Classe c : classes) {
            if (c.getOption() != null && c.getOption().getId() == optionId && c.isActive()) {
                result.add(c);
            }
        }
        return result;
    }

    public List<Classe> findByPromotion(int promotionId) {
        List<Classe> result = new ArrayList<>();
        for (Classe c : classes) {
            if (c.getPromotion() != null && c.getPromotion().getId() == promotionId && c.isActive()) {
                result.add(c);
            }
        }
        return result;
    }

    public List<Classe> findBySection(int sectionId) {
        List<Classe> result = new ArrayList<>();
        for (Classe c : classes) {
            if (c.getSection() != null && c.getSection().getId() == sectionId && c.isActive()) {
                result.add(c);
            }
        }
        return result;
    }

    public Classe findById(int id) {
        for (Classe c : classes) {
            if (c.getId() == id) return c;
        }
        return null;
    }

    public List<Classe> findAll() {
        return new ArrayList<>(classes);
    }

    public void save(Classe c) {
        if (c.getId() == 0) {
            c.setId(classes.size() + 1);
            classes.add(c);
        } else {
            for (int i = 0; i < classes.size(); i++) {
                if (classes.get(i).getId() == c.getId()) {
                    classes.set(i, c);
                    return;
                }
            }
        }
    }
}

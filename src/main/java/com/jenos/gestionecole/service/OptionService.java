package com.jenos.gestionecole.service;

import com.jenos.gestionecole.model.AcademicOption;
import com.jenos.gestionecole.model.Promotion;
import com.jenos.gestionecole.model.Section;

import java.util.ArrayList;
import java.util.List;

/**
 * OptionService — Gestion complète des options d'étude.
 * Permet l'ajout, la modification, et l'activation/désactivation des options.
 */
public class OptionService {

    private static OptionService instance;
    private final List<AcademicOption> options = new ArrayList<>();

    private OptionService() {
        initDefaultOptions();
    }

    public static OptionService getInstance() {
        if (instance == null) {
            instance = new OptionService();
        }
        return instance;
    }

    private void initDefaultOptions() {
        Section secondaire = new Section(2, "Secondaire");
        Promotion promo6 = new Promotion(1, "6e", secondaire);
        Promotion promo5 = new Promotion(2, "5e", secondaire);

        options.add(new AcademicOption(1, "Scientifique", promo6));
        options.add(new AcademicOption(2, "Commerciale", promo6));
        options.add(new AcademicOption(3, "Littéraire", promo6));
        options.add(new AcademicOption(4, "Pédagogie Générale", promo6));
        options.add(new AcademicOption(5, "Scientifique", promo5));
        options.add(new AcademicOption(6, "Commerciale", promo5));
    }

    public List<AcademicOption> findAll() {
        return new ArrayList<>(options);
    }

    public List<AcademicOption> findByPromotion(Promotion promotion) {
        if (promotion == null) return findAll();
        List<AcademicOption> filtered = new ArrayList<>();
        for (AcademicOption opt : options) {
            if (opt.getPromotion() != null && opt.getPromotion().getId() == promotion.getId()) {
                filtered.add(opt);
            }
        }
        return filtered;
    }

    public void save(AcademicOption option) {
        if (option.getName() == null || option.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom de l'option est obligatoire.");
        }
        if (option.getPromotion() == null) {
            throw new IllegalArgumentException("La promotion de rattachement est obligatoire.");
        }

        if (option.getId() == 0) {
            option.setId(options.size() + 1);
            options.add(option);
        } else {
            for (int i = 0; i < options.size(); i++) {
                if (options.get(i).getId() == option.getId()) {
                    options.set(i, option);
                    return;
                }
            }
        }
    }

    public void toggleActive(int optionId) {
        for (AcademicOption opt : options) {
            if (opt.getId() == optionId) {
                opt.setActive(!opt.isActive());
                return;
            }
        }
    }
}

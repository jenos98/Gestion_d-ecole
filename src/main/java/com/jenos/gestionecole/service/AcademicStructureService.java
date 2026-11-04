package com.jenos.gestionecole.service;

import com.jenos.gestionecole.model.AcademicOption;
import com.jenos.gestionecole.model.Classe;
import com.jenos.gestionecole.model.Promotion;
import com.jenos.gestionecole.model.Section;
import com.jenos.gestionecole.repository.*;

import java.util.ArrayList;
import java.util.List;

/**
 * AcademicStructureService — Fournisseur de la hiérarchie académique.
 *
 * Architecture :
 * AcademicStructureService
 *        ↓
 * SectionRepository / PromotionRepository / OptionRepository / ClasseRepository
 *        ↓
 * Database
 *
 * RÈGLE ABSOLUE : Section → Promotion → Option → Classe
 */
public class AcademicStructureService {

    private static AcademicStructureService instance;

    private final SectionRepository sectionRepository;
    private final PromotionRepository promotionRepository;
    private final OptionRepository optionRepository;
    private final ClasseRepository classeRepository;

    private AcademicStructureService() {
        this.sectionRepository = new SectionRepository();
        this.promotionRepository = new PromotionRepository(sectionRepository);
        this.optionRepository = new OptionRepository(promotionRepository);
        this.classeRepository = new ClasseRepository(optionRepository, promotionRepository);
    }

    public static AcademicStructureService getInstance() {
        if (instance == null) {
            instance = new AcademicStructureService();
        }
        return instance;
    }

    // ─────────────────────────────────────────────────────────────
    // SECTIONS
    // ─────────────────────────────────────────────────────────────

    public List<Section> findAllSections() {
        return sectionRepository.findAllActive();
    }

    public Section findSectionById(int id) {
        return sectionRepository.findById(id);
    }

    // ─────────────────────────────────────────────────────────────
    // PROMOTIONS (dépendent d'une Section)
    // ─────────────────────────────────────────────────────────────

    public List<Promotion> findPromotionsBySection(Section section) {
        if (section == null) return new ArrayList<>();
        return promotionRepository.findBySection(section.getId());
    }

    public Promotion findPromotionById(int id) {
        return promotionRepository.findById(id);
    }

    // ─────────────────────────────────────────────────────────────
    // OPTIONS (dépendent d'une Promotion)
    // ─────────────────────────────────────────────────────────────

    public List<AcademicOption> findOptionsByPromotion(Promotion promotion) {
        if (promotion == null) return new ArrayList<>();
        return optionRepository.findByPromotion(promotion.getId());
    }

    public boolean promotionHasOptions(Promotion promotion) {
        return !findOptionsByPromotion(promotion).isEmpty();
    }

    public AcademicOption findOptionById(int id) {
        return optionRepository.findById(id);
    }

    // ─────────────────────────────────────────────────────────────
    // CLASSES (dépendent d'une Option)
    // ─────────────────────────────────────────────────────────────

    public List<Classe> findClassesByOption(AcademicOption option) {
        if (option == null) return new ArrayList<>();
        return classeRepository.findByOption(option.getId());
    }

    public List<Classe> findClassesByPromotion(Promotion promotion) {
        if (promotion == null) return new ArrayList<>();
        return classeRepository.findByPromotion(promotion.getId());
    }

    public List<Classe> findClassesBySection(Section section) {
        if (section == null) return new ArrayList<>();
        return classeRepository.findBySection(section.getId());
    }

    public Classe findClasseById(int id) {
        return classeRepository.findById(id);
    }

    public List<Promotion> findAllPromotions() {
        return promotionRepository.findAll();
    }

    public List<AcademicOption> findAllOptions() {
        return optionRepository.findAll();
    }

    public List<Classe> findAllClasses() {
        return classeRepository.findAll();
    }

    public void saveSection(Section s) {
        if (s == null) return;
        sectionRepository.save(s);
        AuditService.getInstance().logAction("admin", "ENREGISTREMENT_SECTION", "Section", null, s.getName());
    }

    public void savePromotion(Promotion p) {
        if (p == null) return;
        promotionRepository.save(p);
        AuditService.getInstance().logAction("admin", "ENREGISTREMENT_PROMOTION", "Promotion", null, p.getName());
    }

    public void saveOption(AcademicOption opt) {
        if (opt == null) return;
        optionRepository.save(opt);
        AuditService.getInstance().logAction("admin", "ENREGISTREMENT_OPTION", "AcademicOption", null, opt.getName());
    }

    public void saveClasse(Classe c) {
        if (c == null) return;
        classeRepository.save(c);
        AuditService.getInstance().logAction("admin", "ENREGISTREMENT_CLASSE", "Classe", null, c.getName());
    }

    // ─────────────────────────────────────────────────────────────
    // ANNÉES SCOLAIRES
    // ─────────────────────────────────────────────────────────────

    public List<String> findAllAcademicYears() {
        List<String> years = new ArrayList<>();
        years.add("2025-2026");
        years.add("2024-2025");
        years.add("2023-2024");
        return years;
    }
}

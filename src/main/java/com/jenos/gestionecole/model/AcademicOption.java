package com.jenos.gestionecole.model;

/**
 * AcademicOption — Filière d'étude dans une promotion.
 * Exemples : Scientifique, Commerciale, Littéraire, Math-Physique, Gestion.
 *
 * Note : Cette classe s'appelle AcademicOption (et non Option)
 * pour éviter la collision avec java.util.Optional.
 *
 * Hiérarchie complète :
 *   Section → Promotion → AcademicOption → Classe
 */
public class AcademicOption {

    private int id;
    private String name;
    private String code;
    private String description;
    private Promotion promotion;
    private boolean active;

    public AcademicOption() {}

    public AcademicOption(int id, String name, Promotion promotion) {
        this.id = id;
        this.name = name;
        this.code = (name != null && name.length() >= 3) ? name.substring(0, 3).toUpperCase() : "OPT";
        this.promotion = promotion;
        this.active = true;
    }

    public AcademicOption(int id, String name, String code, Promotion promotion, String description) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.promotion = promotion;
        this.description = description;
        this.active = true;
    }

    public int getId()               { return id; }
    public String getName()          { return name; }
    public String getCode()          { return code != null ? code : ""; }
    public String getDescription()   { return description != null ? description : ""; }
    public Promotion getPromotion()  { return promotion; }
    public boolean isActive()        { return active; }

    public void setId(int id)                     { this.id = id; }
    public void setName(String name)               { this.name = name; }
    public void setCode(String code)               { this.code = code; }
    public void setDescription(String description) { this.description = description; }
    public void setPromotion(Promotion promotion) { this.promotion = promotion; }
    public void setActive(boolean active)         { this.active = active; }

    /** Retourne le chemin complet : Section / Promotion / Option */
    public String getFullPath() {
        if (promotion != null && promotion.getSection() != null) {
            return promotion.getSection().getName()
                + " › " + promotion.getName()
                + " › " + name;
        }
        return name;
    }

    @Override
    public String toString() { return name; }
}

package com.jenos.gestionecole.model;

/**
 * Classe — Groupe d'élèves dans une option.
 * Exemple : Math-Physique A, Gestion B.
 * Dépend d'une AcademicOption.
 *
 * Hiérarchie : Section → Promotion → AcademicOption → Classe
 */
public class Classe {

    private int id;
    private String name;
    private String code;
    private String titulaire;
    private int capacity;
    private AcademicOption option;
    private Promotion promotion;
    private boolean active;

    public Classe() {
        this.capacity = 45;
        this.active = true;
    }

    public Classe(int id, String name, AcademicOption option) {
        this.id = id;
        this.name = name;
        this.code = (name != null && name.length() >= 3) ? name.substring(0, 3).toUpperCase() : "CLS";
        this.option = option;
        this.promotion = option != null ? option.getPromotion() : null;
        this.capacity = 45;
        this.active = true;
    }

    public Classe(int id, String name, Promotion promotion) {
        this.id = id;
        this.name = name;
        this.code = (name != null && name.length() >= 3) ? name.substring(0, 3).toUpperCase() : "CLS";
        this.option = null;
        this.promotion = promotion;
        this.capacity = 45;
        this.active = true;
    }

    public Classe(int id, String name, String code, AcademicOption option, Promotion promotion, String titulaire, int capacity) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.option = option;
        this.promotion = promotion != null ? promotion : (option != null ? option.getPromotion() : null);
        this.titulaire = titulaire;
        this.capacity = capacity > 0 ? capacity : 45;
        this.active = true;
    }

    public int getId()                 { return id; }
    public String getName()            { return name; }
    public String getCode()            { return code != null ? code : ""; }
    public String getTitulaire()       { return titulaire != null ? titulaire : "Non assigné"; }
    public int getCapacity()           { return capacity; }
    public AcademicOption getOption()  { return option; }
    public boolean isActive()          { return active; }

    public void setId(int id)                       { this.id = id; }
    public void setName(String name)                 { this.name = name; }
    public void setCode(String code)                 { this.code = code; }
    public void setTitulaire(String titulaire)       { this.titulaire = titulaire; }
    public void setCapacity(int capacity)           { this.capacity = capacity; }
    public void setOption(AcademicOption option)    {
        this.option = option;
        if (option != null) this.promotion = option.getPromotion();
    }
    public void setPromotion(Promotion promotion)   { this.promotion = promotion; }
    public void setActive(boolean active)           { this.active = active; }

    public Promotion getPromotion() {
        if (promotion != null) return promotion;
        return option != null ? option.getPromotion() : null;
    }

    public Section getSection() {
        Promotion p = getPromotion();
        return p != null ? p.getSection() : null;
    }

    public String getFullPath() {
        StringBuilder sb = new StringBuilder();
        if (getSection() != null) sb.append(getSection().getName()).append(" › ");
        if (getPromotion() != null) sb.append(getPromotion().getName()).append(" › ");
        if (getOption() != null) sb.append(getOption().getName()).append(" › ");
        sb.append(name);
        return sb.toString();
    }

    @Override
    public String toString() { return name; }
}

package com.jenos.gestionecole.model;

/**
 * Promotion — Année d'étude dans une section (ex: 6e, 5e, 4e).
 * Dépend d'une Section.
 */
public class Promotion {

    private int id;
    private String name;
    private String code;
    private String description;
    private Section section;
    private boolean active;

    public Promotion() {}

    public Promotion(int id, String name, Section section) {
        this.id = id;
        this.name = name;
        this.code = (name != null && name.length() >= 2) ? name.substring(0, 2).toUpperCase() : "PR";
        this.section = section;
        this.active = true;
    }

    public Promotion(int id, String name, String code, Section section, String description) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.section = section;
        this.description = description;
        this.active = true;
    }

    public int getId()           { return id; }
    public String getName()      { return name; }
    public String getCode()      { return code != null ? code : ""; }
    public String getDescription() { return description != null ? description : ""; }
    public Section getSection()  { return section; }
    public boolean isActive()    { return active; }

    public void setId(int id)                 { this.id = id; }
    public void setName(String name)           { this.name = name; }
    public void setCode(String code)           { this.code = code; }
    public void setDescription(String description) { this.description = description; }
    public void setSection(Section section)   { this.section = section; }
    public void setActive(boolean active)     { this.active = active; }

    @Override
    public String toString() { return name; }
}

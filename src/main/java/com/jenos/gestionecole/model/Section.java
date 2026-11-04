package com.jenos.gestionecole.model;

/**
 * Section — Niveau d'étude (ex: Primaire, Secondaire).
 */
public class Section {

    private int id;
    private String name;
    private String code;
    private String description;
    private boolean active;

    public Section() {}

    public Section(int id, String name) {
        this.id = id;
        this.name = name;
        this.code = (name != null && name.length() >= 3) ? name.substring(0, 3).toUpperCase() : "SEC";
        this.active = true;
    }

    public Section(int id, String name, String code, String description) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.description = description;
        this.active = true;
    }

    public int getId()        { return id; }
    public String getName()   { return name; }
    public String getCode()   { return code != null ? code : ""; }
    public String getDescription() { return description != null ? description : ""; }
    public boolean isActive() { return active; }

    public void setId(int id)           { this.id = id; }
    public void setName(String name)     { this.name = name; }
    public void setCode(String code)     { this.code = code; }
    public void setDescription(String description) { this.description = description; }
    public void setActive(boolean active){ this.active = active; }

    @Override
    public String toString() { return name; }
}

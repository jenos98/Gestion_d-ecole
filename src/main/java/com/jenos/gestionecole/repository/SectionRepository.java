package com.jenos.gestionecole.repository;

import com.jenos.gestionecole.model.Section;

import java.util.ArrayList;
import java.util.List;

/**
 * SectionRepository — Accès aux données des sections (Primaire, Secondaire, etc.).
 */
public class SectionRepository {

    private final List<Section> sections = new ArrayList<>();

    public SectionRepository() {
        initDefaultSections();
    }

    private void initDefaultSections() {
        sections.add(new Section(1, "Primaire"));
        sections.add(new Section(2, "Secondaire"));
    }

    public List<Section> findAllActive() {
        return new ArrayList<>(sections);
    }

    public List<Section> findAll() {
        return new ArrayList<>(sections);
    }

    public Section findById(int id) {
        for (Section s : sections) {
            if (s.getId() == id) return s;
        }
        return null;
    }

    public void save(Section s) {
        if (s.getId() == 0) {
            s.setId(sections.size() + 1);
            sections.add(s);
        } else {
            for (int i = 0; i < sections.size(); i++) {
                if (sections.get(i).getId() == s.getId()) {
                    sections.set(i, s);
                    return;
                }
            }
        }
    }
}

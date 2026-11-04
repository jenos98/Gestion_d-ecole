package com.jenos.gestionecole.repository;

import com.jenos.gestionecole.model.Staff;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * StaffRepository — Accès aux données et persistance des membres du personnel.
 * Aucune suppression physique n'est opérée (statut non destructif).
 */
public class StaffRepository {

    private final List<Staff> storage = new ArrayList<>();

    public StaffRepository() {
        initDefaultStaff();
    }

    private void initDefaultStaff() {
        Staff s1 = new Staff(1, "STF-2025-0001", "Mukendi", "Tshilumba", "Alain",
                "Enseignant", "Professeur de Mathématiques", "Titulaire 6e Sc", 450.0, "USD");
        s1.setGenre("M");
        s1.setQualification("Licencié en Sciences Pédagogiques");
        s1.setDateEmbauche(LocalDate.of(2021, 9, 1));
        s1.setTelephone("+243 81 123 4567");
        s1.setEmail("alain.mukendi@ecolejenos.cd");

        Staff s2 = new Staff(2, "STF-2025-0002", "Kabange", "Ilunga", "Marie",
                "Direction", "Directrice des Études", "Bureau de Direction", 600.0, "USD");
        s2.setGenre("F");
        s2.setQualification("Master en Gestion Scolaire");
        s2.setDateEmbauche(LocalDate.of(2019, 8, 15));
        s2.setTelephone("+243 82 234 5678");
        s2.setEmail("marie.kabange@ecolejenos.cd");

        Staff s3 = new Staff(3, "STF-2025-0003", "Kashama", "Mwamba", "David",
                "Administratif", "Comptable", "Caisse Principale", 400.0, "USD");
        s3.setGenre("M");
        s3.setQualification("Gradué en Sciences Commerciales & Financières");
        s3.setDateEmbauche(LocalDate.of(2022, 10, 1));
        s3.setTelephone("+243 99 345 6789");
        s3.setEmail("david.kashama@ecolejenos.cd");

        Staff s4 = new Staff(4, "STF-2025-0004", "Nyemba", "Biduaya", "Solange",
                "Administratif", "Secrétaire", "Accueil & Inscriptions", 300.0, "USD");
        s4.setGenre("F");
        s4.setQualification("Diplôme d'État Commerciale");
        s4.setDateEmbauche(LocalDate.of(2023, 2, 1));
        s4.setTelephone("+243 90 456 7890");
        s4.setEmail("solange.nyemba@ecolejenos.cd");

        Staff s5 = new Staff(5, "STF-2025-0005", "Ilunga", "Tshite", "Pierre",
                "Ouvrier", "Technicien de Maintenance", "Atelier Général", 180.0, "USD");
        s5.setGenre("M");
        s5.setQualification("Brevet Professionnel Électromécanique");
        s5.setDateEmbauche(LocalDate.of(2022, 5, 10));
        s5.setTelephone("+243 81 567 8901");

        Staff s6 = new Staff(6, "STF-2025-0006", "Tshibangu", "Kalala", "Jeanne",
                "Enseignant", "Professeur de Français", "Titulaire 5e Littéraire", 420.0, "USD");
        s6.setGenre("F");
        s6.setQualification("Licenciée en Lettres & Linguistique");
        s6.setDateEmbauche(LocalDate.of(2020, 9, 1));
        s6.setTelephone("+243 82 678 9012");
        s6.setEmail("jeanne.tshibangu@ecolejenos.cd");

        storage.add(s1);
        storage.add(s2);
        storage.add(s3);
        storage.add(s4);
        storage.add(s5);
        storage.add(s6);
    }

    public List<Staff> findAll() {
        return new ArrayList<>(storage);
    }

    public Staff findById(int id) {
        return storage.stream().filter(s -> s.getId() == id).findFirst().orElse(null);
    }

    public Staff findByMatricule(String matricule) {
        if (matricule == null) return null;
        return storage.stream().filter(s -> matricule.equalsIgnoreCase(s.getMatricule())).findFirst().orElse(null);
    }

    public List<Staff> search(String keyword, String categorie, String fonction, Staff.StaffStatus statut) {
        return storage.stream().filter(s -> {
            if (categorie != null && !categorie.isBlank() && !"Toutes".equalsIgnoreCase(categorie)) {
                if (s.getCategorie() == null || !s.getCategorie().equalsIgnoreCase(categorie)) return false;
            }
            if (fonction != null && !fonction.isBlank() && !"Toutes".equalsIgnoreCase(fonction)) {
                if (s.getFonction() == null || !s.getFonction().toLowerCase().contains(fonction.toLowerCase())) return false;
            }
            if (statut != null) {
                if (s.getStatut() != statut) return false;
            }
            if (keyword != null && !keyword.isBlank()) {
                String kw = keyword.toLowerCase().trim();
                boolean match = (s.getMatricule() != null && s.getMatricule().toLowerCase().contains(kw))
                        || (s.getNom() != null && s.getNom().toLowerCase().contains(kw))
                        || (s.getPostnom() != null && s.getPostnom().toLowerCase().contains(kw))
                        || (s.getPrenom() != null && s.getPrenom().toLowerCase().contains(kw))
                        || (s.getTelephone() != null && s.getTelephone().contains(kw));
                if (!match) return false;
            }
            return true;
        }).collect(Collectors.toList());
    }

    public void save(Staff staff) {
        if (staff == null) return;
        if (staff.getId() == 0) {
            int maxId = storage.stream().mapToInt(Staff::getId).max().orElse(0);
            staff.setId(maxId + 1);
            if (staff.getMatricule() == null || staff.getMatricule().isBlank()) {
                staff.setMatricule(generateMatricule());
            }
            storage.add(staff);
        } else {
            for (int i = 0; i < storage.size(); i++) {
                if (storage.get(i).getId() == staff.getId()) {
                    storage.set(i, staff);
                    return;
                }
            }
        }
    }

    public void updateStatus(int staffId, Staff.StaffStatus status) {
        Staff s = findById(staffId);
        if (s != null) {
            s.setStatut(status);
        }
    }

    public String generateMatricule() {
        int year = LocalDate.now().getYear();
        int seq = storage.size() + 1;
        return String.format("STF-%d-%04d", year, seq);
    }

    public int countTotal() {
        return storage.size();
    }

    /** Archive un membre du personnel (non destructif). */
    public boolean archive(int staffId) {
        Staff s = findById(staffId);
        if (s != null && s.getStatut() != Staff.StaffStatus.ARCHIVE) {
            s.setStatut(Staff.StaffStatus.ARCHIVE);
            return true;
        }
        return false;
    }

    /** Retourne uniquement les membres non archivés. */
    public List<Staff> findAllActive() {
        return storage.stream()
                .filter(s -> s.getStatut() != Staff.StaffStatus.ARCHIVE)
                .collect(Collectors.toList());
    }

    /** Retourne les départements (catégories) distincts. */
    public List<String> findAllDepartments() {
        return storage.stream()
                .map(Staff::getCategorie)
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
}

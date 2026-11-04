package com.jenos.gestionecole.model;

import java.time.LocalDate;

/**
 * Staff — Modèle pour un membre du personnel de l'établissement.
 *
 * RÈGLE ABSOLUE : Les montants USD et CDF sont traités séparément,
 * JAMAIS additionnés ni convertis implicitement.
 */
public class Staff {

    public enum StaffStatus {
        ACTIF("Actif"),
        INACTIF("Inactif"),
        ARCHIVE("Archivé"),
        EN_CONGE("En Congé"),
        SUSPENDU("Suspendu"),
        DEMISSIONNAIRE("Démissionnaire"),
        RETRAITE("Retraité");

        private final String label;

        StaffStatus(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private int id;
    private String matricule;
    private String nom;
    private String postnom;
    private String prenom;
    private String genre = "M"; // "M" ou "F"
    private String categorie;   // Enseignant, Administratif, Ouvrier, Direction...
    private String fonction;    // Professeur, Secrétaire, Comptable, Chauffeur...
    private String poste;       // Titulaire 6e Sc, Caissière Principale...
    private StaffStatus statut = StaffStatus.ACTIF;
    private String telephone;
    private String email;
    private String adresse;
    private String qualification; // Licence en Pédagogie, Graduat en Gestion...
    private LocalDate dateEmbauche = LocalDate.now();
    private String contractType = "CDI"; // CDI, CDD, Temporaire, Vacataire...
    private String serviceAffectation;   // Service ou département d'affectation

    // Rémunération contractuelle
    private double salaireBase = 0.0;
    private String deviseSalaire = "USD"; // "USD" ou "CDF"

    public Staff() {}

    public Staff(int id, String matricule, String nom, String postnom, String prenom,
                 String categorie, String fonction, String poste, double salaireBase, String deviseSalaire) {
        this.id = id;
        this.matricule = matricule;
        this.nom = nom;
        this.postnom = postnom;
        this.prenom = prenom;
        this.categorie = categorie;
        this.fonction = fonction;
        this.poste = poste;
        this.statut = StaffStatus.ACTIF;
        this.salaireBase = salaireBase;
        this.deviseSalaire = deviseSalaire;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getMatricule() { return matricule; }
    public void setMatricule(String matricule) { this.matricule = matricule; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPostnom() { return postnom; }
    public void setPostnom(String postnom) { this.postnom = postnom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }

    public String getFonction() { return fonction; }
    public void setFonction(String fonction) { this.fonction = fonction; }

    public String getPoste() { return poste; }
    public void setPoste(String poste) { this.poste = poste; }

    public String getRole() { return fonction != null ? fonction : (poste != null ? poste : "Personnel"); }

    /** Département ou service d'affectation (repli sur catégorie si non spécifié). */
    public String getDepartment() {
        return (serviceAffectation != null && !serviceAffectation.isBlank()) ? serviceAffectation : categorie;
    }

    public String getServiceAffectation() { return serviceAffectation; }
    public void setServiceAffectation(String serviceAffectation) { this.serviceAffectation = serviceAffectation; }

    public String getContractType() { return contractType; }
    public void setContractType(String contractType) { this.contractType = contractType; }

    public StaffStatus getStatut() { return statut; }
    public void setStatut(StaffStatus statut) { this.statut = statut; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public String getQualification() { return qualification; }
    public void setQualification(String qualification) { this.qualification = qualification; }

    public LocalDate getDateEmbauche() { return dateEmbauche; }
    public void setDateEmbauche(LocalDate dateEmbauche) { this.dateEmbauche = dateEmbauche; }

    public double getSalaireBase() { return salaireBase; }
    public void setSalaireBase(double salaireBase) { this.salaireBase = salaireBase; }

    public String getDeviseSalaire() { return deviseSalaire; }
    public void setDeviseSalaire(String deviseSalaire) { this.deviseSalaire = deviseSalaire; }

    public String getFullName() {
        return (nom != null ? nom.toUpperCase() : "")
             + " " + (postnom != null ? postnom : "")
             + " " + (prenom != null ? prenom : "");
    }

    @Override
    public String toString() {
        return getFullName() + " (" + (fonction != null ? fonction : "Personnel") + ")";
    }
}

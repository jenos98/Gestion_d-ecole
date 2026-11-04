package com.jenos.gestionecole.model;

import java.time.LocalDate;

/**
 * Student — Modèle de l'élève inscrit.
 *
 * RÈGLE ARCHITECTURALE :
 * L'élève référence principalement sa Classe (qui résout en chaîne Option → Promotion → Section).
 * La suppression physique est formellement interdite : un élève désactivé a statut = INACTIF ou ARCHIVE.
 */
public class Student {

    // ── Identité ──────────────────────────────────────────────────
    private int id;
    private String nom;
    private String postnom;
    private String prenom;
    private String sexe;           // "M" ou "F"
    private LocalDate dateNaissance;
    private String lieuNaissance;
    private String nationalite = "Congolaise";

    // ── Contact Élève ─────────────────────────────────────────────
    private String telephone;
    private String email;
    private String adresse;

    // ── Responsable / Tuteur Légal ────────────────────────────────
    private String guardianName;
    private String guardianPhone;
    private String guardianRelation; // Père, Mère, Tuteur, Oncle...
    private String guardianAddress;

    // ── Matricule & Inscription ───────────────────────────────────
    private String matricule;
    private String anneeScolaire = "2024-2025";
    private LocalDate dateInscription = LocalDate.now();

    // ── Structure Scolaire ────────────────────────────────────────
    // La classe est le pivot qui relie Option -> Promotion -> Section
    private Classe classe;

    // Stockage de secours ou de transition pour le formulaire
    private Section section;
    private Promotion promotion;
    private AcademicOption option;

    // ── Statut (Non destructif) ───────────────────────────────────
    public enum StudentStatus {
        ACTIF("Actif"),
        INACTIF("Inactif"),
        ARCHIVE("Archivé"),
        TRANSFERE("Transféré"),
        DIPLOME("Diplômé");

        private final String label;

        StudentStatus(String label) {
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

    private StudentStatus statut = StudentStatus.ACTIF;

    public Student() {
        this.statut = StudentStatus.ACTIF;
        this.dateInscription = LocalDate.now();
    }

    public Student(int id, String matricule, String nom, String postnom, String prenom,
                   String sexe, Classe classe, String anneeScolaire) {
        this.id = id;
        this.matricule = matricule;
        this.nom = nom;
        this.postnom = postnom;
        this.prenom = prenom;
        this.sexe = sexe;
        this.classe = classe;
        this.anneeScolaire = anneeScolaire;
        this.statut = StudentStatus.ACTIF;
        this.dateInscription = LocalDate.now();
    }

    // ── Getters ───────────────────────────────────────────────────

    public int getId()                    { return id; }
    public String getNom()                { return nom; }
    public String getPostnom()            { return postnom; }
    public String getPrenom()             { return prenom; }
    public String getSexe()               { return sexe; }
    public LocalDate getDateNaissance()   { return dateNaissance; }
    public String getLieuNaissance()      { return lieuNaissance; }
    public String getNationalite()        { return nationalite; }
    public String getTelephone()          { return telephone; }
    public String getEmail()              { return email; }
    public String getAdresse()            { return adresse; }

    public String getGuardianName()       { return guardianName; }
    public String getGuardianPhone()      { return guardianPhone; }
    public String getGuardianRelation()   { return guardianRelation; }
    public String getGuardianAddress()    { return guardianAddress; }

    public String getMatricule()          { return matricule; }
    public String getAnneeScolaire()      { return anneeScolaire; }
    public LocalDate getDateInscription() { return dateInscription; }
    public Classe getClasse()             { return classe; }
    public StudentStatus getStatut()      { return statut; }

    /** Résolution de l'option (explicite ou via la classe) */
    public AcademicOption getOption() {
        if (option != null) return option;
        if (classe != null && classe.getOption() != null) {
            return classe.getOption();
        }
        return null;
    }

    /** Résolution de la promotion (explicite ou via l'option ou la classe) */
    public Promotion getPromotion() {
        if (promotion != null) return promotion;
        AcademicOption opt = getOption();
        if (opt != null && opt.getPromotion() != null) {
            return opt.getPromotion();
        }
        if (classe != null && classe.getPromotion() != null) {
            return classe.getPromotion();
        }
        return null;
    }

    /** Résolution de la section (explicite ou via la promotion ou la classe) */
    public Section getSection() {
        if (section != null) return section;
        Promotion promo = getPromotion();
        if (promo != null && promo.getSection() != null) {
            return promo.getSection();
        }
        if (classe != null && classe.getSection() != null) {
            return classe.getSection();
        }
        return null;
    }

    // ── Setters ───────────────────────────────────────────────────

    public void setId(int id)                         { this.id = id; }
    public void setNom(String nom)                     { this.nom = nom; }
    public void setPostnom(String postnom)             { this.postnom = postnom; }
    public void setPrenom(String prenom)               { this.prenom = prenom; }
    public void setSexe(String sexe)                   { this.sexe = sexe; }
    public void setDateNaissance(LocalDate d)         { this.dateNaissance = d; }
    public void setLieuNaissance(String lieu)         { this.lieuNaissance = lieu; }
    public void setNationalite(String nat)            { this.nationalite = nat; }
    public void setTelephone(String telephone)         { this.telephone = telephone; }
    public void setEmail(String email)                 { this.email = email; }
    public void setAdresse(String adresse)             { this.adresse = adresse; }

    public void setGuardianName(String guardianName)         { this.guardianName = guardianName; }
    public void setGuardianPhone(String guardianPhone)       { this.guardianPhone = guardianPhone; }
    public void setGuardianRelation(String guardianRelation) { this.guardianRelation = guardianRelation; }
    public void setGuardianAddress(String guardianAddress)   { this.guardianAddress = guardianAddress; }

    public void setMatricule(String matricule)         { this.matricule = matricule; }
    public void setAnneeScolaire(String annee)        { this.anneeScolaire = annee; }
    public void setDateInscription(LocalDate d)       { this.dateInscription = d; }
    public void setClasse(Classe classe)              { this.classe = classe; }
    public void setSection(Section section)           { this.section = section; }
    public void setPromotion(Promotion promotion)     { this.promotion = promotion; }
    public void setOption(AcademicOption option)      { this.option = option; }
    public void setStatut(StudentStatus statut)       { this.statut = statut; }

    // ── Utilitaires d'affichage ───────────────────────────────────

    public String getFullName() {
        return (nom != null ? nom.toUpperCase() : "")
             + " " + (postnom != null ? postnom : "")
             + " " + (prenom != null ? prenom : "");
    }

    public String getSectionName()   { return getSection()   != null ? getSection().getName()   : "—"; }
    public String getPromotionName() { return getPromotion() != null ? getPromotion().getName() : "—"; }
    public String getOptionName()    { return getOption()    != null ? getOption().getName()    : "—"; }
    public String getClassName()     { return classe         != null ? classe.getName()         : "—"; }

    @Override
    public String toString() {
        return getFullName() + " [" + matricule + "]";
    }
}

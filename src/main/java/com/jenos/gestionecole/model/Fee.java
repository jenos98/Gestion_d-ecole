package com.jenos.gestionecole.model;

import java.time.LocalDate;

/**
 * Fee — Type de frais scolaire et tarification hiérarchique.
 *
 * RÈGLE ABSOLUE : Les montants USD et CDF sont traités séparément,
 * JAMAIS additionnés ni convertis implicitement.
 */
public class Fee {

    public enum FeeCategory {
        INSCRIPTION("Frais d'Inscription"),
        REINSCRIPTION("Frais de Réinscription"),
        MINERVAL_T1("Minerval 1er Trimestre"),
        MINERVAL_T2("Minerval 2ème Trimestre"),
        MINERVAL_T3("Minerval 3ème Trimestre"),
        FRAIS_TECHNIQUES("Frais Techniques / Laboratoire"),
        EXAMEN_ETAT("Frais d'Examen d'État / TENASOSP"),
        AUTRE("Autres Frais Scolaires");

        private final String label;

        FeeCategory(String label) {
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

    public enum ScopeLevel {
        GLOBAL("Toute l'école (Global)"),
        SECTION("Par Section"),
        PROMOTION("Par Promotion"),
        OPTION("Par Option d'Étude"),
        CLASSE("Par Classe Spécifique");

        private final String label;

        ScopeLevel(String label) {
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
    private String name;
    private double amount;
    private String currency = "USD"; // "USD" ou "CDF"
    private boolean mandatory = true;
    private FeeCategory category = FeeCategory.MINERVAL_T1;
    private ScopeLevel scopeLevel = ScopeLevel.GLOBAL;

    // Rattachements hiérarchiques selon le ScopeLevel
    private Section section;
    private Promotion promotion;
    private AcademicOption option;
    private Classe classe;

    private String anneeScolaire = "2024-2025";
    private LocalDate dateEcheance;
    private boolean active = true;

    /**
     * Fréquence de paiement — détermine le nombre d'échéances générées
     * lors de l'affectation à un élève.
     */
    public enum PaymentFrequency {
        ANNUEL("Annuel", 1),
        TRIMESTRIEL("Trimestriel", 3),
        MENSUEL("Mensuel", 10);

        private final String label;
        private final int installmentCount;

        PaymentFrequency(String label, int installmentCount) {
            this.label = label;
            this.installmentCount = installmentCount;
        }

        public String getLabel() { return label; }
        public int getInstallmentCount() { return installmentCount; }

        @Override
        public String toString() { return label; }
    }

    private PaymentFrequency frequency = PaymentFrequency.ANNUEL;

    public Fee() {}

    public Fee(int id, String name, double amount, String currency, boolean mandatory) {
        this.id = id;
        this.name = name;
        this.amount = amount;
        this.currency = currency;
        this.mandatory = mandatory;
    }

    public Fee(int id, String name, double amount, String currency, boolean mandatory,
               FeeCategory category, ScopeLevel scopeLevel) {
        this.id = id;
        this.name = name;
        this.amount = amount;
        this.currency = currency;
        this.mandatory = mandatory;
        this.category = category;
        this.scopeLevel = scopeLevel;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public boolean isMandatory() { return mandatory; }
    public void setMandatory(boolean mandatory) { this.mandatory = mandatory; }

    public FeeCategory getCategory() { return category; }
    public void setCategory(FeeCategory category) { this.category = category; }

    public ScopeLevel getScopeLevel() { return scopeLevel; }
    public void setScopeLevel(ScopeLevel scopeLevel) { this.scopeLevel = scopeLevel; }

    public Section getSection() { return section; }
    public void setSection(Section section) { this.section = section; }

    public Promotion getPromotion() { return promotion; }
    public void setPromotion(Promotion promotion) { this.promotion = promotion; }

    public AcademicOption getOption() { return option; }
    public void setOption(AcademicOption option) { this.option = option; }

    public Classe getClasse() { return classe; }
    public void setClasse(Classe classe) { this.classe = classe; }

    public String getAnneeScolaire() { return anneeScolaire; }
    public void setAnneeScolaire(String anneeScolaire) { this.anneeScolaire = anneeScolaire; }

    public LocalDate getDateEcheance() { return dateEcheance; }
    public void setDateEcheance(LocalDate dateEcheance) { this.dateEcheance = dateEcheance; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public PaymentFrequency getFrequency() { return frequency; }
    public void setFrequency(PaymentFrequency frequency) { this.frequency = frequency; }

    /**
     * Retourne une description textuelle claire du niveau d'application.
     */
    public String getTargetDescription() {
        if (scopeLevel == null) return "Global";
        return switch (scopeLevel) {
            case GLOBAL -> "Global (Toute l'école)";
            case SECTION -> section != null ? "Section : " + section.getName() : "Section (Non définie)";
            case PROMOTION -> promotion != null ? "Promotion : " + promotion.getName() : "Promotion (Non définie)";
            case OPTION -> option != null ? "Option : " + option.getName() : "Option (Non définie)";
            case CLASSE -> classe != null ? "Classe : " + classe.getName() : "Classe (Non définie)";
        };
    }

    @Override
    public String toString() {
        return name + " (" + String.format("%.2f", amount) + " " + currency + ")";
    }
}

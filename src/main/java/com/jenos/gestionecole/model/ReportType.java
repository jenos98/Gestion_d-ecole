package com.jenos.gestionecole.model;

/**
 * ReportType — Types de rapports disponibles dans l'application.
 */
public enum ReportType {
    RAPPORT_JOURNALIER("Rapport Journalier de Caisse"),
    RAPPORT_MENSUEL("Rapport Mensuel d'Activité"),
    RAPPORT_ANNUEL("Rapport Annuel Consolidé"),
    ETAT_DE_CAISSE("État & Mouvements de Caisse"),
    RAPPORT_DEPENSES("Rapport Analytique des Dépenses"),
    RAPPORT_RECETTES("Rapport des Recettes"),
    RAPPORT_PAIEMENTS("Journal des Encaissements"),
    RAPPORT_PAR_CAISSIERE("Rapport par Caissière"),
    RAPPORT_PAR_CAISSE("Rapport par Caisse"),
    LISTE_APPEL_CLASSE("Liste d'Appel Officielle par Classe"),
    TABLEAU_EFFECTIFS("Tableau Synoptique des Effectifs"),
    RAPPORT_ELEVES("Situation Financière des Élèves"),
    RAPPORT_PAR_CLASSE("Rapport de Solvabilité par Classe"),
    RAPPORT_PAR_OPTION("Rapport Financier par Option d'Étude"),
    RAPPORT_PERSONNEL("Rapport des Règlements du Personnel"),
    RAPPORT_IMPAYES("Rapport des Impayés et Soldes Débiteurs"),
    RAPPORT_PERSONNALISE("Rapport Personnalisé Multicritères"),
    HISTORIQUE_RAPPORTS("Journal d'Audit des Rapports Émis");

    private final String label;

    ReportType(String label) {
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

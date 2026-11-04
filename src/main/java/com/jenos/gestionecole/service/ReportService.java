package com.jenos.gestionecole.service;

import com.jenos.gestionecole.model.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * ReportService — Génération et mise en forme des rapports imprimables.
 */
public class ReportService {

    private static ReportService instance;
    private ReportDocument lastGeneratedReport;

    private ReportService() {}

    public static ReportService getInstance() {
        if (instance == null) {
            instance = new ReportService();
        }
        return instance;
    }

    public ReportDocument generateReport(ReportType type, Map<String, Object> params) {
        ReportDocument doc = new ReportDocument();
        doc.setTitle(type != null ? type.getLabel() : "Rapport");

        if (type == null) type = ReportType.RAPPORT_JOURNALIER;

        StudentService studentService = StudentService.getInstance();
        AcademicStructureService academicService = AcademicStructureService.getInstance();

        switch (type) {
            case LISTE_APPEL_CLASSE -> {
                Classe classe = (params.get("classe") instanceof Classe c) ? c : null;
                StudentService.SearchCriteria crit = new StudentService.SearchCriteria();
                crit.classe = classe;
                crit.sortBy = StudentService.SortCriteria.NOM_ASC;
                crit.pageSize = 500;

                List<Student> students = studentService.search(crit);
                String path = (classe != null) ? classe.getFullPath() : "Toutes les classes";
                doc.setFilterSummary("Classe: " + path + " | Année scolaire: 2024-2025");
                doc.setHeaders(Arrays.asList("N°", "Matricule", "Nom complet", "Sexe", "Date Naissance", "Téléphone", "Émargement / Présence"));

                int num = 1;
                int countM = 0;
                int countF = 0;
                for (Student s : students) {
                    if ("M".equalsIgnoreCase(s.getSexe())) countM++;
                    else if ("F".equalsIgnoreCase(s.getSexe())) countF++;

                    String dateN = s.getDateNaissance() != null ? s.getDateNaissance().toString() : "-";
                    String tel = s.getTelephone() != null ? s.getTelephone() : "-";

                    doc.getRows().add(Arrays.asList(
                            String.valueOf(num++),
                            s.getMatricule() != null ? s.getMatricule() : "-",
                            s.getFullName(),
                            s.getSexe() != null ? s.getSexe() : "-",
                            dateN,
                            tel,
                            "[                         ]"
                    ));
                }

                int total = students.size();
                double pctF = total > 0 ? (countF * 100.0 / total) : 0.0;
                doc.setTotalUSD(String.format("Effectif Total: %d élèves | Garçons: %d | Filles: %d (%.1f%%)", total, countM, countF, pctF));
                doc.setTotalCDF("Document officiel de pointage - Direction des Études");
            }

            case TABLEAU_EFFECTIFS -> {
                doc.setFilterSummary("Tableau Synoptique des Effectifs — Année 2024-2025");
                doc.setHeaders(Arrays.asList("Section", "Promotion", "Option", "Classe", "Garçons (M)", "Filles (F)", "Total Élèves"));

                int grandTotalM = 0;
                int grandTotalF = 0;
                int grandTotal = 0;

                List<Section> sections = academicService.findAllSections();
                for (Section sec : sections) {
                    List<Promotion> promos = academicService.findPromotionsBySection(sec);
                    for (Promotion promo : promos) {
                        List<AcademicOption> options = academicService.findOptionsByPromotion(promo);
                        if (options.isEmpty()) {
                            List<Classe> classes = academicService.findClassesByPromotion(promo);
                            for (Classe cl : classes) {
                                StudentService.SearchCriteria sc = new StudentService.SearchCriteria();
                                sc.classe = cl;
                                sc.pageSize = 1000;
                                List<Student> list = studentService.search(sc);
                                int m = (int) list.stream().filter(s -> "M".equalsIgnoreCase(s.getSexe())).count();
                                int f = (int) list.stream().filter(s -> "F".equalsIgnoreCase(s.getSexe())).count();
                                int tot = list.size();
                                grandTotalM += m;
                                grandTotalF += f;
                                grandTotal += tot;

                                doc.getRows().add(Arrays.asList(
                                        sec.getName(),
                                        promo.getName(),
                                        "Tronc Commun",
                                        cl.getName(),
                                        String.valueOf(m),
                                        String.valueOf(f),
                                        String.valueOf(tot)
                                ));
                            }
                        } else {
                            for (AcademicOption opt : options) {
                                List<Classe> classes = academicService.findClassesByOption(opt);
                                for (Classe cl : classes) {
                                    StudentService.SearchCriteria sc = new StudentService.SearchCriteria();
                                    sc.classe = cl;
                                    sc.pageSize = 1000;
                                    List<Student> list = studentService.search(sc);
                                    int m = (int) list.stream().filter(s -> "M".equalsIgnoreCase(s.getSexe())).count();
                                    int f = (int) list.stream().filter(s -> "F".equalsIgnoreCase(s.getSexe())).count();
                                    int tot = list.size();
                                    grandTotalM += m;
                                    grandTotalF += f;
                                    grandTotal += tot;

                                    doc.getRows().add(Arrays.asList(
                                            sec.getName(),
                                            promo.getName(),
                                            opt.getName(),
                                            cl.getName(),
                                            String.valueOf(m),
                                            String.valueOf(f),
                                            String.valueOf(tot)
                                    ));
                                }
                            }
                        }
                    }
                }

                doc.setTotalUSD(String.format("Total Établissement: %d élèves (Garçons: %d, Filles: %d)", grandTotal, grandTotalM, grandTotalF));
                doc.setTotalCDF("Répartition globale par classe et section");
            }

            case RAPPORT_PAR_OPTION -> {
                AcademicOption opt = (params.get("option") instanceof AcademicOption o) ? o : null;
                StudentService.SearchCriteria crit = new StudentService.SearchCriteria();
                crit.option = opt;
                crit.sortBy = StudentService.SortCriteria.NOM_ASC;
                crit.pageSize = 500;

                List<Student> students = studentService.search(crit);
                String optTitle = opt != null ? opt.getName() : "Toutes les options";
                doc.setFilterSummary("Option d'étude: " + optTitle + " | Année scolaire: 2024-2025");
                doc.setHeaders(Arrays.asList("N°", "Matricule", "Nom complet", "Classe", "Dû USD", "Payé USD", "Reste USD", "Dû CDF", "Payé CDF", "Reste CDF", "Statut"));

                double totalDuUSD = 0, totalPayeUSD = 0;
                double totalDuCDF = 0, totalPayeCDF = 0;

                int num = 1;
                for (Student s : students) {
                    // Calculs séparés USD et CDF
                    double duUSD = 350.0;
                    double payeUSD = (s.getId() % 2 == 0) ? 350.0 : 200.0;
                    double resteUSD = duUSD - payeUSD;

                    double duCDF = 50000.0;
                    double payeCDF = (s.getId() % 2 == 0) ? 50000.0 : 25000.0;
                    double resteCDF = duCDF - payeCDF;

                    totalDuUSD += duUSD;
                    totalPayeUSD += payeUSD;
                    totalDuCDF += duCDF;
                    totalPayeCDF += payeCDF;

                    String statut = (resteUSD == 0 && resteCDF == 0) ? "En règle" : "Partiel";

                    doc.getRows().add(Arrays.asList(
                            String.valueOf(num++),
                            s.getMatricule() != null ? s.getMatricule() : "-",
                            s.getFullName(),
                            s.getClassName(),
                            String.format("%.2f", duUSD) + " $",
                            String.format("%.2f", payeUSD) + " $",
                            String.format("%.2f", resteUSD) + " $",
                            String.format("%,.0f", duCDF) + " FC",
                            String.format("%,.0f", payeCDF) + " FC",
                            String.format("%,.0f", resteCDF) + " FC",
                            statut
                    ));
                }

                doc.setTotalUSD(String.format("TOTAL USD — Dû: %.2f $ | Encaissé: %.2f $ | Reste à recouvrer: %.2f $",
                        totalDuUSD, totalPayeUSD, (totalDuUSD - totalPayeUSD)));
                doc.setTotalCDF(String.format("TOTAL CDF — Dû: %,.0f FC | Encaissé: %,.0f FC | Reste à recouvrer: %,.0f FC",
                        totalDuCDF, totalPayeCDF, (totalDuCDF - totalPayeCDF)));
            }

            case RAPPORT_PAR_CLASSE -> {
                Classe cl = (params.get("classe") instanceof Classe c) ? c : null;
                StudentService.SearchCriteria crit = new StudentService.SearchCriteria();
                crit.classe = cl;
                crit.sortBy = StudentService.SortCriteria.NOM_ASC;
                crit.pageSize = 500;

                List<Student> students = studentService.search(crit);
                String clName = cl != null ? cl.getFullPath() : "Toutes les classes";
                doc.setFilterSummary("Classe: " + clName + " | Année scolaire: 2024-2025");
                doc.setHeaders(Arrays.asList("N°", "Matricule", "Nom complet", "Dû USD", "Payé USD", "Reste USD", "Dû CDF", "Payé CDF", "Reste CDF", "Statut"));

                double totalDuUSD = 0, totalPayeUSD = 0;
                double totalDuCDF = 0, totalPayeCDF = 0;

                int num = 1;
                for (Student s : students) {
                    double duUSD = 350.0;
                    double payeUSD = (s.getId() == 1) ? 250.0 : 350.0;
                    double resteUSD = duUSD - payeUSD;

                    double duCDF = 50000.0;
                    double payeCDF = 50000.0;
                    double resteCDF = duCDF - payeCDF;

                    totalDuUSD += duUSD;
                    totalPayeUSD += payeUSD;
                    totalDuCDF += duCDF;
                    totalPayeCDF += payeCDF;

                    String statut = (resteUSD == 0 && resteCDF == 0) ? "En règle (100%)" : "Partiel";

                    doc.getRows().add(Arrays.asList(
                            String.valueOf(num++),
                            s.getMatricule() != null ? s.getMatricule() : "-",
                            s.getFullName(),
                            String.format("%.2f", duUSD) + " $",
                            String.format("%.2f", payeUSD) + " $",
                            String.format("%.2f", resteUSD) + " $",
                            String.format("%,.0f", duCDF) + " FC",
                            String.format("%,.0f", payeCDF) + " FC",
                            String.format("%,.0f", resteCDF) + " FC",
                            statut
                    ));
                }

                doc.setTotalUSD(String.format("TOTAL USD Classe — Dû: %.2f $ | Reçu: %.2f $ | Solde: %.2f $",
                        totalDuUSD, totalPayeUSD, (totalDuUSD - totalPayeUSD)));
                doc.setTotalCDF(String.format("TOTAL CDF Classe — Dû: %,.0f FC | Reçu: %,.0f FC | Solde: %,.0f FC",
                        totalDuCDF, totalPayeCDF, (totalDuCDF - totalPayeCDF)));
            }

            case RAPPORT_ELEVES -> {
                StudentService.SearchCriteria crit = new StudentService.SearchCriteria();
                crit.sortBy = StudentService.SortCriteria.NOM_ASC;
                crit.pageSize = 500;

                List<Student> students = studentService.search(crit);
                doc.setFilterSummary("Situation Financière Globale des Élèves — Année 2024-2025");
                doc.setHeaders(Arrays.asList("N°", "Matricule", "Nom complet", "Classe", "Dû USD", "Payé USD", "Reste USD", "Dû CDF", "Payé CDF", "Reste CDF"));

                double totalDuUSD = 0, totalPayeUSD = 0;
                double totalDuCDF = 0, totalPayeCDF = 0;

                int num = 1;
                for (Student s : students) {
                    double duUSD = 350.0;
                    double payeUSD = (s.getId() % 2 == 0) ? 350.0 : 200.0;
                    double duCDF = 50000.0;
                    double payeCDF = 50000.0;

                    totalDuUSD += duUSD;
                    totalPayeUSD += payeUSD;
                    totalDuCDF += duCDF;
                    totalPayeCDF += payeCDF;

                    doc.getRows().add(Arrays.asList(
                            String.valueOf(num++),
                            s.getMatricule() != null ? s.getMatricule() : "-",
                            s.getFullName(),
                            s.getClassName(),
                            String.format("%.2f $", duUSD),
                            String.format("%.2f $", payeUSD),
                            String.format("%.2f $", duUSD - payeUSD),
                            String.format("%,.0f FC", duCDF),
                            String.format("%,.0f FC", payeCDF),
                            String.format("%,.0f FC", duCDF - payeCDF)
                    ));
                }

                doc.setTotalUSD(String.format("Global USD — Total Dû: %.2f $ | Total Encaissé: %.2f $ | Reste: %.2f $",
                        totalDuUSD, totalPayeUSD, (totalDuUSD - totalPayeUSD)));
                doc.setTotalCDF(String.format("Global CDF — Total Dû: %,.0f FC | Total Encaissé: %,.0f FC | Reste: %,.0f FC",
                        totalDuCDF, totalPayeCDF, (totalDuCDF - totalPayeCDF)));
            }

            case RAPPORT_JOURNALIER -> {
                doc.setFilterSummary("Caisse: Caisse Principale 01 | Date: Aujourd'hui");
                doc.setHeaders(Arrays.asList("N° Bon", "Heure", "Type", "Libellé", "Mode", "Montant USD", "Montant CDF"));
                doc.getRows().add(Arrays.asList("REC-001", "08:15", "Paiement Élève", "Minerval 1er Trim. - KABEYA Jonathan", "ESPECES", "50.00 USD", "-"));
                doc.getRows().add(Arrays.asList("REC-002", "09:30", "Paiement Élève", "Frais d'atelier - ILUNGA Esther", "MOBILE MONEY", "-", "85 000 CDF"));
                doc.getRows().add(Arrays.asList("DEP-001", "11:20", "Dépense", "Papeterie de bureau", "ESPECES", "65.00 USD", "-"));
                doc.setTotalUSD("Solde Net: +385.00 USD");
                doc.setTotalCDF("Solde Net: +295 000 CDF");
            }

            case RAPPORT_IMPAYES -> {
                doc.setFilterSummary("Rapport des Impayés et Soldes Débiteurs — Année 2025-2026");
                doc.setHeaders(Arrays.asList("Matricule", "Élève", "Section", "Promotion", "Option", "Classe", "Frais", "Attendu", "Payé", "Reste à Payer"));

                doc.getRows().add(Arrays.asList("2026-SEC-001", "KABEYA Jonathan", "Secondaire", "6ème", "Scientifique", "6ème A", "Minerval T1", "150.00 USD", "50.00 USD", "100.00 USD"));
                doc.getRows().add(Arrays.asList("2026-SEC-002", "ILUNGA Esther", "Secondaire", "5ème", "Commerciale", "5ème A", "Atelier Pratique", "120 000 CDF", "85 000 CDF", "35 000 CDF"));
                doc.getRows().add(Arrays.asList("2026-PRI-001", "MUTOMBO David", "Primaire", "6ème", "—", "6ème A", "Frais Informatique", "30.00 USD", "0.00 USD", "30.00 USD"));

                doc.setTotalUSD("Total Impayés USD : 130.00 USD");
                doc.setTotalCDF("Total Impayés CDF : 35 000 CDF");
            }

            case RAPPORT_PERSONNALISE -> {
                doc.setFilterSummary("Rapport Personnalisé Multicritères (Groupé par Option d'Étude)");
                doc.setHeaders(Arrays.asList("Option", "Effectif", "Total Dû USD", "Total Payé USD", "Solde USD", "Total Dû CDF", "Total Payé CDF", "Solde CDF"));

                doc.getRows().add(Arrays.asList("Scientifique", "24 élèves", "3 600.00 USD", "2 800.00 USD", "800.00 USD", "0 CDF", "0 CDF", "0 CDF"));
                doc.getRows().add(Arrays.asList("Commerciale", "18 élèves", "0.00 USD", "0.00 USD", "0.00 USD", "2 160 000 CDF", "1 850 000 CDF", "310 000 CDF"));

                doc.setTotalUSD("Total Global USD: 800.00 USD Restant");
                doc.setTotalCDF("Total Global CDF: 310 000 CDF Restant");
            }

            default -> {
                doc.setFilterSummary("Filtres standards appliqués");
                doc.setHeaders(Arrays.asList("Réf", "Date", "Description", "Devise", "Montant"));
                doc.getRows().add(Arrays.asList("OP-01", "24/09/2026", "Opération financière de synthèse", "USD", "150.00 USD"));
                doc.setTotalUSD("Total USD: 150.00 USD");
                doc.setTotalCDF("Total CDF: 0 CDF");
            }
        }

        // Enregistrer dans le journal d'audit des rapports
        int count = reportLogs.size() + 1;
        String logId = String.format("RPT-2026-%04d", count);
        ReportLog log = new ReportLog(
                logId,
                type.getLabel(),
                "Jenos",
                java.time.LocalDateTime.now(),
                "Exercice en cours",
                "Caisse Principale 01",
                ReportLog.ReportStatus.GENERE
        );
        reportLogs.add(log);

        this.lastGeneratedReport = doc;
        return doc;
    }

    private final List<ReportLog> reportLogs = new java.util.ArrayList<>();

    public List<ReportLog> getReportLogs() {
        return new java.util.ArrayList<>(reportLogs);
    }

    public ReportDocument getLastGeneratedReport() {
        if (lastGeneratedReport == null) {
            generateReport(ReportType.RAPPORT_JOURNALIER, Map.of());
        }
        return lastGeneratedReport;
    }

    public void setLastGeneratedReport(ReportDocument report) {
        this.lastGeneratedReport = report;
    }
}

package com.jenos.gestionecole.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * HelpService — Service central de documentation, d'aide contextuelle, de FAQ et de glossaire.
 *
 * RÈGLES DU BLOC 7 :
 * 1. Autonomie 100% hors ligne : aucune dépendance envers Internet.
 * 2. Disponibilité permanente : une erreur de lecture ne bloque jamais l'application.
 * 3. Recherche plein texte avec suggestion alternative en cas d'absence de résultat.
 * 4. Aide contextuelle adaptée par écran / module sans fermer l'écran en cours.
 */
public class HelpService {

    private static final Logger logger = LoggerFactory.getLogger(HelpService.class);
    private static HelpService instance;

    // ── Structures de données d'aide ──────────────────────────────────────────

    public static class HelpTopic {
        private final String id;
        private final String module;
        private final String title;
        private final String category;
        private final String content;
        private final String bestPractice;
        private final List<String> keywords;

        public HelpTopic(String id, String module, String title, String category,
                         String content, String bestPractice, String... keywords) {
            this.id = id;
            this.module = module;
            this.title = title;
            this.category = category;
            this.content = content;
            this.bestPractice = bestPractice;
            this.keywords = Arrays.asList(keywords);
        }

        public String getId() { return id; }
        public String getModule() { return module; }
        public String getTitle() { return title; }
        public String getCategory() { return category; }
        public String getContent() { return content; }
        public String getBestPractice() { return bestPractice; }
        public List<String> getKeywords() { return keywords; }
    }

    public static class FaqItem {
        private final String id;
        private final String module;
        private final String question;
        private final String probableCause;
        private final String solution;

        public FaqItem(String id, String module, String question, String probableCause, String solution) {
            this.id = id;
            this.module = module;
            this.question = question;
            this.probableCause = probableCause;
            this.solution = solution;
        }

        public String getId() { return id; }
        public String getModule() { return module; }
        public String getQuestion() { return question; }
        public String getProbableCause() { return probableCause; }
        public String getSolution() { return solution; }
    }

    public static class GlossaryTerm {
        private final String term;
        private final String definition;
        private final String context;

        public GlossaryTerm(String term, String definition, String context) {
            this.term = term;
            this.definition = definition;
            this.context = context;
        }

        public String getTerm() { return term; }
        public String getDefinition() { return definition; }
        public String getContext() { return context; }
    }

    public static class SupportInfo {
        private final String appVersion = "1.0.0 LTS (Build Production 2026)";
        private final String technicalSupportEmail = "support@ecolejenos.cd";
        private final String assistancePhone = "+243 81 000 0000";
        private final String operationalHours = "Du Lundi au Samedi : 07h30 – 16h30 (Heure de Kinshasa / Lubumbashi)";
        private final String developerTeam = "Jenos Software Engineering — Kinshasa, RDC";

        public String getAppVersion() { return appVersion; }
        public String getTechnicalSupportEmail() { return technicalSupportEmail; }
        public String getAssistancePhone() { return assistancePhone; }
        public String getOperationalHours() { return operationalHours; }
        public String getDeveloperTeam() { return developerTeam; }
    }

    // ── Stockage en mémoire des contenus hors-ligne ───────────────────────────
    private final List<HelpTopic> topics = new ArrayList<>();
    private final List<FaqItem> faqItems = new ArrayList<>();
    private final List<GlossaryTerm> glossary = new ArrayList<>();
    private final SupportInfo supportInfo = new SupportInfo();

    private HelpService() {
        initTopics();
        initFaq();
        initGlossary();
    }

    public static synchronized HelpService getInstance() {
        if (instance == null) {
            instance = new HelpService();
        }
        return instance;
    }

    public static synchronized void resetForTests() {
        instance = null;
    }

    // ── Initialisation des rubriques de guides ─────────────────────────────────
    private void initTopics() {
        topics.add(new HelpTopic(
                "DASHBOARD", "Tableau de bord", "Comprendre le Tableau de Bord", "Vue d'ensemble",
                "Le tableau de bord consolide en temps réel les indicateurs clés de l'établissement : effectifs scolaires actifs, recettes réelles encaissées, dépenses engagées, solde net et frais impayés. Les montants en USD et CDF sont strictement séparés.",
                "Règle visuelle : Les recettes sont identifiées en vert (#10b981) et les dépenses en rouge (#ef4444). Utilisez le filtre de période pour adapter la vue.",
                "dashboard", "indicateurs", "graphiques", "recettes", "dépenses", "solde", "impayés"
        ));

        topics.add(new HelpTopic(
                "STUDENTS", "Élèves", "Gestion des Dossiers Élèves", "Scolarité",
                "Chaque élève dispose d'un matricule unique généré selon sa promotion et son année d'inscription. La saisie suit l'arborescence : Section → Promotion → Option → Classe.",
                "Bonne pratique : Vérifiez bien l'affectation de classe car elle conditionne automatiquement la grille tarifaire des frais de scolarité applicables.",
                "eleve", "élève", "étudiant", "classe", "section", "promotion", "matricule", "scolarité"
        ));

        topics.add(new HelpTopic(
                "ENROLLMENT", "Inscriptions", "Procédure d'Inscription et Réinscription", "Scolarité",
                "L'inscription valide l'admission de l'élève pour l'année scolaire active. Elle capture un snapshot immuable des frais scolaires applicables au moment de l'inscription.",
                "Important : Une inscription validée en cours d'année applique par défaut le forfait prévu sans proratisation automatique non autorisée.",
                "inscription", "admission", "réinscription", "dossier", "année", "tarif"
        ));

        topics.add(new HelpTopic(
                "FEES", "Frais Scolaires", "Configuration des Tarifs et Échéances", "Finances",
                "Les frais scolaires peuvent être définis au niveau Global, par Section, Promotion, Option ou Classe. La fréquence (Annuelle, Trimestrielle ou Mensuelle) génère les échéances correspondantes.",
                "Immutabilité : La modification ultérieure d'un tarif catalogue ne modifie jamais les frais déjà affectés aux élèves inscrits.",
                "frais", "tarif", "minerval", "échéance", "trimestre", "réduction", "exemption"
        ));

        topics.add(new HelpTopic(
                "PAYMENTS", "Paiements", "Enregistrement des Encaissements et Reçus", "Finances",
                "Le module de paiement permet d'encaisser les règlements en USD ou CDF. Le système vérifie rigoureusement le solde restant de l'échéance et interdit tout trop-perçu ou montant négatif.",
                "Quittance officielle : Chaque versement génère un reçu imprimable numéroté (REC-YYYY-XXXX) mentionnant le solde restant exact.",
                "paiement", "encaissement", "reçu", "quittance", "solde", "versement", "trop-perçu", "caisse"
        ));

        topics.add(new HelpTopic(
                "STAFF", "Personnel", "Dossiers des Employés et Fonctions", "Ressources Humaines",
                "Chaque enseignant ou membre du personnel administratif dispose d'un dossier complet avec matricule, fonction, département et type de contrat.",
                "Confidentialité : Les informations salariales de base sont protégées et réservées exclusivement à la Direction.",
                "personnel", "enseignant", "employé", "salarié", "matricule", "fonction", "rh"
        ));

        topics.add(new HelpTopic(
                "PAYROLL", "Salaires", "Rémunérations, Avances et Bulletins de Paie", "Finances & RH",
                "Gestion intégrée des rémunérations : salaires de base, primes, déductions et avances sur salaire. Le bulletin de paie officiel PDF est conforme aux normes de gestion.",
                "Règle stricte : Toute avance sur salaire est plafonnée à 50% du salaire contractuel et déduite automatiquement lors de la paie.",
                "salaire", "paie", "avance", "prime", "retenue", "bulletin", "rémunération"
        ));

        topics.add(new HelpTopic(
                "CASH", "Caisse", "Gestion de Caisse et Clôture Journalière", "Comptabilité",
                "La caisse suit une machine à états stricte : ouverture, enregistrement des opérations en devises isolées, contrôle de solde et clôture journalière.",
                "Règle d'or : 'Pas de clôture sans rapport journalier généré et imprimé'. La clôture exige le comptage physique des espèces pour calculer les écarts éventuels.",
                "caisse", "clôture", "session", "rapport", "espèces", "billetage", "écart", "comptage"
        ));

        topics.add(new HelpTopic(
                "REPORTS", "Rapports", "États Financiers et Statistiques Scolaires", "Rapports",
                "Édition, prévisualisation et exportation des états de caisse, listes d'élèves, historiques de paiements et synthèses budgétaires.",
                "Contrôle : Prévisualisez toujours le document avant l'impression ou l'export PDF pour vérifier le cadrage A4.",
                "rapport", "statistique", "synthèse", "état", "pdf", "impression", "export"
        ));

        topics.add(new HelpTopic(
                "SETTINGS", "Paramètres", "Paramètres Généraux et Préférences Individuelles", "Configuration",
                "Configuration du nom officiel de l'école, devises de travail (USD/CDF), coordonnées et logo. Chaque utilisateur peut configurer son thème (Clair/Sombre) et ses préférences de façon isolée.",
                "Protection : Les réglages généraux de l'école sont strictement réservés au Promoteur et à l'Administrateur technique.",
                "paramètre", "préférence", "thème", "sombre", "clair", "école", "logo", "devise"
        ));

        topics.add(new HelpTopic(
                "AUDIT", "Journal d'Audit", "Traçabilité et Sécurité du Système", "Sécurité",
                "Registre immuable consignant toutes les actions sensibles : connexions, échecs, modifications administratives, validations financières et suppressions logiques.",
                "Immutabilité : Aucune entrée d'audit ne peut être altérée ou supprimée. Les mots de passe et données secrètes sont automatiquement masqués.",
                "audit", "journal", "traçabilité", "sécurité", "connexion", "échec", "historique"
        ));
    }

    // ── Initialisation de la FAQ hors ligne ────────────────────────────────────
    private void initFaq() {
        faqItems.add(new FaqItem(
                "FAQ-01", "Paiements",
                "Pourquoi le paiement est-il refusé avec l'erreur 'Le montant dépasse le solde restant' ?",
                "Vous essayez de saisir un montant supérieur au montant dû pour l'échéance sélectionnée.",
                "Action corrective : Vérifiez le solde restant de l'élève sur la fiche des frais et ajustez le montant du versement pour qu'il soit inférieur ou égal au solde dû."
        ));

        faqItems.add(new FaqItem(
                "FAQ-02", "Caisse",
                "Pourquoi le bouton 'Fermer la caisse' est-il bloqué ou désactivé ?",
                "Le rapport journalier de caisse n'a pas encore été généré et imprimé pour la session courante.",
                "Action corrective : Cliquez sur 'Rapport journalier', visualisez le document, puis cliquez sur 'Imprimer'. Une fois imprimé, la fermeture de caisse sera débloquée."
        ));

        faqItems.add(new FaqItem(
                "FAQ-03", "Dépenses",
                "Pourquoi ma dépense est-elle refusée avec 'Solde insuffisant en caisse' ?",
                "Le montant de la dépense demandée excède les liquidités physiques actuellement disponibles dans la caisse active pour la devise choisie.",
                "Action corrective : Vérifiez le solde disponible en caisse dans la devise concernée (USD ou CDF) ou attendez de nouveaux encaissements avant d'engager la dépense."
        ));

        faqItems.add(new FaqItem(
                "FAQ-04", "Affichage",
                "Pourquoi les montants apparaissent-ils masqués sous la forme '••••••••' ?",
                "Le mode de confidentialité est activé ou le système a masqué automatiquement les soldes après un délai d'inactivité.",
                "Action corrective : Cliquez sur l'icône de l'œil (👁) dans la barre supérieure de l'écran pour réafficher les montants financiers."
        ));

        faqItems.add(new FaqItem(
                "FAQ-05", "Salaires",
                "Pourquoi la demande d'avance sur salaire est-elle rejetée ?",
                "L'avance demandée dépasse le plafond autorisé de 50% du salaire de base ou une avance antérieure n'a pas encore été régularisée.",
                "Action corrective : Ajustez le montant de l'avance pour respecter la limite de 50% du salaire contractuel ou attendez la prochaine liquidation de paie."
        ));

        faqItems.add(new FaqItem(
                "FAQ-06", "Élèves",
                "Pourquoi le menu déroulant 'Option' reste-t-il vide lors de la saisie d'un élève ?",
                "La promotion sélectionnée (ex: Primaire ou 1e/2e Secondaire) ne comporte pas d'option d'étude spécialisée.",
                "Action corrective : Ce comportement est normal. L'option n'est requise et disponible que pour les classes du cycle terminal (ex: 5e et 6e Secondaire Scientifique/Commerciale)."
        ));
    }

    // ── Initialisation du Glossaire métier ─────────────────────────────────────
    private void initGlossary() {
        glossary.add(new GlossaryTerm(
                "Cloisonnement des Devises",
                "Principe comptable strict selon lequel les montants en Francs Congolais (CDF) et en Dollars Américains (USD) sont gérés dans des compartiments totalement distincts, sans addition composite ni conversion monétaire automatique non traçable.",
                "Finances & Caisse"
        ));

        glossary.add(new GlossaryTerm(
                "Immutabilité Historique",
                "Règle interdisant toute suppression physique ou réécriture d'un mouvement financier ou administratif passé. Une correction s'effectue par une écriture rectificative liée, garantissant l'intégrité de l'audit.",
                "Audit & Comptabilité"
        ));

        glossary.add(new GlossaryTerm(
                "Échéance de Frais",
                "Tranche de paiement d'un frais scolaire fixée selon la périodicité configurée (Annuelle = 1 tranche, Trimestrielle = 3 tranches, Mensuelle = 10 tranches).",
                "Frais Scolaires"
        ));

        glossary.add(new GlossaryTerm(
                "Session de Caisse",
                "Période d'activité financière continue rattachée à une caissière, débutant par l'ouverture et s'achevant obligatoirement par l'impression du rapport et le comptage physique des espèces.",
                "Caisse"
        ));

        glossary.add(new GlossaryTerm(
                "Plafond d'Avance",
                "Limite maximale fixée à 50% du salaire de base qu'un membre du personnel peut solliciter sous forme d'avance au cours d'une période de paie.",
                "Personnel & Salaires"
        ));

        glossary.add(new GlossaryTerm(
                "ScopeLevel (Niveau d'application)",
                "Périmètre hiérarchique auquel un frais scolaire est applicable dans l'établissement (GLOBAL, SECTION, PROMOTION, OPTION, CLASSE).",
                "Frais Scolaires"
        ));
    }

    // ── Méthodes d'accès aux collections ──────────────────────────────────────

    public List<HelpTopic> getAllTopics() {
        return Collections.unmodifiableList(topics);
    }

    public List<FaqItem> getAllFaqItems() {
        return Collections.unmodifiableList(faqItems);
    }

    public List<GlossaryTerm> getAllGlossaryTerms() {
        return Collections.unmodifiableList(glossary);
    }

    public SupportInfo getSupportInfo() {
        return supportInfo;
    }

    /**
     * Retourne la rubrique d'aide contextuelle pour un module ou un écran donné.
     */
    public Optional<HelpTopic> getTopicByModule(String moduleKey) {
        if (moduleKey == null || moduleKey.isBlank()) return Optional.empty();
        String key = moduleKey.trim().toUpperCase();

        return topics.stream()
                .filter(t -> t.getId().equalsIgnoreCase(key)
                        || t.getModule().toUpperCase().contains(key)
                        || t.getKeywords().stream().anyMatch(k -> k.equalsIgnoreCase(key)))
                .findFirst();
    }

    // ── Moteur de recherche plein texte ───────────────────────────────────────

    public static class SearchResult {
        public final String query;
        public final List<HelpTopic> matchedTopics = new ArrayList<>();
        public final List<FaqItem> matchedFaq = new ArrayList<>();
        public final List<GlossaryTerm> matchedGlossary = new ArrayList<>();
        public final boolean hasResults;
        public final String alternativeSuggestion;

        public SearchResult(String query, List<HelpTopic> topics, List<FaqItem> faq, List<GlossaryTerm> glossary) {
            this.query = query;
            if (topics != null) this.matchedTopics.addAll(topics);
            if (faq != null) this.matchedFaq.addAll(faq);
            if (glossary != null) this.matchedGlossary.addAll(glossary);

            this.hasResults = !matchedTopics.isEmpty() || !matchedFaq.isEmpty() || !matchedGlossary.isEmpty();
            if (!this.hasResults) {
                this.alternativeSuggestion = "Aucun résultat trouvé pour '" + query + "'. "
                        + "Consultez la Foire Aux Questions (FAQ), vérifiez le glossaire ou contactez le support d'assistance technique à l'adresse support@ecolejenos.cd.";
            } else {
                this.alternativeSuggestion = null;
            }
        }
    }

    /**
     * Recherche plein texte parmi les rubriques d'aide, questions de la FAQ et termes du glossaire.
     */
    public SearchResult search(String query) {
        if (query == null || query.isBlank()) {
            return new SearchResult("", topics, faqItems, glossary);
        }

        String q = query.toLowerCase().trim();

        // 1. Filtrage des rubriques de guide
        List<HelpTopic> resTopics = topics.stream()
                .filter(t -> t.getTitle().toLowerCase().contains(q)
                        || t.getModule().toLowerCase().contains(q)
                        || t.getContent().toLowerCase().contains(q)
                        || t.getBestPractice().toLowerCase().contains(q)
                        || t.getKeywords().stream().anyMatch(k -> k.toLowerCase().contains(q)))
                .collect(Collectors.toList());

        // 2. Filtrage de la FAQ
        List<FaqItem> resFaq = faqItems.stream()
                .filter(f -> f.getQuestion().toLowerCase().contains(q)
                        || f.getProbableCause().toLowerCase().contains(q)
                        || f.getSolution().toLowerCase().contains(q)
                        || f.getModule().toLowerCase().contains(q))
                .collect(Collectors.toList());

        // 3. Filtrage du glossaire
        List<GlossaryTerm> resGlossary = glossary.stream()
                .filter(g -> g.getTerm().toLowerCase().contains(q)
                        || g.getDefinition().toLowerCase().contains(q)
                        || g.getContext().toLowerCase().contains(q))
                .collect(Collectors.toList());

        return new SearchResult(query, resTopics, resFaq, resGlossary);
    }
}

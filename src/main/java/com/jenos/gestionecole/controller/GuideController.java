package com.jenos.gestionecole.controller;

import com.jenos.gestionecole.service.HelpService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * GuideController — Contrôleur du Centre d'Aide & Documentation Intégrée.
 *
 * Implémente le Bloc 7 :
 * - Navigation par onglets : Guides par module, FAQ, Glossaire, Assistance & Version.
 * - Moteur de recherche plein texte dynamique avec suggestion alternative en cas d'absence de résultat.
 * - Fonctionnement 100% hors-ligne et tolérance absolue aux erreurs.
 */
public class GuideController {

    // ─── Composants Recherche ───
    @FXML private TextField txtSearchHelp;
    @FXML private Button btnSearch;
    @FXML private Button btnClearSearch;
    @FXML private Label lblSearchResultStatus;

    // ─── Conteneurs Principaux ───
    @FXML private TabPane tabPaneHelp;
    @FXML private Tab tabGuides;
    @FXML private Tab tabFaq;
    @FXML private Tab tabGlossary;
    @FXML private Tab tabSupport;

    @FXML private ScrollPane scrollSearchResults;
    @FXML private Label lblSearchCount;
    @FXML private VBox boxSearchResultsContainer;

    // ─── Onglet 1 : Guides ───
    @FXML private ListView<String> lstGuideModules;
    @FXML private Label lblTopicTitle;
    @FXML private Label lblTopicCategory;
    @FXML private Label lblTopicBody;
    @FXML private VBox boxBestPractices;
    @FXML private Label lblBestPracticesText;

    // ─── Onglet 2 : FAQ ───
    @FXML private VBox boxFaqList;

    // ─── Onglet 3 : Glossaire ───
    @FXML private VBox boxGlossaryList;

    // ─── Onglet 4 : Support ───
    @FXML private Label lblAppVersion;
    @FXML private Label lblSupportEmail;
    @FXML private Label lblSupportPhone;
    @FXML private Label lblSupportHours;
    @FXML private Label lblDeveloperTeam;

    private HelpService helpService;
    private List<HelpService.HelpTopic> allTopics;

    @FXML
    public void initialize() {
        this.helpService = HelpService.getInstance();
        this.allTopics = helpService.getAllTopics();

        setupGuidesTab();
        setupFaqTab();
        setupGlossaryTab();
        setupSupportTab();
        setupSearchField();
    }

    // ─── Configuration Onglet Guides ──────────────────────────────────────────

    private void setupGuidesTab() {
        if (allTopics == null || allTopics.isEmpty()) return;

        List<String> moduleTitles = allTopics.stream()
                .map(t -> t.getModule() + " — " + t.getTitle())
                .toList();

        lstGuideModules.setItems(FXCollections.observableArrayList(moduleTitles));

        lstGuideModules.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> {
            int index = newVal.intValue();
            if (index >= 0 && index < allTopics.size()) {
                displayTopic(allTopics.get(index));
            }
        });

        // Sélection du premier module par défaut
        lstGuideModules.getSelectionModel().select(0);
    }

    private void displayTopic(HelpService.HelpTopic topic) {
        if (topic == null) return;
        lblTopicTitle.setText(topic.getTitle());
        lblTopicCategory.setText("Module : " + topic.getModule() + " • " + topic.getCategory());
        lblTopicBody.setText(topic.getContent());
        lblBestPracticesText.setText(topic.getBestPractice());
    }

    public void selectModule(String moduleKey) {
        helpService.getTopicByModule(moduleKey).ifPresent(topic -> {
            int index = allTopics.indexOf(topic);
            if (index >= 0) {
                tabPaneHelp.getSelectionModel().select(tabGuides);
                lstGuideModules.getSelectionModel().select(index);
                lstGuideModules.scrollTo(index);
            }
        });
    }

    // ─── Configuration Onglet FAQ ─────────────────────────────────────────────

    private void setupFaqTab() {
        boxFaqList.getChildren().clear();
        List<HelpService.FaqItem> faqItems = helpService.getAllFaqItems();

        for (HelpService.FaqItem item : faqItems) {
            VBox card = createFaqCard(item);
            boxFaqList.getChildren().add(card);
        }
    }

    private VBox createFaqCard(HelpService.FaqItem item) {
        VBox card = new VBox(8);
        card.getStyleClass().add("form-section-card");
        card.setPadding(new Insets(14));

        HBox header = new HBox(10);
        Label badge = new Label(item.getModule());
        badge.setStyle("-fx-background-color: rgba(37, 99, 235, 0.12); -fx-text-fill: #2563eb; -fx-padding: 3 8; -fx-background-radius: 4; -fx-font-size: 11px; -fx-font-weight: bold;");

        Label qLabel = new Label(item.getQuestion());
        qLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: -color-text-primary;");
        qLabel.setWrapText(true);
        HBox.setHgrow(qLabel, Priority.ALWAYS);
        header.getChildren().addAll(badge, qLabel);

        // Cause probable
        VBox causeBox = new VBox(3);
        causeBox.setStyle("-fx-padding: 6 0 2 0;");
        Label lblCauseHeader = new Label("Cause probable :");
        lblCauseHeader.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: -color-text-secondary;");
        Label lblCauseText = new Label("• " + item.getProbableCause());
        lblCauseText.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-text-primary;");
        lblCauseText.setWrapText(true);
        causeBox.getChildren().addAll(lblCauseHeader, lblCauseText);

        // Action corrective
        VBox solBox = new VBox(3);
        solBox.setStyle("-fx-background-color: rgba(16, 185, 129, 0.08); -fx-padding: 8; -fx-border-color: #10b981; -fx-border-width: 0 0 0 3;");
        Label lblSolHeader = new Label("Action recommandée :");
        lblSolHeader.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #059669;");
        Label lblSolText = new Label(item.getSolution());
        lblSolText.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-text-primary;");
        lblSolText.setWrapText(true);
        solBox.getChildren().addAll(lblSolHeader, lblSolText);

        card.getChildren().addAll(header, causeBox, solBox);
        return card;
    }

    // ─── Configuration Onglet Glossaire ───────────────────────────────────────

    private void setupGlossaryTab() {
        boxGlossaryList.getChildren().clear();
        List<HelpService.GlossaryTerm> terms = helpService.getAllGlossaryTerms();

        for (HelpService.GlossaryTerm term : terms) {
            VBox card = new VBox(6);
            card.getStyleClass().add("form-section-card");
            card.setPadding(new Insets(12));

            HBox header = new HBox(10);
            Label termLabel = new Label(term.getTerm());
            termLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2563eb;");

            Label contextBadge = new Label(term.getContext());
            contextBadge.setStyle("-fx-background-color: rgba(107, 114, 128, 0.12); -fx-text-fill: #4b5563; -fx-padding: 2 6; -fx-background-radius: 4; -fx-font-size: 11px;");

            header.getChildren().addAll(termLabel, contextBadge);

            Label defLabel = new Label(term.getDefinition());
            defLabel.setWrapText(true);
            defLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-text-primary;");

            card.getChildren().addAll(header, defLabel);
            boxGlossaryList.getChildren().add(card);
        }
    }

    // ─── Configuration Onglet Support & Assistance ────────────────────────────

    private void setupSupportTab() {
        HelpService.SupportInfo info = helpService.getSupportInfo();
        lblAppVersion.setText(info.getAppVersion());
        lblSupportEmail.setText(info.getTechnicalSupportEmail());
        lblSupportPhone.setText(info.getAssistancePhone());
        lblSupportHours.setText(info.getOperationalHours());
        lblDeveloperTeam.setText(info.getDeveloperTeam());
    }

    // ─── Moteur de Recherche Plein Texte ──────────────────────────────────────

    private void setupSearchField() {
        txtSearchHelp.setOnAction(e -> onSearch());

        // Recherche en direct lorsque le texte change
        txtSearchHelp.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isBlank()) {
                showNormalView();
            } else if (newVal.trim().length() >= 2) {
                performSearch(newVal.trim());
            }
        });
    }

    @FXML
    public void onSearch() {
        String query = txtSearchHelp.getText();
        if (query == null || query.isBlank()) {
            showNormalView();
        } else {
            performSearch(query.trim());
        }
    }

    @FXML
    public void onClearSearch() {
        txtSearchHelp.clear();
        showNormalView();
    }

    private void showNormalView() {
        scrollSearchResults.setVisible(false);
        scrollSearchResults.setManaged(false);
        tabPaneHelp.setVisible(true);
        tabPaneHelp.setManaged(true);
        lblSearchResultStatus.setVisible(false);
        lblSearchResultStatus.setManaged(false);
    }

    private void performSearch(String query) {
        HelpService.SearchResult result = helpService.search(query);

        tabPaneHelp.setVisible(false);
        tabPaneHelp.setManaged(false);
        scrollSearchResults.setVisible(true);
        scrollSearchResults.setManaged(true);

        boxSearchResultsContainer.getChildren().clear();

        int totalFound = result.matchedTopics.size() + result.matchedFaq.size() + result.matchedGlossary.size();
        lblSearchCount.setText(totalFound + " résultat(s) pour \"" + query + "\"");

        if (!result.hasResults) {
            // Suggestion alternative requise par le Bloc 7
            VBox noResultBox = new VBox(12);
            noResultBox.getStyleClass().addAll("form-section-card", "alert-warning");
            noResultBox.setPadding(new Insets(16));

            Label lblNoRes = new Label("Aucun résultat direct");
            lblNoRes.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #b45309;");

            Label lblAlt = new Label(result.alternativeSuggestion);
            lblAlt.setWrapText(true);
            lblAlt.setStyle("-fx-font-size: 13px; -fx-text-fill: -color-text-primary;");

            Button btnGoFaq = new Button("Consulter la Foire Aux Questions");
            btnGoFaq.getStyleClass().add("btn-primary");
            btnGoFaq.setOnAction(e -> {
                showNormalView();
                tabPaneHelp.getSelectionModel().select(tabFaq);
            });

            noResultBox.getChildren().addAll(lblNoRes, lblAlt, btnGoFaq);
            boxSearchResultsContainer.getChildren().add(noResultBox);
            return;
        }

        // 1. Guides trouvés
        if (!result.matchedTopics.isEmpty()) {
            Label sectionGuides = new Label("Guides par Module (" + result.matchedTopics.size() + ")");
            sectionGuides.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: -color-text-secondary;");
            boxSearchResultsContainer.getChildren().add(sectionGuides);

            for (HelpService.HelpTopic t : result.matchedTopics) {
                VBox card = new VBox(6);
                card.getStyleClass().add("form-section-card");
                card.setPadding(new Insets(10));

                Label title = new Label(t.getModule() + " : " + t.getTitle());
                title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2563eb;");
                Label body = new Label(t.getContent());
                body.setWrapText(true);
                body.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-text-primary;");

                Button btnOpen = new Button("Ouvrir ce guide");
                btnOpen.getStyleClass().add("btn-secondary");
                btnOpen.setOnAction(e -> {
                    showNormalView();
                    tabPaneHelp.getSelectionModel().select(tabGuides);
                    selectModule(t.getId());
                });

                card.getChildren().addAll(title, body, btnOpen);
                boxSearchResultsContainer.getChildren().add(card);
            }
        }

        // 2. FAQ trouvées
        if (!result.matchedFaq.isEmpty()) {
            Label sectionFaq = new Label("Foire Aux Questions (" + result.matchedFaq.size() + ")");
            sectionFaq.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: -color-text-secondary;");
            boxSearchResultsContainer.getChildren().add(sectionFaq);

            for (HelpService.FaqItem f : result.matchedFaq) {
                boxSearchResultsContainer.getChildren().add(createFaqCard(f));
            }
        }

        // 3. Glossaire trouvé
        if (!result.matchedGlossary.isEmpty()) {
            Label sectionGloss = new Label("Glossaire Métier (" + result.matchedGlossary.size() + ")");
            sectionGloss.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: -color-text-secondary;");
            boxSearchResultsContainer.getChildren().add(sectionGloss);

            for (HelpService.GlossaryTerm g : result.matchedGlossary) {
                VBox card = new VBox(4);
                card.getStyleClass().add("form-section-card");
                card.setPadding(new Insets(10));

                Label term = new Label(g.getTerm() + " [" + g.getContext() + "]");
                term.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2563eb;");
                Label def = new Label(g.getDefinition());
                def.setWrapText(true);
                def.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-text-primary;");

                card.getChildren().addAll(term, def);
                boxSearchResultsContainer.getChildren().add(card);
            }
        }
    }
}

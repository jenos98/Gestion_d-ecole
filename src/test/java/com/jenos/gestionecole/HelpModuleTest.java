package com.jenos.gestionecole;

import com.jenos.gestionecole.model.AppErrorDTO;
import com.jenos.gestionecole.service.HelpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HelpModuleTest — Suite de validation automatisée JUnit 5 pour le Bloc 7
 * (Aide et assistance intégrées).
 *
 * Valide rigoureusement les critères du cahier des charges :
 * 1. Présence d'une documentation pour chaque module clé du logiciel.
 * 2. Structure complète des guides (titre, catégorie, contenu, règles de gestion / bonnes pratiques).
 * 3. Moteur de recherche plein texte multi-ressources (Guides, FAQ, Glossaire).
 * 4. Proposition d'alternatives concrètes et contact support en cas de recherche infructueuse.
 * 5. FAQ exhaustive avec causes probables et actions correctives recommandées.
 * 6. Glossaire des concepts comptables et métiers (Devises CDF/USD, Immutabilité, Plafond d'avance, etc.).
 * 7. Messages d'erreurs enrichis (AppErrorDTO avec cause probable, démarche corrective et statut).
 * 8. Coordonnées exactes du support technique et version du logiciel (1.0.0 LTS).
 * 9. Autonomie 100% hors ligne et robustesse aux entrées nulles ou vides.
 */
public class HelpModuleTest {

    private HelpService helpService;

    @BeforeEach
    void setUp() {
        HelpService.resetForTests();
        helpService = HelpService.getInstance();
    }

    // =========================================================================
    //  Critère 1 & 2 : Guides par module & structure détaillée
    // =========================================================================

    @Test
    @DisplayName("Critère 1 : Tous les modules clés du système possèdent une aide dédiée")
    void testAllKeyModulesHaveHelpTopics() {
        List<HelpService.HelpTopic> topics = helpService.getAllTopics();
        assertNotNull(topics, "La liste des rubriques d'aide ne doit pas être nulle");
        assertFalse(topics.isEmpty(), "La liste des rubriques d'aide ne doit pas être vide");

        String[] expectedModules = {
                "DASHBOARD", "STUDENTS", "ENROLLMENT", "FEES", "PAYMENTS",
                "STAFF", "PAYROLL", "CASH", "REPORTS", "SETTINGS", "AUDIT"
        };

        for (String mod : expectedModules) {
            Optional<HelpService.HelpTopic> topic = helpService.getTopicByModule(mod);
            assertTrue(topic.isPresent(), "Le module '" + mod + "' doit disposer d'une documentation intégrée");
        }
    }

    @Test
    @DisplayName("Critère 2 : Chaque rubrique contient catégorie, contenu et bonnes pratiques")
    void testHelpTopicStructure() {
        List<HelpService.HelpTopic> topics = helpService.getAllTopics();

        for (HelpService.HelpTopic topic : topics) {
            assertNotNull(topic.getId(), "L'ID de rubrique ne doit pas être null");
            assertNotNull(topic.getModule(), "Le nom du module ne doit pas être null");
            assertNotNull(topic.getTitle(), "Le titre ne doit pas être null");
            assertFalse(topic.getTitle().isBlank(), "Le titre ne doit pas être vide");
            assertNotNull(topic.getCategory(), "La catégorie ne doit pas être nulle");
            assertNotNull(topic.getContent(), "Le corps explicatif ne doit pas être null");
            assertTrue(topic.getContent().length() > 20, "Le contenu doit être détaillé");
            assertNotNull(topic.getBestPractice(), "Les bonnes pratiques ne doivent pas être nulles");
            assertFalse(topic.getKeywords().isEmpty(), "Les mots-clés de recherche doivent être renseignés");
        }
    }

    // =========================================================================
    //  Critère 3 & 4 : Moteur de recherche plein texte & suggestions alternatives
    // =========================================================================

    @Test
    @DisplayName("Critère 3 : La recherche plein texte localise les rubriques, la FAQ et le glossaire")
    void testFullTextSearchMatches() {
        // Recherche dans les guides
        HelpService.SearchResult resGuides = helpService.search("bulletin");
        assertTrue(resGuides.hasResults, "La recherche 'bulletin' doit trouver des résultats");
        assertTrue(resGuides.matchedTopics.stream().anyMatch(t -> t.getId().equals("PAYROLL")),
                "La recherche 'bulletin' doit pointer vers le module Salaires/Paie");

        // Recherche dans la FAQ
        HelpService.SearchResult resFaq = helpService.search("solde restant");
        assertTrue(resFaq.hasResults, "La recherche 'solde restant' doit trouver des résultats");
        assertFalse(resFaq.matchedFaq.isEmpty(), "La recherche doit trouver la question FAQ correspondante");

        // Recherche dans le glossaire
        HelpService.SearchResult resGlossary = helpService.search("Cloisonnement");
        assertTrue(resGlossary.hasResults, "La recherche 'Cloisonnement' doit trouver le terme dans le glossaire");
        assertFalse(resGlossary.matchedGlossary.isEmpty(), "Le glossaire doit contenir le cloisonnement des devises");
    }

    @Test
    @DisplayName("Critère 4 : Une recherche infructueuse fournit une suggestion alternative et les coordonnées du support")
    void testSearchAlternativeWhenNotFound() {
        HelpService.SearchResult emptyResult = helpService.search("terme_inexistant_xyz_12345");
        assertFalse(emptyResult.hasResults, "Une recherche sans correspondance ne doit renvoyer aucun résultat");
        assertNotNull(emptyResult.alternativeSuggestion, "Une suggestion alternative doit être proposée");
        assertTrue(emptyResult.alternativeSuggestion.contains("support@ecolejenos.cd"),
                "La suggestion doit diriger vers le support technique officiel");
        assertTrue(emptyResult.alternativeSuggestion.contains("FAQ") || emptyResult.alternativeSuggestion.contains("Foire"),
                "La suggestion doit proposer de consulter la FAQ");
    }

    // =========================================================================
    //  Critère 5 : Foire Aux Questions (FAQ) hors ligne
    // =========================================================================

    @Test
    @DisplayName("Critère 5 : Chaque question de la FAQ contient le problème, la cause probable et la solution")
    void testFaqCompleteness() {
        List<HelpService.FaqItem> faq = helpService.getAllFaqItems();
        assertFalse(faq.isEmpty(), "La FAQ doit contenir des questions fréquentes");

        for (HelpService.FaqItem item : faq) {
            assertNotNull(item.getId(), "L'identifiant de la question FAQ ne doit pas être null");
            assertNotNull(item.getModule(), "Le module lié à la question ne doit pas être null");
            assertNotNull(item.getQuestion(), "La question ne doit pas être nulle");
            assertNotNull(item.getProbableCause(), "La cause probable doit être explicite");
            assertNotNull(item.getSolution(), "La solution recommandée doit être détaillée");
            assertTrue(item.getSolution().startsWith("Action"), "La solution doit commencer par une démarche concrète");
        }
    }

    // =========================================================================
    //  Critère 6 : Glossaire des termes comptables et techniques
    // =========================================================================

    @Test
    @DisplayName("Critère 6 : Le glossaire définit les concepts clés de gestion scolaire et d'architecture")
    void testGlossaryDefinitions() {
        List<HelpService.GlossaryTerm> terms = helpService.getAllGlossaryTerms();
        assertFalse(terms.isEmpty(), "Le glossaire ne doit pas être vide");

        boolean hasDevises = terms.stream().anyMatch(t -> t.getTerm().contains("Cloisonnement des Devises"));
        boolean hasImmutability = terms.stream().anyMatch(t -> t.getTerm().contains("Immutabilité"));
        boolean hasAdvanceCap = terms.stream().anyMatch(t -> t.getTerm().contains("Plafond d'Avance"));

        assertTrue(hasDevises, "Le glossaire doit expliquer le cloisonnement des devises (USD/CDF)");
        assertTrue(hasImmutability, "Le glossaire doit expliquer l'immutabilité historique");
        assertTrue(hasAdvanceCap, "Le glossaire doit expliquer le plafond d'avance de 50%");
    }

    // =========================================================================
    //  Critère 7 : Enrichissement des dialogues d'erreurs (AppErrorDTO)
    // =========================================================================

    @Test
    @DisplayName("Critère 7 : AppErrorDTO encapsule la cause probable, l'action corrective et le statut d'opération")
    void testAppErrorDTOEnrichment() {
        AppErrorDTO error = new AppErrorDTO(
                "ERR-PAY-001",
                "Le paiement ne peut pas être validé.",
                "Le montant saisi (100 USD) dépasse le solde restant dû (60 USD).",
                "Réduisez le montant versé à 60 USD ou moins, puis revalidez l'opération.",
                "ValidationException at PaymentService:214",
                true
        );

        assertEquals("ERR-PAY-001", error.getErrorCode());
        assertEquals("Le paiement ne peut pas être validé.", error.getUserMessage());
        assertEquals("Le montant saisi (100 USD) dépasse le solde restant dû (60 USD).", error.getProbableCause());
        assertEquals("Réduisez le montant versé à 60 USD ou moins, puis revalidez l'opération.", error.getSuggestedAction());
        assertTrue(error.isOperationFailed(), "Le statut de l'opération doit indiquer l'échec");

        // Vérification de la rétro-compatibilité
        AppErrorDTO legacyError = new AppErrorDTO("ERR-002", "Erreur générique", "Détail technique");
        assertEquals("ERR-002", legacyError.getErrorCode());
        assertEquals("Erreur générique", legacyError.getUserMessage());
        assertTrue(legacyError.isOperationFailed());
    }

    // =========================================================================
    //  Critère 8 : Coordonnées de support et version
    // =========================================================================

    @Test
    @DisplayName("Critère 8 : Les informations de support indiquent la version exacte et les canaux de contact")
    void testSupportInformation() {
        HelpService.SupportInfo support = helpService.getSupportInfo();
        assertNotNull(support, "Les informations de support doivent être disponibles");

        assertTrue(support.getAppVersion().contains("1.0.0 LTS"), "La version de l'application doit être 1.0.0 LTS");
        assertEquals("support@ecolejenos.cd", support.getTechnicalSupportEmail(), "L'adresse email doit être support@ecolejenos.cd");
        assertNotNull(support.getAssistancePhone(), "Le téléphone d'assistance doit être configuré");
        assertNotNull(support.getOperationalHours(), "Les plages horaires d'assistance doivent être mentionnées");
        assertNotNull(support.getDeveloperTeam(), "L'équipe de développement doit être mentionnée");
    }

    // =========================================================================
    //  Critère 9 : Résilience hors ligne & immutabilité
    // =========================================================================

    @Test
    @DisplayName("Critère 9 : HelpService est résilient face aux requêtes nulles et protège ses collections")
    void testOfflineResilienceAndImmutability() {
        // Test requêtes nulles ou vides
        assertDoesNotThrow(() -> {
            helpService.search(null);
            helpService.search("");
            helpService.search("   ");
            helpService.getTopicByModule(null);
            helpService.getTopicByModule("");
        }, "Le service d'aide ne doit jamais lever d'exception sur des arguments null ou vides");

        // Test immutabilité des listes retournées
        List<HelpService.HelpTopic> topics = helpService.getAllTopics();
        assertThrows(UnsupportedOperationException.class, () -> topics.add(null),
                "La liste des rubriques doit être immuable en lecture seule");

        List<HelpService.FaqItem> faq = helpService.getAllFaqItems();
        assertThrows(UnsupportedOperationException.class, () -> faq.add(null),
                "La liste de la FAQ doit être immuable en lecture seule");

        List<HelpService.GlossaryTerm> glossary = helpService.getAllGlossaryTerms();
        assertThrows(UnsupportedOperationException.class, () -> glossary.add(null),
                "Le glossaire doit être immuable en lecture seule");
    }
}

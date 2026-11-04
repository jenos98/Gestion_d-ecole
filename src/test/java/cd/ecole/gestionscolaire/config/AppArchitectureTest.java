package cd.ecole.gestionscolaire.config;

import cd.ecole.gestionscolaire.dto.CreatePaymentDTO;
import cd.ecole.gestionscolaire.dto.PageResult;
import cd.ecole.gestionscolaire.dto.PaymentReceiptDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AppArchitectureTest — Suite de tests automatisés JUnit 5 pour valider :
 * 1. La configuration de l'application (AppConfig, devises USD/CDF)
 * 2. La validation des paramètres PostgreSQL (DatabaseConfig)
 * 3. Le hachage et la vérification des mots de passe (jBCrypt)
 * 4. La pagination et l'intégrité des DTOs financiers
 */
public class AppArchitectureTest {

    @Test
    @DisplayName("Chargement de la configuration applicative (AppConfig)")
    void testAppConfigLoadsSuccessfully() {
        AppConfig config = AppConfig.getInstance();
        assertNotNull(config, "AppConfig ne doit pas être null");

        String schoolName = config.getSchoolName();
        assertNotNull(schoolName, "Le nom de l'école doit être défini");
        assertFalse(schoolName.isBlank(), "Le nom de l'école ne doit pas être vide");

        assertEquals("USD", config.getPrimaryCurrency(), "La devise principale doit être USD");
        assertEquals("CDF", config.getSecondaryCurrency(), "La devise secondaire doit être CDF");
    }

    @Test
    @DisplayName("Validation de la configuration PostgreSQL (DatabaseConfig)")
    void testDatabaseConfigValidation() {
        String url = DatabaseConfig.getJdbcUrl();
        assertNotNull(url, "L'URL JDBC doit être définie");
        assertTrue(url.startsWith("jdbc:postgresql://"), "L'URL doit être une URL PostgreSQL");

        String user = DatabaseConfig.getUsername();
        assertNotNull(user, "L'utilisateur DB doit être défini");
        assertFalse(user.isBlank(), "L'utilisateur DB ne doit pas être vide");

        assertDoesNotThrow(DatabaseConfig::validateConfiguration, "La configuration par défaut doit être valide");
    }

    @Test
    @DisplayName("Sécurité : Hachage et vérification des mots de passe avec jBCrypt")
    void testBCryptPasswordHashing() {
        String plainPassword = "SecretPassword1234!";
        String salt = BCrypt.gensalt(10);
        String hash = BCrypt.hashpw(plainPassword, salt);

        assertNotNull(hash, "Le hash généré ne doit pas être null");
        assertTrue(hash.startsWith("$2a$") || hash.startsWith("$2b$"), "Le hash doit respecter le format standard BCrypt");

        // Vérification avec mot de passe correct
        assertTrue(BCrypt.checkpw(plainPassword, hash), "La vérification du mot de passe doit réussir");

        // Vérification avec mot de passe erroné
        assertFalse(BCrypt.checkpw("MauvaisMotDePasse", hash), "La vérification avec mauvais mot de passe doit échouer");
    }

    @Test
    @DisplayName("DTO & Pagination : Calcul automatique des pages (PageResult)")
    void testPageResultPaginationCalculation() {
        List<String> items = List.of("Paiement 1", "Paiement 2", "Paiement 3");
        PageResult<String> pageResult = new PageResult<>(items, 50, 1, 10);

        assertEquals(1, pageResult.getPageNumber());
        assertEquals(10, pageResult.getPageSize());
        assertEquals(50, pageResult.getTotalCount());
        assertEquals(5, pageResult.getTotalPages());
        assertTrue(pageResult.hasNext(), "La page 1 doit avoir une page suivante (total 5)");
        assertFalse(pageResult.hasPrevious(), "La page 1 ne doit pas avoir de page précédente");

        // Dernière page
        PageResult<String> lastPage = new PageResult<>(items, 50, 5, 10);
        assertFalse(lastPage.hasNext(), "La dernière page ne doit pas avoir de page suivante");
        assertTrue(lastPage.hasPrevious(), "La dernière page doit avoir une page précédente");
    }

    @Test
    @DisplayName("DTOs Financiers : Séparation stricte USD / CDF et cohérence des montants")
    void testFinancialDTOIntegrity() {
        CreatePaymentDTO createDTO = new CreatePaymentDTO(
                101L,
                5L,
                150.0,
                "USD",
                "ESPECES",
                null,
                "caissiere01",
                "CS-01"
        );

        assertEquals("USD", createDTO.getCurrency());
        assertEquals(150.0, createDTO.getAmount());
        assertEquals("caissiere01", createDTO.getCashierUsername());

        PaymentReceiptDTO receipt = new PaymentReceiptDTO();
        receipt.setPaymentId(1L);
        receipt.setReceiptNumber("REC-2026-0001");
        receipt.setIssuedAt(LocalDateTime.now());
        receipt.setAmountPaid(150.0);
        receipt.setCurrency("USD");
        receipt.setTotalFeeDue(350.0);
        receipt.setBalanceRemaining(200.0);

        assertEquals("REC-2026-0001", receipt.getReceiptNumber());
        assertEquals(200.0, receipt.getBalanceRemaining());
        assertEquals("USD", receipt.getCurrency());
    }
}

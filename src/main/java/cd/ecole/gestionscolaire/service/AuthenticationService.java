package cd.ecole.gestionscolaire.service;

/**
 * AuthenticationService — Contrat de vérification des identifiants et de création de session.
 */
public interface AuthenticationService {

    /**
     * Authentifie un utilisateur par son identifiant et son mot de passe en clair.
     * @return true si l'authentification est réussie et le compte actif
     */
    boolean authenticate(String username, String plainPassword);

    /**
     * Change le mot de passe d'un utilisateur après vérification de l'ancien.
     */
    void changePassword(String username, String oldPassword, String newPassword);

    /**
     * Hache un mot de passe avec le sel standard (jBCrypt).
     */
    String hashPassword(String plainPassword);

    /**
     * Vérifie un mot de passe en clair contre un hash BCrypt.
     */
    boolean verifyPassword(String plainPassword, String hashedPassword);
}

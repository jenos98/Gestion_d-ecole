package cd.ecole.gestionscolaire.repository;

import java.util.Optional;

/**
 * UserRepository — Contrat d'accès aux données des utilisateurs (PostgreSQL).
 * RÈGLE ABSOLUE : Pas de suppression physique d'utilisateur.
 */
public interface UserRepository {

    Optional<UserRecord> findByUsername(String username);

    Optional<UserRecord> findById(long id);

    long save(UserRecord user);

    void updateStatus(long id, String newStatus);

    void updatePasswordHash(long id, String newHash);

    record UserRecord(
            long id,
            String username,
            String passwordHash,
            String firstName,
            String lastName,
            String postName,
            String email,
            String phone,
            String role,
            String cashRegisterCode,
            String status
    ) {}
}

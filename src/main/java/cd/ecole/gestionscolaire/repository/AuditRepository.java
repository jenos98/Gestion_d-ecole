package cd.ecole.gestionscolaire.repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AuditRepository — Contrat de journalisation immuable (PostgreSQL).
 * RÈGLE ABSOLUE : Aucune opération UPDATE ou DELETE sur la table audit_logs (append-only).
 */
public interface AuditRepository {

    void log(AuditRecord record);

    List<AuditRecord> findRecent(int limit);

    List<AuditRecord> findByUsername(String username, int limit);

    record AuditRecord(
            long id,
            LocalDateTime timestamp,
            String username,
            String action,
            String entityName,
            String oldValue,
            String newValue,
            String ipAddress
    ) {}
}

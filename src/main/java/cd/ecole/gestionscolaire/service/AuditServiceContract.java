package cd.ecole.gestionscolaire.service;

import java.util.List;

/**
 * AuditServiceContract — Contrat de journalisation immuable des événements sensibles.
 */
public interface AuditServiceContract {

    void logEvent(String username, String action, String entityName, String oldValue, String newValue);

    void logSecurityAlert(String username, String action, String details);
}

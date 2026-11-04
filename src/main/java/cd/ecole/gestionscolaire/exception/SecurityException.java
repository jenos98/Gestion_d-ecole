package cd.ecole.gestionscolaire.exception;

/**
 * SecurityException — Levée en cas d'accès non autorisé, compte bloqué ou tentative suspecte.
 */
public class SecurityException extends RuntimeException {
    public SecurityException(String message) {
        super(message);
    }
}

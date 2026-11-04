package cd.ecole.gestionscolaire.exception;

/**
 * BusinessException — Levée en cas de violation d'une règle métier (solde insuffisant, clôture bloquée, etc.).
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}

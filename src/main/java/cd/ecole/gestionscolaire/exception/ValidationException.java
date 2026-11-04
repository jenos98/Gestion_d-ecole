package cd.ecole.gestionscolaire.exception;

/**
 * ValidationException — Levée lorsqu'une entrée utilisateur ou une contrainte de format est invalide.
 */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}

package com.jenos.gestionecole.model;

import java.time.LocalDateTime;

/**
 * AppErrorDTO — Modèle enrichi de transport pour les erreurs système et métier.
 *
 * RÈGLES STRICTES DU BLOC 7 :
 * 1. Les erreurs ne doivent jamais se limiter à un message technique cryptique.
 * 2. Chaque message doit indiquer la cause probable et une action corrective claire.
 * 3. Indique clairement si l'opération a réussi ou échoué.
 * 4. La stack trace brute est réservée aux journaux de diagnostic techniques.
 */
public class AppErrorDTO {

    private final String code;
    private final String userMessage;
    private final String probableCause;
    private final String suggestedAction;
    private final String technicalMessage;
    private final LocalDateTime timestamp;
    private final boolean operationFailed;

    public AppErrorDTO(String code, String userMessage, String technicalMessage) {
        this(code, userMessage, null, null, technicalMessage, true);
    }

    public AppErrorDTO(String code, String userMessage, String probableCause,
                       String suggestedAction, String technicalMessage) {
        this(code, userMessage, probableCause, suggestedAction, technicalMessage, true);
    }

    public AppErrorDTO(String code, String userMessage, String probableCause,
                       String suggestedAction, String technicalMessage, boolean operationFailed) {
        this.code = code != null ? code : "ERR-SYS-001";
        this.userMessage = userMessage != null ? userMessage : "Une anomalie s'est produite lors de l'exécution.";
        this.probableCause = probableCause != null ? probableCause : "Paramètres incorrects ou contrainte métier non respectée.";
        this.suggestedAction = suggestedAction != null ? suggestedAction : "Vérifiez les données saisies et réessayez l'opération.";
        this.technicalMessage = technicalMessage != null ? technicalMessage : "Aucun détail technique supplémentaire.";
        this.operationFailed = operationFailed;
        this.timestamp = LocalDateTime.now();
    }

    public String getCode() { return code; }
    public String getErrorCode() { return code; }
    public String getUserMessage() { return userMessage; }
    public String getProbableCause() { return probableCause; }
    public String getSuggestedAction() { return suggestedAction; }
    public String getTechnicalMessage() { return technicalMessage; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public boolean isOperationFailed() { return operationFailed; }
}

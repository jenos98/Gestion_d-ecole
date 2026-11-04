package cd.ecole.gestionscolaire.service;

/**
 * PermissionService — Contrat de vérification des autorisations par rôle et action.
 */
public interface PermissionService {

    /**
     * Vérifie si l'utilisateur actuellement connecté peut exécuter l'action demandée.
     * @param permissionCode Code de permission granulaire (ex: "payment.create", "cash.close")
     */
    boolean can(String permissionCode);

    /**
     * Vérifie si l'utilisateur a le droit de visualiser les montants financiers réels.
     * RÈGLE ABSOLUE : false pour DIRECTEUR et ADMIN_TECHNIQUE.
     */
    boolean canViewFinancialData();
}
